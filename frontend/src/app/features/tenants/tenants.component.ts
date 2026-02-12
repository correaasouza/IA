import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { finalize } from 'rxjs/operators';

import { TenantService, LocatarioResponse } from './tenant.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    DateMaskDirective,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './tenants.component.html',
})
export class TenantsComponent implements OnInit {
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

  form = this.fb.group({
    nome: ['', Validators.required],
    dataLimiteAcesso: ['', Validators.required]
  });

  filters = this.fb.group({
    status: ['']
  });

  constructor(private fb: FormBuilder, private service: TenantService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    const status = this.filters.value.status || '';
    const bloqueado = status === 'bloqueado' ? 'true' : '';
    const ativo = status === 'ativo' ? 'true' : '';
    const nome = this.searchFields.includes('nome') ? this.searchTerm : '';

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
    this.applyFilters();
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  sortChange(sort: Sort) {
    if (sort.direction) {
      this.sort = `${sort.active},${sort.direction}`;
    } else {
      this.sort = 'nome,asc';
    }
    this.load();
  }

  create() {
    if (this.form.invalid) {
      return;
    }
    this.service.create({
      nome: this.form.value.nome!,
      dataLimiteAcesso: this.form.value.dataLimiteAcesso!,
      ativo: true
    }).subscribe({
      next: () => {
        this.form.reset();
        this.load();
      }
    });
  }

  renew(row: LocatarioResponse) {
    const date = new Date(row.dataLimiteAcesso);
    date.setDate(date.getDate() + 30);
    const iso = date.toISOString().slice(0, 10);
    this.service.updateAccessLimit(row.id, iso).subscribe({
      next: () => this.load()
    });
  }

  toggle(row: LocatarioResponse) {
    this.service.updateStatus(row.id, !row.ativo).subscribe({
      next: () => this.load()
    });
  }
}


