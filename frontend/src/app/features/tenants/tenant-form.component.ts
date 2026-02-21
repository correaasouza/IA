import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toDisplayDate } from '../../shared/date-utils';
import { TenantService, LocatarioResponse } from './tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { CompanyService, EmpresaResponse } from '../companies/company.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatSelectModule,
    DateMaskDirective,
    MatIconModule,
    MatDialogModule,
    InlineLoaderComponent,
    RouterLink,
    FieldSearchComponent
  ],
  templateUrl: './tenant-form.component.html',
  styleUrls: ['./tenant-form.component.css']
})
export class TenantFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  locatario: LocatarioResponse | null = null;
  title = 'Novo locatário';
  loading = false;
  saving = false;
  deleting = false;
  renewing = false;
  toggling = false;
  loadingFiliais = false;
  togglingEmpresaId: number | null = null;
  deletingEmpresaId: number | null = null;
  empresas: EmpresaResponse[] = [];
  filteredEmpresas: EmpresaResponse[] = [];
  matrizNomeById: Record<number, string> = {};
  tenantContextMatches = false;
  empresaSearchOptions: FieldSearchOption[] = [
    { key: 'razaoSocial', label: 'Razão social' },
    { key: 'nomeFantasia', label: 'Nome fantasia' },
    { key: 'cnpj', label: 'CNPJ' }
  ];
  empresaSearchTerm = '';
  empresaSearchFields = ['razaoSocial'];
  isMobile = false;
  mobileEmpresaFiltersOpen = false;
  empresaFilters = this.fb.group({
    tipo: [''],
    status: ['']
  });

  form = this.fb.group({
    nome: ['', Validators.required],
    dataLimiteAcesso: ['', Validators.required],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: TenantService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService,
    private companyService: CompanyService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (id) {
      this.mode = isEdit ? 'edit' : 'view';
      this.load(Number(id));
    } else {
      this.mode = 'new';
      this.updateTitle();
    }
    this.empresaFilters.valueChanges.subscribe(() => this.applyEmpresaFilters());
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  private updateTitle() {
    this.title = this.mode === 'new' ? 'Novo locatário' : this.mode === 'edit' ? 'Editar locatário' : 'Consultar locatário';
  }

  private load(id: number) {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.evaluateTenantContextAndLoadFiliais();
        this.form.patchValue({
          nome: data.nome,
          dataLimiteAcesso: toDisplayDate(data.dataLimiteAcesso),
          ativo: data.ativo
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
        }
        this.updateTitle();
      },
      error: () => this.notify.error('Não foi possível carregar o locatário.')
    });
  }

  private evaluateTenantContextAndLoadFiliais() {
    if (!this.locatario) {
      this.tenantContextMatches = false;
      this.empresas = [];
      this.filteredEmpresas = [];
      return;
    }
    const currentTenant = Number(localStorage.getItem('tenantId') || 0);
    this.tenantContextMatches = currentTenant > 0 && currentTenant === this.locatario.id;
    if (!this.tenantContextMatches) {
      this.empresas = [];
      this.filteredEmpresas = [];
      return;
    }
    this.loadFiliais();
  }

  private loadFiliais() {
    this.loadingFiliais = true;
    this.companyService.list({ page: 0, size: 200 }).pipe(finalize(() => this.loadingFiliais = false)).subscribe({
      next: data => {
        this.empresas = (data.content || []).sort((a: EmpresaResponse, b: EmpresaResponse) => {
          if (a.tipo === b.tipo) return a.razaoSocial.localeCompare(b.razaoSocial);
          return a.tipo === 'MATRIZ' ? -1 : 1;
        });
        this.loadMatrizesMap();
        this.applyEmpresaFilters();
      },
      error: () => {
        this.empresas = [];
        this.filteredEmpresas = [];
      }
    });
  }

  private loadMatrizesMap() {
    this.matrizNomeById = {};
    for (const m of this.empresas.filter(e => e.tipo === 'MATRIZ')) {
      this.matrizNomeById[m.id] = m.razaoSocial;
    }
  }

  matrizLabel(empresa: EmpresaResponse): string {
    if (empresa.tipo === 'MATRIZ') return 'Matriz';
    if (!empresa.matrizId) return '-';
    return this.matrizNomeById[empresa.matrizId] || `#${empresa.matrizId}`;
  }

  private applyEmpresaFilters() {
    const term = this.empresaSearchTerm.toLowerCase().trim();
    const fields = this.empresaSearchFields.length ? this.empresaSearchFields : this.empresaSearchOptions.map(o => o.key);
    const tipo = this.empresaFilters.value.tipo || '';
    const status = this.empresaFilters.value.status || '';
    this.filteredEmpresas = this.empresas.filter(e => {
      const hitTerm = !term || fields.some(field => {
        if (field === 'razaoSocial') return e.razaoSocial.toLowerCase().includes(term);
        if (field === 'nomeFantasia') return (e.nomeFantasia || '').toLowerCase().includes(term);
        if (field === 'cnpj') return e.cnpj.includes(term);
        return false;
      });
      const hitTipo = !tipo || e.tipo === tipo;
      const hitStatus = !status || (status === 'ativo' ? e.ativo : !e.ativo);
      return hitTerm && hitTipo && hitStatus;
    });
  }

  onEmpresaSearchChange(value: FieldSearchValue) {
    this.empresaSearchTerm = value.term;
    this.empresaSearchFields = value.fields.length ? value.fields : this.empresaSearchOptions.map(o => o.key);
    this.applyEmpresaFilters();
  }

  toggleMobileEmpresaFilters(): void {
    this.mobileEmpresaFiltersOpen = !this.mobileEmpresaFiltersOpen;
  }

  activeEmpresaFiltersCount(): number {
    let count = 0;
    if ((this.empresaSearchTerm || '').trim()) count++;
    if ((this.empresaFilters.value.tipo || '').trim()) count++;
    if ((this.empresaFilters.value.status || '').trim()) count++;
    return count;
  }

  toggleEmpresaStatus(empresa: EmpresaResponse) {
    const nextStatus = !empresa.ativo;
    this.togglingEmpresaId = empresa.id;
    this.companyService.updateStatus(empresa.id, nextStatus).pipe(finalize(() => this.togglingEmpresaId = null)).subscribe({
      next: () => {
        this.notify.success(nextStatus ? 'Empresa ativada.' : 'Empresa desativada.');
        this.loadFiliais();
      },
      error: () => this.notify.error('Não foi possível atualizar o status da empresa.')
    });
  }

  removeEmpresa(empresa: EmpresaResponse) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir empresa', message: `Deseja excluir "${empresa.razaoSocial}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deletingEmpresaId = empresa.id;
      this.companyService.delete(empresa.id).pipe(finalize(() => this.deletingEmpresaId = null)).subscribe({
        next: () => {
          this.notify.success('Empresa removida.');
          this.loadFiliais();
        },
        error: () => this.notify.error('Não foi possível excluir a empresa.')
      });
    });
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dataLimite = this.form.value.dataLimiteAcesso!;
    if (!isValidDateInput(dataLimite)) {
      this.form.get('dataLimiteAcesso')?.setErrors({ invalidDate: true });
      this.form.get('dataLimiteAcesso')?.markAsTouched();
      return;
    }
    const payload = {
      nome: this.form.value.nome!,
      dataLimiteAcesso: dataLimite,
      ativo: !!this.form.value.ativo
    };
    this.saving = true;
    if (this.mode === 'new') {
      this.service.create(payload).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.notify.success('Locatário criado.');
          this.router.navigateByUrl('/tenants');
        },
        error: () => this.notify.error('Não foi possível criar o locatário.')
      });
      return;
    }
    if (!this.locatario) {
      this.saving = false;
      return;
    }
    this.service.update(this.locatario.id, payload).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.notify.success('Locatário atualizado.');
        this.router.navigateByUrl('/tenants');
      },
      error: () => this.notify.error('Não foi possível atualizar o locatário.')
    });
  }

  renew() {
    if (!this.locatario) return;
    const date = new Date(this.locatario.dataLimiteAcesso);
    date.setDate(date.getDate() + 30);
    const iso = date.toISOString().slice(0, 10);
    this.renewing = true;
    this.service.updateAccessLimit(this.locatario.id, iso).pipe(finalize(() => this.renewing = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ dataLimiteAcesso: toDisplayDate(data.dataLimiteAcesso) });
        this.notify.success('Acesso renovado por 30 dias.');
      },
      error: () => this.notify.error('Não foi possível renovar o acesso.')
    });
  }

  toggleStatus() {
    if (!this.locatario) return;
    const ativo = !this.locatario.ativo;
    this.toggling = true;
    this.service.updateStatus(this.locatario.id, ativo).pipe(finalize(() => this.toggling = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ ativo: data.ativo });
        this.notify.success(ativo ? 'Locatário ativado.' : 'Locatário desativado.');
      },
      error: () => this.notify.error('Não foi possível atualizar o status.')
    });
  }

  remove() {
    if (!this.locatario) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir locatário', message: `Deseja excluir o locatário "${this.locatario.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deleting = true;
      this.service.delete(this.locatario!.id).pipe(finalize(() => this.deleting = false)).subscribe({
        next: () => {
          this.notify.success('Locatário removido.');
          this.router.navigateByUrl('/tenants');
        },
        error: () => this.notify.error('Não foi possível remover o locatário.')
      });
    });
  }

  toEdit() {
    if (!this.locatario) return;
    this.router.navigate(['/tenants', this.locatario.id, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/tenants');
  }

  statusLabel() {
    if (!this.locatario) return '';
    if (!this.locatario.ativo) return 'Inativo';
    if (this.locatario.bloqueado) return 'Bloqueado';
    return 'Ativo';
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileEmpresaFiltersOpen = false;
    }
  }
}



