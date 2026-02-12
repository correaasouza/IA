import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { EntidadeService, EntidadeDefinicao, EntidadeRegistro } from './entidade.service';
import { ConfigService } from '../configs/config.service';
import { cpfCnpjValidator } from '../../shared/cpf-cnpj.validator';
import { CpfCnpjMaskDirective } from '../../shared/cpf-cnpj-mask.directive';
import { SimpleMaskDirective } from '../../shared/simple-mask.directive';
import { ContatosComponent } from './contatos.component';
import { ContatoTiposComponent } from './contato-tipos.component';
import { ContatoTiposPorEntidadeComponent } from './contato-tipos-por-entidade.component';
import { ContatoTipoPorEntidadeService, ContatoTipoPorEntidade } from './contato-tipo-por-entidade.service';
import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { ContatoService, Contato } from './contato.service';

@Component({
  selector: 'app-entities',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatPaginatorModule,
    CpfCnpjMaskDirective,
    SimpleMaskDirective,
    ContatosComponent,
    ContatoTiposComponent,
    ContatoTiposPorEntidadeComponent
  ],
  templateUrl: './entities.component.html'
})
export class EntitiesComponent implements OnInit {
  definicoes: EntidadeDefinicao[] = [];
  registros: EntidadeRegistro[] = [];
  selectedDefId: number | null = null;
  selectedRegistroId: number | null = null;

  contatoTiposPorEntidade: ContatoTipoPorEntidade[] = [];
  contatoTipos: ContatoTipo[] = [];
  contatosDoRegistro: Contato[] = [];

  visible = { nome: true, apelido: true, cpfCnpj: true };
  editable = { nome: true, apelido: true, cpfCnpj: true };
  labels = { nome: 'Nome', apelido: 'Apelido', cpfCnpj: 'Documento' };

