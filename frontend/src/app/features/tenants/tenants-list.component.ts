import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { TenantService, LocatarioResponse } from './tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-tenants-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatDialogModule,
    MatTooltipModule,
    MatMenuModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './tenants-list.component.html',
  styleUrls: ['./tenants-list.component.css']
})
export class TenantsListComponent implements OnInit {
  locatarios: LocatarioResponse[] = [];
  displayedColumns = ['nome', 'dataLimite', 'status', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  sort = 'nome,asc';
  loading = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['nome'];

  filters = this.fb.group({
    status: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: TenantService,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    const status = this.filters.value.status || '';
    const bloqueado = status === 'bloqueado' ? 'true' : '';
    const ativo = status === 'ativo' ? 'true' : '';
    const nome = this.searchFields.includes('nome') ? this.searchTerm : '';

    this.loading = true;
    this.service.list({
      page: this.pageIndex,
      size: this.pageSize,
      sort: this.sort,
      nome,
      bloqueado,
      ativo
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        let items = data.content || [];
        this.locatarios = items;
        this.totalElements = data.totalElements || 0;
      },
      error: () => {
        this.locatarios = [];
        this.totalElements = 0;
        this.notify.error('Não foi possível carregar os locatários.');
      }
    });
  }

  applyFilters() {
    this.pageIndex = 0;
    this.load();
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  sortChange(sort: Sort) {
    this.sort = sort.direction ? `${sort.active},${sort.direction}` : 'nome,asc';
    this.load();
  }

  statusLabel(row: LocatarioResponse) {
    if (!row.ativo) return 'Inativo';
    if (row.bloqueado) return 'Bloqueado';
    return 'Ativo';
  }

  statusClass(row: LocatarioResponse) {
    if (!row.ativo) return 'off';
    if (row.bloqueado) return 'warn';
    return '';
  }

  renew(row: LocatarioResponse) {
    const date = new Date(row.dataLimiteAcesso);
    date.setDate(date.getDate() + 30);
    const iso = date.toISOString().slice(0, 10);
    this.service.updateAccessLimit(row.id, iso).subscribe({
      next: () => {
        this.notify.success('Acesso renovado por mais 30 dias.');
        this.load();
      },
      error: () => this.notify.error('Não foi possível renovar o acesso.')
    });
  }

  view(row: LocatarioResponse) {
    this.router.navigate(['/tenants', row.id]);
  }

  edit(row: LocatarioResponse) {
    this.router.navigate(['/tenants', row.id, 'edit']);
  }

  remove(row: LocatarioResponse) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir locatário', message: `Deseja excluir o locatário "${row.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({
        next: () => {
          this.notify.success('Locatário removido.');
          this.load();
        },
        error: () => this.notify.error('Não foi possível remover o locatário.')
      });
    });
  }
}


