import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toIsoDate } from '../../shared/date-utils';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { MovementOperationService, MovimentoEstoqueResponse } from './movement-operation.service';

@Component({
  selector: 'app-movimento-estoque-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatTableModule,
    MatTooltipModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    FieldSearchComponent,
    DateMaskDirective,
    AccessControlDirective
  ],
  templateUrl: './movimento-estoque-list.component.html',
  styleUrls: ['./movimento-estoque-list.component.css']
})
export class MovimentoEstoqueListComponent implements OnInit {
  displayedColumns = ['nome', 'dataMovimento', 'movimentoConfig', 'tipoEntidadePadrao', 'acoes'];
  rows: MovimentoEstoqueResponse[] = [];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  loading = false;
  isMobile = false;
  mobileFiltersOpen = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['nome'];

  filters = this.fb.group({
    dataInicio: [''],
    dataFim: ['']
  });

  constructor(
    private fb: FormBuilder,
    private dialog: MatDialog,
    private router: Router,
    private notify: NotificationService,
    private service: MovementOperationService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  @HostListener('window:empresa-context-updated')
  onEmpresaContextUpdated(): void {
    this.pageIndex = 0;
    this.load();
  }

  load(): void {
    const dataInicioInput = (this.filters.value.dataInicio || '').trim();
    const dataFimInput = (this.filters.value.dataFim || '').trim();
    if (dataInicioInput && !isValidDateInput(dataInicioInput)) {
      this.notify.error('Data inicial invalida. Use DD/MM/AAAA.');
      return;
    }
    if (dataFimInput && !isValidDateInput(dataFimInput)) {
      this.notify.error('Data final invalida. Use DD/MM/AAAA.');
      return;
    }

    const nome = this.searchFields.includes('nome') ? this.searchTerm : '';
    this.loading = true;
    this.service.listEstoque({
      page: this.pageIndex,
      size: this.pageSize,
      nome,
      dataInicio: dataInicioInput ? toIsoDate(dataInicioInput) : null,
      dataFim: dataFimInput ? toIsoDate(dataFimInput) : null
    }).pipe(finalize(() => (this.loading = false))).subscribe({
      next: page => {
        this.rows = page.content || [];
        this.totalElements = page.totalElements || 0;
      },
      error: err => {
        this.rows = [];
        this.totalElements = 0;
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar movimentos de estoque.');
      }
    });
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applyFilters();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.load();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['nome'];
    this.filters.patchValue({
      dataInicio: '',
      dataFim: ''
    });
    this.applyFilters();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count++;
    if ((this.filters.value.dataInicio || '').trim()) count++;
    if ((this.filters.value.dataFim || '').trim()) count++;
    return count;
  }

  pageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  newMovimento(): void {
    this.router.navigate(['/movimentos/estoque/new'], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  view(row: MovimentoEstoqueResponse): void {
    this.router.navigate(['/movimentos/estoque', row.id], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  edit(row: MovimentoEstoqueResponse): void {
    this.router.navigate(['/movimentos/estoque', row.id, 'edit'], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  remove(row: MovimentoEstoqueResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir movimento',
        message: `Deseja excluir o movimento "${row.nome}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.service.deleteEstoque(row.id).subscribe({
        next: () => {
          this.notify.success('Movimento excluido.');
          this.load();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o movimento.')
      });
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }
}