  columns: string[] = ['nome', 'apelido', 'cpfCnpj', 'ativo', 'acoes'];
  defColumns: string[] = ['nome', 'role', 'ativo', 'save'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;

  editingRowId: number | null = null;
  editingRow: any = {};

  form = this.fb.group({
    nome: ['', Validators.required],
    apelido: [''],
    tipoPessoa: ['FISICA'],
    cpfCnpj: [''],
    ativo: [true]
  });

  filters = this.fb.group({
    nome: [''],
    tipoPessoa: [''],
    cpfCnpj: [''],
    ativo: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: EntidadeService,
    private config: ConfigService,
    private contatoTiposPorEntidadeService: ContatoTipoPorEntidadeService,
    private contatoTipoService: ContatoTipoService,
    private contatoService: ContatoService
  ) {}

  ngOnInit(): void {
    this.loadDef();
    this.form.get('tipoPessoa')?.valueChanges.subscribe(() => this.updateDocumentoValidators());
  }

  loadDef() {
    this.service.listDef(0, 50).subscribe({
      next: data => {
        this.definicoes = data.content || [];
        if (this.definicoes.length > 0 && this.definicoes[0]) {
          this.selectedDefId = this.definicoes[0].id;
          this.changeDef();
        }
      }
    });
  }

  changeDef() {
    if (!this.selectedDefId) {
      return;
    }
    this.form.reset({ ativo: true, tipoPessoa: 'FISICA' });
    this.updateDocumentoValidators();
    this.editingRowId = null;
    this.selectedRegistroId = null;
    this.contatosDoRegistro = [];
    this.loadContatoTiposPorEntidade();
    this.loadContatoTipos();
    this.loadReg();
    this.loadConfig();
  }

  loadReg() {
    if (!this.selectedDefId) return;
    const ativo = this.filters.value.ativo || '';
    this.service.listReg(this.selectedDefId, this.pageIndex, this.pageSize, {
      nome: this.filters.value.nome || '',
      cpfCnpj: this.filters.value.cpfCnpj || '',
      ativo
    }).subscribe({
      next: data => {
        this.registros = data.content || [];
        this.totalElements = data.totalElements || 0;
      },
      error: () => {
        this.registros = [];
        this.totalElements = 0;
      }
    });
  }

  loadConfig() {
    const screenId = `entities-${this.selectedDefId}`;
    const rolesKeycloak = (JSON.parse(localStorage.getItem('roles') || '[]') as string[]);
    const rolesTenant = (JSON.parse(localStorage.getItem('tenantRoles') || '[]') as string[]);
    const roles = Array.from(new Set([...rolesKeycloak, ...rolesTenant])).join(',');
    const userId = localStorage.getItem('userId') || '';

    this.config.getForm(screenId, userId, roles).subscribe({
      next: cfg => this.applyConfig(cfg?.configJson || '{}', 'form')
    });

    this.config.getColunas(screenId, userId, roles).subscribe({
      next: cfg => this.applyConfig(cfg?.configJson || '{}', 'columns')
    });
  }

  applyConfig(configJson: string, mode: 'form' | 'columns') {
    try {
      const cfg = JSON.parse(configJson);
      if (mode === 'form') {
        this.visible.nome = cfg?.fields?.nome?.visible ?? true;
        this.visible.apelido = cfg?.fields?.apelido?.visible ?? true;
        this.visible.cpfCnpj = cfg?.fields?.cpfCnpj?.visible ?? true;
        this.editable.nome = cfg?.fields?.nome?.editable ?? true;
        this.editable.apelido = cfg?.fields?.apelido?.editable ?? true;
        this.editable.cpfCnpj = cfg?.fields?.cpfCnpj?.editable ?? true;
        this.labels.nome = cfg?.fields?.nome?.label ?? 'Nome';
        this.labels.apelido = cfg?.fields?.apelido?.label ?? 'Apelido';
        this.labels.cpfCnpj = cfg?.fields?.cpfCnpj?.label ?? 'Documento';
      } else {
        const cols = cfg?.columns || ['nome', 'apelido', 'cpfCnpj', 'ativo', 'acoes'];
        this.columns = cols.filter((c: string) => ['nome','apelido','cpfCnpj','ativo','acoes'].includes(c));
      }
    } catch {
    }
  }

  editRow(row: EntidadeRegistro) {
    this.editingRowId = row.id;
    this.editingRow = { ...row, tipoPessoa: row.tipoPessoa || this.resolveTipoPessoa(row.cpfCnpj) };
  }

  selectRegistro(row: EntidadeRegistro) {
    this.selectedRegistroId = row.id;
    this.loadContatosRegistro();
  }

  cancelEdit() {
    this.editingRowId = null;
    this.editingRow = {};
  }

  saveRow() {
    if (!this.editingRowId) return;
    if (!this.isDocumentoValido(this.editingRow.tipoPessoa || 'FISICA', this.editingRow.cpfCnpj)) {
      alert('Documento inválido.');
      return;
    }
    if (this.editingRow?.ativo) {
      const missing = this.checkMissingObrigatorios();
      if (missing.length > 0) {
        alert(`Contatos obrigatórios faltando: ${missing.join(', ')}`);
        return;
      }
    }
    const payload = {
      nome: this.editingRow.nome,
      apelido: this.editingRow.apelido,
      cpfCnpj: this.normalizeDocumento(this.editingRow.cpfCnpj, this.editingRow.tipoPessoa || 'FISICA'),
      tipoPessoa: this.editingRow.tipoPessoa || 'FISICA',
      ativo: this.editingRow.ativo
    };
    this.service.updateReg(this.editingRowId, payload).subscribe({
      next: () => {
        this.cancelEdit();
        this.loadReg();
      },
      error: err => {
        if (err?.error?.detail?.startsWith('contato_obrigatorio_')) {
          const tipo = err.error.detail.replace('contato_obrigatorio_', '');
          alert(`Contato obrigatório faltando: ${tipo}`);
        }
      }
    });
  }

  saveDef(row: EntidadeDefinicao) {
    const payload = {
      codigo: row.codigo,
      nome: row.nome,
      ativo: row.ativo,
      roleRequired: row.roleRequired || null
    };
    this.service.updateDef(row.id, payload).subscribe();
  }

  save() {
    if (this.form.invalid || !this.selectedDefId) {
      return;
    }
    if (!this.isDocumentoValido(this.form.value.tipoPessoa || 'FISICA', this.form.value.cpfCnpj || '')) {
      alert('Documento inválido.');
      return;
    }
    if (this.form.value.ativo) {
      const missing = this.checkMissingObrigatorios();
      if (missing.length > 0) {
        alert(`Contatos obrigatórios faltando: ${missing.join(', ')}`);
        return;
      }
    }
    const payload = {
      entidadeDefinicaoId: this.selectedDefId,
      nome: this.form.value.nome,
      apelido: this.form.value.apelido,
      cpfCnpj: this.normalizeDocumento(this.form.value.cpfCnpj || '', this.form.value.tipoPessoa || 'FISICA'),
      tipoPessoa: this.form.value.tipoPessoa || 'FISICA',
      ativo: this.form.value.ativo
    };
    this.service.createReg(payload).subscribe({
      next: () => {
        this.form.reset({ ativo: true });
        this.loadReg();
      },
      error: err => {
        if (err?.error?.detail?.startsWith('contato_obrigatorio_')) {
          const tipo = err.error.detail.replace('contato_obrigatorio_', '');
          alert(`Contato obrigatório faltando: ${tipo}`);
        }
      }
    });
  }

  applyFilters() {
    this.pageIndex = 0;
    this.loadReg();
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadReg();
  }

  checkMissingObrigatorios(): string[] {
    const tiposObrigatorios = (this.contatoTiposPorEntidade || []).filter(t => t.obrigatorio);
    if (tiposObrigatorios.length === 0) return [];
    const existentes = (this.contatosDoRegistro || []).map(c => c.tipo);
    return tiposObrigatorios
      .filter(t => !existentes.includes(this.getTipoCodigoById(t.contatoTipoId)))
      .map(t => this.getTipoCodigoById(t.contatoTipoId))
      .filter(Boolean) as string[];
  }

  loadContatoTiposPorEntidade() {
    if (!this.selectedDefId) return;
    this.contatoTiposPorEntidadeService.list(this.selectedDefId).subscribe({
      next: data => this.contatoTiposPorEntidade = data,
      error: () => this.contatoTiposPorEntidade = []
    });
  }

  loadContatoTipos() {
    this.contatoTipoService.list().subscribe({
      next: data => this.contatoTipos = data,
      error: () => this.contatoTipos = []
    });
  }

  loadContatosRegistro() {
    if (!this.selectedRegistroId) {
      this.contatosDoRegistro = [];
      return;
    }
    this.contatoService.list(this.selectedRegistroId).subscribe({
      next: data => this.contatosDoRegistro = data,
      error: () => this.contatosDoRegistro = []
    });
  }

  getTipoCodigoById(id: number): string {
    const tipo = this.contatoTipos.find(t => t.id === id);
    return tipo ? tipo.codigo : '';
  }

  get documentoLabel(): string {
    const tipo = this.form.value.tipoPessoa || 'FISICA';
    if (tipo === 'JURIDICA') return 'CNPJ';
    if (tipo === 'ESTRANGEIRA') return 'ID estrangeiro';
    return 'CPF';
  }

  get filterDocumentoLabel(): string {
    const tipo = this.filters.value.tipoPessoa || '';
    if (tipo === 'JURIDICA') return 'CNPJ';
    if (tipo === 'ESTRANGEIRA') return 'ID estrangeiro';
    if (tipo === 'FISICA') return 'CPF';
    return 'Documento';
  }

  updateDocumentoValidators() {
    const control = this.form.get('cpfCnpj');
    if (!control) return;
    const tipo = this.form.value.tipoPessoa || 'FISICA';
    const validators: ValidatorFn[] = [Validators.required];
    if (tipo !== 'ESTRANGEIRA') {
      validators.push(cpfCnpjValidator);
      validators.push(this.documentoTipoValidator(tipo));
    }
    control.setValidators(validators);
    control.updateValueAndValidity({ emitEvent: false });
  }

  private normalizeDocumento(value: string, tipo: string): string {
    if (tipo === 'ESTRANGEIRA') return (value || '').trim();
    return (value || '').replace(/\D/g, '');
  }

  resolveTipoPessoa(documento: string): string {
    const digits = (documento || '').replace(/\D/g, '');
    if (digits.length === 14) return 'JURIDICA';
    if (digits.length === 11) return 'FISICA';
    return 'ESTRANGEIRA';
  }

  private isDocumentoValido(tipo: string, documento: string): boolean {
    const value = (documento || '').trim();
    if (!value) return false;
    if (tipo === 'ESTRANGEIRA') return value.length >= 5;
    const digits = value.replace(/\D/g, '');
    if (tipo === 'FISICA' && digits.length !== 11) return false;
    if (tipo === 'JURIDICA' && digits.length !== 14) return false;
    return cpfCnpjValidator({ value } as any) === null;
  }

  private documentoTipoValidator(tipo: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = String(control.value || '').trim();
      if (!value) return null;
      const digits = value.replace(/\D/g, '');
      if (tipo === 'FISICA' && digits.length !== 11) return { cpf: true };
      if (tipo === 'JURIDICA' && digits.length !== 14) return { cnpj: true };
      return null;
    };
  }
}


