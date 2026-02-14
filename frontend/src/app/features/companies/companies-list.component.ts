import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { CompanyService, EmpresaResponse } from './company.service';

@Component({
  selector: 'app-companies-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './companies-list.component.html'
})
export class CompaniesListComponent implements OnInit {
  empresas: EmpresaResponse[] = [];
  displayedColumns = ['razaoSocial', 'cnpj', 'tipo', 'matriz', 'status', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  loading = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Razão social' },
    { key: 'cnpj', label: 'CNPJ' }
  ];
  searchTerm = '';
  searchFields = ['nome'];
  mobileFiltersOpen = false;
  isMobile = false;

  matrizNomeById: Record<number, string> = {};

  filters = this.fb.group({
    tipo: [''],
    status: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: CompanyService,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  private tenantId(): string {
    return (localStorage.getItem('tenantId') || '').trim();
  }

  private defaultKey(): string {
    return `empresaDefault:${this.tenantId()}`;
  }

  private getDefaultEmpresaId(): number {
    const raw = localStorage.getItem(this.defaultKey()) || '';
    return Number(raw || 0);
  }

  isDefaultEmpresa(row: EmpresaResponse): boolean {
    return row.id === this.getDefaultEmpresaId();
  }

  private notifyEmpresaContextUpdated(): void {
    if (typeof window === 'undefined') return;
    window.dispatchEvent(new CustomEvent('empresa-context-updated'));
  }

  toggleDefaultEmpresa(row: EmpresaResponse): void {
    const key = this.defaultKey();
    if (!this.tenantId()) {
      this.notify.error('Selecione um locatário para definir empresa padrão.');
      return;
    }
    if (this.isDefaultEmpresa(row)) {
      localStorage.removeItem(key);
      this.notify.success('Empresa padrão removida.');
      this.notifyEmpresaContextUpdated();
      return;
    }
    localStorage.setItem(key, String(row.id));
    this.notify.success(`Empresa padrão definida: ${row.razaoSocial}.`);
    // Atualiza também o contexto atual do header para refletir imediatamente após refresh.
    localStorage.setItem('empresaContextId', String(row.id));
    localStorage.setItem('empresaContextTipo', row.tipo || '');
    localStorage.setItem('empresaContextNome', row.razaoSocial || '');
    this.notifyEmpresaContextUpdated();
  }

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
  }

  load() {
    const params: Record<string, any> = {
      page: this.pageIndex,
      size: this.pageSize
    };
    if (this.searchFields.includes('nome')) params['nome'] = this.searchTerm;
    if (this.searchFields.includes('cnpj')) params['cnpj'] = this.searchTerm;
    const tipo = this.filters.value.tipo || '';
    if (tipo) params['tipo'] = tipo;
    const status = this.filters.value.status || '';
    if (status === 'ativo') params['ativo'] = true;
    if (status === 'inativo') params['ativo'] = false;

    this.loading = true;
    this.service.list(params).pipe(finalize(() => (this.loading = false))).subscribe({
      next: data => {
        this.empresas = data.content || [];
        this.totalElements = data.totalElements || 0;
        this.buildMatrizMap();
      },
      error: () => {
        this.empresas = [];
        this.totalElements = 0;
        this.notify.error('Não foi possível carregar as empresas.');
      }
    });
  }

  private buildMatrizMap() {
    for (const e of this.empresas) {
      if (e.tipo === 'MATRIZ') this.matrizNomeById[e.id] = e.razaoSocial;
    }
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.pageIndex = 0;
    this.load();
  }

  
  @HostListener('window:resize')
  onWindowResize() {
    this.updateViewportMode();
  }

  toggleMobileFilters() {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count++;
    if ((this.filters.value.tipo || '').trim()) count++;
    if ((this.filters.value.status || '').trim()) count++;
    return count;
  }

  private updateViewportMode() {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }

  applyFilters() {
    this.pageIndex = 0;
    this.load();
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  statusLabel(row: EmpresaResponse) {
    return row.ativo ? 'Ativo' : 'Inativo';
  }

  matrizLabel(row: EmpresaResponse) {
    if (!row.matrizId) return '-';
    return this.matrizNomeById[row.matrizId] || `#${row.matrizId}`;
  }

  toggleStatus(row: EmpresaResponse) {
    const nextStatus = !row.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${nextStatus ? 'Ativar' : 'Desativar'} empresa`,
        message: `Deseja ${nextStatus ? 'ativar' : 'desativar'} "${row.razaoSocial}"?`,
        confirmText: nextStatus ? 'Ativar' : 'Desativar',
        confirmColor: nextStatus ? 'primary' : 'warn'
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.updateStatus(row.id, nextStatus).subscribe({
        next: () => {
          this.notify.success(nextStatus ? 'Empresa ativada.' : 'Empresa desativada.');
          this.load();
        },
        error: () => this.notify.error('Não foi possível atualizar o status da empresa.')
      });
    });
  }
}




