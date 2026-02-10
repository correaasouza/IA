import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { ContatoTipoPorEntidadeService, ContatoTipoPorEntidade } from './contato-tipo-por-entidade.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-contato-tipos-por-entidade',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTableModule, MatSelectModule, MatSlideToggleModule, MatButtonModule, MatIconModule, InlineLoaderComponent, FieldSearchComponent],
  templateUrl: './contato-tipos-por-entidade.component.html',
  styleUrls: ['./contato-tipos-por-entidade.component.css']
})
export class ContatoTiposPorEntidadeComponent implements OnChanges {
  @Input() entidadeDefinicaoId: number | null = null;

  tipos: ContatoTipo[] = [];
  rows: ContatoTipoPorEntidade[] = [];
  filteredRows: ContatoTipoPorEntidade[] = [];
  columns = ['tipo', 'obrigatorio', 'principalUnico', 'acoes'];
  loading = false;
  savingIds = new Set<number>();
  savedIds = new Set<number>();
  errorIds = new Set<number>();

  searchOptions: FieldSearchOption[] = [
    { key: 'tipo', label: 'Tipo' },
    { key: 'obrigatorio', label: 'Obrigatório' },
    { key: 'principalUnico', label: 'Principal único' }
  ];
  searchTerm = '';
  searchFields = ['tipo'];

  constructor(
    private tiposService: ContatoTipoService,
    private service: ContatoTipoPorEntidadeService
  ) {}

  ngOnChanges(): void {
    if (this.entidadeDefinicaoId) {
      this.loadTipos();
      this.load();
    }
  }

  loadTipos() {
    this.tiposService.list().subscribe({
      next: data => {
        this.tipos = data;
        this.applySearch();
      },
      error: () => this.tipos = []
    });
  }

  load() {
    if (!this.entidadeDefinicaoId) return;
    this.loading = true;
    this.service.list(this.entidadeDefinicaoId).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.rows = data;
        this.applySearch();
      },
      error: () => {
        this.rows = [];
        this.filteredRows = [];
      }
    });
  }

  addRow() {
    if (!this.entidadeDefinicaoId) return;
    this.rows = this.rows.concat({
      id: 0,
      entidadeDefinicaoId: this.entidadeDefinicaoId,
      contatoTipoId: this.tipos[0]?.id ?? 0,
      obrigatorio: false,
      principalUnico: true
    });
  }

  save(row: ContatoTipoPorEntidade) {
    if (!this.entidadeDefinicaoId) return;
    this.savingIds.add(row.id || 0);
    this.savedIds.delete(row.id || 0);
    this.errorIds.delete(row.id || 0);
    const payload = {
      entidadeDefinicaoId: this.entidadeDefinicaoId,
      contatoTipoId: row.contatoTipoId,
      obrigatorio: row.obrigatorio,
      principalUnico: row.principalUnico
    };
    const done = () => this.savingIds.delete(row.id || 0);
    if (!row.id) {
      this.service.create(payload).pipe(finalize(done)).subscribe({
        next: () => { this.savedIds.add(row.id || 0); this.load(); },
        error: () => this.errorIds.add(row.id || 0)
      });
    } else {
      this.service.update(row.id, payload).pipe(finalize(done)).subscribe({
        next: () => { this.savedIds.add(row.id || 0); this.load(); },
        error: () => this.errorIds.add(row.id || 0)
      });
    }
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredRows = [...this.rows];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredRows = this.rows.filter(r => {
      const tipoNome = this.getTipoNome(r.contatoTipoId);
      const matchTipo = this.searchFields.includes('tipo') && match(tipoNome);
      const matchObrigatorio = this.searchFields.includes('obrigatorio') && match(r.obrigatorio ? 'sim' : 'nao');
      const matchPrincipal = this.searchFields.includes('principalUnico') && match(r.principalUnico ? 'sim' : 'nao');
      return matchTipo || matchObrigatorio || matchPrincipal;
    });
  }

  getTipoNome(id: number) {
    return this.tipos.find(t => t.id === id)?.nome || '';
  }
}

