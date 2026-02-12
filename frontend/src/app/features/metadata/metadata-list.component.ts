import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { MetadataService, TipoEntidade } from './metadata.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-metadata-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './metadata-list.component.html',
})
export class MetadataListComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  filtered: TipoEntidade[] = [];
  columns = ['codigo', 'nome', 'ativo', 'acoes'];
  loading = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'nome', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['codigo', 'nome'];

  constructor(
    private service: MetadataService,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.loading = true;
    this.service.listTipos(tenantId).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.tipos = data.content || [];
        this.applySearch();
      },
      error: () => {
        this.tipos = [];
        this.filtered = [];
        this.notify.error('Não foi possível carregar os tipos.');
      }
    });
  }

  newTipo() {
    this.router.navigate(['/metadata/new']);
  }

  editTipo(row: TipoEntidade) {
    this.router.navigate(['/metadata', row.id, 'edit'], { state: { tipo: row } });
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filtered = [...this.tipos];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filtered = this.tipos.filter(t => {
      const matchCodigo = this.searchFields.includes('codigo') && match(t.codigo);
      const matchNome = this.searchFields.includes('nome') && match(t.nome);
      return matchCodigo || matchNome;
    });
  }
}

