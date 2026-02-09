import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { EntidadeService, EntidadeDefinicao, EntidadeRegistro } from './entidade.service';
import { ConfigService } from '../configs/config.service';
import { cpfCnpjValidator } from '../../shared/cpf-cnpj.validator';
import { CpfCnpjMaskDirective } from '../../shared/cpf-cnpj-mask.directive';
import { ContatosComponent } from './contatos.component';
import { ContatoTipoPorEntidadeService, ContatoTipoPorEntidade } from './contato-tipo-por-entidade.service';
import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { ContatoService, Contato } from './contato.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-entity-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    MatDialogModule,
    CpfCnpjMaskDirective,
    ContatosComponent,
    InlineLoaderComponent
  ],
  templateUrl: './entity-form.component.html',
  styleUrls: ['./entity-form.component.css']
})
export class EntityFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo registro';
  definicoes: EntidadeDefinicao[] = [];
  currentDef: EntidadeDefinicao | null = null;
  registroId: number | null = null;

  contatoTiposPorEntidade: ContatoTipoPorEntidade[] = [];
  contatoTipos: ContatoTipo[] = [];
  contatosDoRegistro: Contato[] = [];
  saving = false;
  deleting = false;

  visible = { nome: true, apelido: true, cpfCnpj: true };
  editable = { nome: true, apelido: true, cpfCnpj: true };
  labels = { nome: 'Nome', apelido: 'Apelido', cpfCnpj: 'CPF/CNPJ' };

  form = this.fb.group({
    entidadeDefinicaoId: [null as number | null, Validators.required],
    nome: ['', Validators.required],
    apelido: [''],
    cpfCnpj: ['', [Validators.required, cpfCnpjValidator]],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: EntidadeService,
    private config: ConfigService,
    private contatoTiposPorEntidadeService: ContatoTipoPorEntidadeService,
    private contatoTipoService: ContatoTipoService,
    private contatoService: ContatoService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (id) {
      this.mode = isEdit ? 'edit' : 'view';
      this.registroId = Number(id);
      this.loadRegistro(this.registroId);
    } else {
      this.mode = 'new';
      this.loadDefList(() => {
        const defId = Number(this.route.snapshot.queryParamMap.get('defId'));
        if (defId) {
          this.form.patchValue({ entidadeDefinicaoId: defId });
          this.currentDef = this.definicoes.find(d => d.id === defId) || null;
          this.loadConfig(defId);
          this.loadContatoTipos(defId);
        }
      });
      this.form.get('entidadeDefinicaoId')?.valueChanges.subscribe(value => {
        if (!value) return;
        this.currentDef = this.definicoes.find(d => d.id === value) || null;
        this.loadConfig(value);
        this.loadContatoTipos(value);
      });
      this.updateTitle();
    }
  }

  private updateTitle() {
    this.title = this.mode === 'new' ? 'Novo registro' : this.mode === 'edit' ? 'Editar registro' : 'Consultar registro';
  }

  private loadDefList(after?: () => void) {
    this.service.listDef(0, 50).subscribe({
      next: data => {
        this.definicoes = data.content || [];
        if (after) after();
        if (!this.form.value.entidadeDefinicaoId && this.definicoes.length > 0 && this.mode === 'new') {
          const first = this.definicoes[0];
          if (first) {
            this.form.patchValue({ entidadeDefinicaoId: first.id });
            this.currentDef = first;
            this.loadConfig(first.id);
            this.loadContatoTipos(first.id);
          }
        }
      }
    });
  }

  private loadRegistro(id: number) {
    this.loadDefList(() => {
      this.service.getReg(id).subscribe({
        next: data => {
          this.form.patchValue({
            entidadeDefinicaoId: data.entidadeDefinicaoId,
            nome: data.nome,
            apelido: data.apelido || '',
            cpfCnpj: data.cpfCnpj,
            ativo: data.ativo
          });
          this.currentDef = this.definicoes.find(d => d.id === data.entidadeDefinicaoId) || null;
          this.loadConfig(data.entidadeDefinicaoId);
          this.loadContatoTipos(data.entidadeDefinicaoId);
          this.loadContatosRegistro();
          if (this.mode === 'view') {
            this.form.disable();
          } else {
            this.form.enable();
            this.form.get('entidadeDefinicaoId')?.disable();
          }
          this.updateTitle();
        }
      });
    });
  }

  private loadConfig(defId: number) {
    const screenId = `entities-${defId}`;
    const rolesKeycloak = (JSON.parse(localStorage.getItem('roles') || '[]') as string[]);
    const rolesTenant = (JSON.parse(localStorage.getItem('tenantRoles') || '[]') as string[]);
    const roles = Array.from(new Set([...rolesKeycloak, ...rolesTenant])).join(',');
    const userId = localStorage.getItem('userId') || '';

    this.config.getForm(screenId, userId, roles).subscribe({
      next: cfg => this.applyConfig(cfg?.configJson || '{}')
    });
  }

  private applyConfig(configJson: string) {
    try {
      const cfg = JSON.parse(configJson);
      this.visible.nome = cfg?.fields?.nome?.visible ?? true;
      this.visible.apelido = cfg?.fields?.apelido?.visible ?? true;
      this.visible.cpfCnpj = cfg?.fields?.cpfCnpj?.visible ?? true;
      this.editable.nome = cfg?.fields?.nome?.editable ?? true;
      this.editable.apelido = cfg?.fields?.apelido?.editable ?? true;
      this.editable.cpfCnpj = cfg?.fields?.cpfCnpj?.editable ?? true;
      this.labels.nome = cfg?.fields?.nome?.label ?? 'Nome';
      this.labels.apelido = cfg?.fields?.apelido?.label ?? 'Apelido';
      this.labels.cpfCnpj = cfg?.fields?.cpfCnpj?.label ?? 'CPF/CNPJ';
    } catch {
    }
  }

  private loadContatoTipos(defId: number) {
    this.contatoTiposPorEntidadeService.list(defId).subscribe({
      next: data => this.contatoTiposPorEntidade = data,
      error: () => this.contatoTiposPorEntidade = []
    });
    this.contatoTipoService.list().subscribe({
      next: data => this.contatoTipos = data,
      error: () => this.contatoTipos = []
    });
  }

  private loadContatosRegistro() {
    if (!this.registroId) return;
    this.contatoService.list(this.registroId).subscribe({
      next: data => this.contatosDoRegistro = data,
      error: () => this.contatosDoRegistro = []
    });
  }

  private checkMissingObrigatorios(): string[] {
    const tiposObrigatorios = (this.contatoTiposPorEntidade || []).filter(t => t.obrigatorio);
    if (tiposObrigatorios.length === 0) return [];
    const existentes = (this.contatosDoRegistro || []).map(c => c.tipo);
    return tiposObrigatorios
      .filter(t => !existentes.includes(this.getTipoCodigoById(t.contatoTipoId)))
      .map(t => this.getTipoCodigoById(t.contatoTipoId))
      .filter(Boolean) as string[];
  }

  private getTipoCodigoById(id: number): string {
    const tipo = this.contatoTipos.find(t => t.id === id);
    return tipo ? tipo.codigo : '';
  }

  save() {
    if (this.form.invalid) return;
    if (this.form.value.ativo) {
      const missing = this.checkMissingObrigatorios();
      if (missing.length > 0) {
        alert(`Contatos obrigatórios faltando: ${missing.join(', ')}`);
        return;
      }
    }
    const payload = {
      entidadeDefinicaoId: this.form.value.entidadeDefinicaoId,
      nome: this.form.value.nome,
      apelido: this.form.value.apelido,
      cpfCnpj: this.form.value.cpfCnpj,
      ativo: this.form.value.ativo
    };
    if (this.mode === 'new') {
      this.saving = true;
      this.service.createReg(payload).pipe(finalize(() => this.saving = false)).subscribe({
        next: data => {
          this.notify.success('Registro criado com sucesso.');
          this.router.navigate(['/entities', data.id]);
        },
        error: () => this.notify.error('Não foi possível criar o registro.')
      });
      return;
    }
    if (!this.registroId) return;
    this.saving = true;
    this.service.updateReg(this.registroId, payload).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.notify.success('Registro atualizado com sucesso.');
        this.router.navigate(['/entities', this.registroId]);
      },
      error: () => this.notify.error('Não foi possível atualizar o registro.')
    });
  }

  remove() {
    if (!this.registroId) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir registro', message: 'Deseja excluir este registro?' }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deleting = true;
      this.service.deleteReg(this.registroId!).pipe(finalize(() => this.deleting = false)).subscribe({
        next: () => {
          this.notify.success('Registro removido.');
          this.router.navigateByUrl('/entities');
        },
        error: () => this.notify.error('Não foi possível remover o registro.')
      });
    });
  }

  toEdit() {
    if (!this.registroId) return;
    this.router.navigate(['/entities', this.registroId, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/entities');
  }
}
