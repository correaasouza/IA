import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';

import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-contato-tipos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatTableModule, MatSlideToggleModule, MatIconModule, MatTooltipModule, InlineLoaderComponent, FieldSearchComponent],
  templateUrl: './contato-tipos.component.html',
  styleUrls: ['./contato-tipos.component.css']
})
export class ContatoTiposComponent implements OnInit {
  tipos: ContatoTipo[] = [];
  filteredTipos: ContatoTipo[] = [];
  columns = ['codigo', 'nome', 'mascara', 'regex', 'ativo', 'obrigatorio', 'principalUnico', 'acoes'];
  loading = false;
  savingIds = new Set<number>();
  savedIds = new Set<number>();
  errorIds = new Set<number>();

  searchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'nome', label: 'Nome' },
    { key: 'mascara', label: 'Máscara' },
    { key: 'regex', label: 'Regex' }
  ];
  searchTerm = '';
  searchFields = ['codigo', 'nome'];

  form = this.fb.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    mascara: [''],
    regexValidacao: [''],
    ativo: [true],
    obrigatorio: [false],
    principalUnico: [true]
  });

  constructor(private fb: FormBuilder, private service: ContatoTipoService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.service.list().pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.tipos = data;
        this.applySearch();
      },
      error: () => {
        this.tipos = [];
        this.filteredTipos = [];
      }
    });
  }

  create() {
    if (this.form.invalid) return;
    this.service.create(this.form.value).subscribe({
      next: () => {
        this.form.reset({ ativo: true, obrigatorio: false, principalUnico: true, mascara: '', regexValidacao: '' });
        this.load();
      }
    });
  }

  save(row: ContatoTipo) {
    this.savingIds.add(row.id);
    this.savedIds.delete(row.id);
    this.errorIds.delete(row.id);
    const payload = {
      codigo: row.codigo,
      nome: row.nome,
      ativo: row.ativo,
      obrigatorio: row.obrigatorio,
      principalUnico: row.principalUnico,
      mascara: row.mascara || null,
      regexValidacao: row.regexValidacao || null
    };
    this.service.update(row.id, payload).pipe(finalize(() => this.savingIds.delete(row.id))).subscribe({
      next: () => this.savedIds.add(row.id),
      error: () => this.errorIds.add(row.id)
    });
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredTipos = [...this.tipos];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredTipos = this.tipos.filter(t => {
      const matchCodigo = this.searchFields.includes('codigo') && match(t.codigo);
      const matchNome = this.searchFields.includes('nome') && match(t.nome);
      const matchMascara = this.searchFields.includes('mascara') && match(t.mascara || '');
      const matchRegex = this.searchFields.includes('regex') && match(t.regexValidacao || '');
      return matchCodigo || matchNome || matchMascara || matchRegex;
    });
  }
}

