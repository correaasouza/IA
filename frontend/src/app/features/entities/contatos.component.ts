import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';

import { ContatoService, Contato } from './contato.service';
import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { TelefoneMaskDirective } from '../../shared/telefone-mask.directive';
import { CustomMaskDirective } from '../../shared/custom-mask.directive';
import { contatoValorValidator } from '../../shared/contato.validator';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-contatos',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    MatTooltipModule,
    TelefoneMaskDirective,
    CustomMaskDirective,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './contatos.component.html',
  styleUrls: ['./contatos.component.css']
})
export class ContatosComponent implements OnChanges {
  @Input() entidadeRegistroId: number | null = null;

  contatos: Contato[] = [];
  filteredContatos: Contato[] = [];
  tipos: ContatoTipo[] = [];
  columns: string[] = ['tipo', 'valor', 'principal', 'acoes'];
  editingId: number | null = null;
  loading = false;
  saving = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'tipo', label: 'Tipo' },
    { key: 'valor', label: 'Valor' },
    { key: 'principal', label: 'Principal' }
  ];
  searchTerm = '';
  searchFields = ['tipo', 'valor'];

  form: FormGroup;

  constructor(private fb: FormBuilder, private service: ContatoService, private tipoService: ContatoTipoService) {
    this.form = this.fb.group({
      tipo: ['TELEFONE', Validators.required],
      valor: ['', [Validators.required, contatoValorValidator(
        () => this.form?.value?.tipo || '',
        () => this.currentRegex()
      )]],
      principal: [false]
    });
    this.form.get('tipo')?.valueChanges.subscribe(() => {
      this.form.get('valor')?.updateValueAndValidity();
    });
  }

  ngOnChanges(): void {
    if (this.entidadeRegistroId) {
      this.load();
      this.loadTipos();
    }
  }

  load() {
    if (!this.entidadeRegistroId) return;
    this.loading = true;
    this.service.list(this.entidadeRegistroId).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.contatos = data;
        this.applySearch();
      },
      error: () => {
        this.contatos = [];
        this.filteredContatos = [];
      }
    });
  }

  loadTipos() {
    this.tipoService.list().subscribe({
      next: data => this.tipos = data.filter(t => t.ativo),
      error: () => this.tipos = []
    });
  }

  edit(row: Contato) {
    this.editingId = row.id;
    this.form.patchValue({
      tipo: row.tipo,
      valor: row.valor,
      principal: row.principal
    });
  }

  cancelEdit() {
    this.editingId = null;
    this.form.reset({ tipo: 'TELEFONE', principal: false });
  }

  save() {
    if (this.form.invalid || !this.entidadeRegistroId) return;
    this.saving = true;
    const payload = {
      entidadeRegistroId: this.entidadeRegistroId,
      tipo: this.form.value.tipo,
      valor: this.form.value.valor,
      principal: this.form.value.principal
    };
    if (this.editingId) {
      this.service.update(this.editingId, payload).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.editingId = null;
          this.form.reset({ tipo: 'TELEFONE', principal: false });
          this.load();
        }
      });
      return;
    }
    this.service.create(payload).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.form.reset({ tipo: 'TELEFONE', principal: false });
        this.load();
      }
    });
  }

  remove(row: Contato) {
    this.service.delete(row.id).subscribe({
      next: () => this.load()
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
      this.filteredContatos = [...this.contatos];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredContatos = this.contatos.filter(c => {
      const matchTipo = this.searchFields.includes('tipo') && match(c.tipo);
      const matchValor = this.searchFields.includes('valor') && match(c.valor);
      const matchPrincipal = this.searchFields.includes('principal') && match(c.principal ? 'sim' : 'nao');
      return matchTipo || matchValor || matchPrincipal;
    });
  }

  currentRegex(): string {
    const tipo = this.form.value.tipo || '';
    const cfg = this.tipos.find(t => t.codigo === tipo);
    return cfg?.regexValidacao || '';
  }

  currentMask(): string {
    const tipo = this.form.value.tipo || '';
    const cfg = this.tipos.find(t => t.codigo === tipo);
    return cfg?.mascara || '';
  }

  useCustomMask(): boolean {
    return !!this.currentMask();
  }

  usePhoneMask(): boolean {
    const tipo = this.form.value.tipo || '';
    return ['TELEFONE', 'WHATSAPP'].includes(tipo) && !this.currentMask();
  }
}

