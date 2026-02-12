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
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';

import { EntidadePapelService } from './entidade-papel.service';
import { TipoEntidadeService, TipoEntidade } from './tipo-entidade.service';
import { PessoaService } from '../people/pessoa.service';
import { MetadataService, TipoEntidadeCampoRegra } from '../metadata/metadata.service';
import { PessoaFieldsComponent, PessoaFieldRule } from '../people/pessoa-fields.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfigService } from '../configs/config.service';

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
    PessoaFieldsComponent,
    InlineLoaderComponent
  ],
  templateUrl: './entity-form.component.html'
})
export class EntityFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo registro';
  tipos: TipoEntidade[] = [];
  registroId: number | null = null;
  loading = false;
  saving = false;
  deleting = false;

  regras: Record<string, PessoaFieldRule> = {};
  autoFilling = false;
  entityRules: { alerta?: PessoaFieldRule } = {};

  entityForm = this.fb.group({
    tipoEntidadeId: [null as number | null, Validators.required],
    pessoaId: [null as number | null],
    alerta: [''],
    ativo: [true]
  });

  pessoaForm = this.fb.group({
    nome: ['', Validators.required],
    apelido: [''],
    cpf: [''],
    cnpj: [''],
    idEstrangeiro: [''],
    tipoPessoa: ['FISICA'],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: EntidadePapelService,
    private tipoService: TipoEntidadeService,
    private pessoaService: PessoaService,
    private metadataService: MetadataService,
    private config: ConfigService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    this.loadTipos();

    this.entityForm.get('tipoEntidadeId')?.valueChanges.subscribe(value => {
      if (value) {
        this.loadRegras(value);
        this.loadEntityConfig(value);
      }
    });
    this.entityForm.get('pessoaId')?.valueChanges.subscribe(value => {
      if (value) {
        this.loadPessoa(value);
      } else if (this.mode !== 'view') {
        this.pessoaForm.reset({ ativo: true, tipoPessoa: 'FISICA' });
      }
    });

    this.pessoaForm.get('tipoPessoa')?.valueChanges.subscribe(tipo => {
      this.handleTipoPessoaChange(String(tipo || 'FISICA'));
    });

    this.watchDocumentoChanges();

    if (id) {
      this.mode = isEdit ? 'edit' : 'view';
      this.registroId = Number(id);
      this.loadRegistro(this.registroId);
    } else {
      this.mode = 'new';
      const tipoId = Number(this.route.snapshot.queryParamMap.get('tipoId')) || null;
      if (tipoId) {
        this.entityForm.patchValue({ tipoEntidadeId: tipoId });
        this.loadRegras(tipoId);
      }
      this.updateTitle();
    }
  }

  private updateTitle() {
    this.title = this.mode === 'new' ? 'Novo registro' : this.mode === 'edit' ? 'Editar registro' : 'Consultar registro';
  }

  private loadTipos() {
    this.tipoService.list(0, 100).subscribe({
      next: data => {
        this.tipos = data.content || [];
        if (!this.entityForm.value.tipoEntidadeId && this.tipos.length > 0 && this.mode === 'new') {
          const first = this.tipos[0];
          if (first) {
            this.entityForm.patchValue({ tipoEntidadeId: first.id });
            this.loadRegras(first.id);
            this.loadEntityConfig(first.id);
          }
        }
      }
    });
  }

  private loadRegistro(id: number) {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.entityForm.patchValue({
          tipoEntidadeId: data.tipoEntidadeId,
          pessoaId: data.pessoaId,
          alerta: data.alerta || '',
          ativo: data.ativo
        });
        this.loadRegras(data.tipoEntidadeId);
        this.loadEntityConfig(data.tipoEntidadeId);
        this.loadPessoa(data.pessoaId);
        if (this.mode === 'view') {
          this.entityForm.disable();
          this.pessoaForm.disable();
        }
        this.updateTitle();
      },
      error: () => this.notify.error('Não foi possível carregar o registro.')
    });
  }

  private loadPessoa(id: number) {
    this.autoFilling = true;
    this.pessoaService.get(id).subscribe({
      next: pessoa => {
        const tipoPessoa = pessoa.tipoPessoa || (pessoa.cnpj ? 'JURIDICA' : (pessoa.idEstrangeiro ? 'ESTRANGEIRA' : 'FISICA'));
        this.pessoaForm.patchValue({
          nome: pessoa.nome,
          apelido: pessoa.apelido || '',
          cpf: pessoa.cpf || '',
          cnpj: pessoa.cnpj || '',
          idEstrangeiro: pessoa.idEstrangeiro || '',
          tipoPessoa,
          ativo: pessoa.ativo
        });
        this.autoFilling = false;
      },
      error: () => {
        this.autoFilling = false;
        this.notify.error('Não foi possível carregar a pessoa.');
      }
    });
  }

  private watchDocumentoChanges() {
    const cpf = this.pessoaForm.get('cpf');
    const cnpj = this.pessoaForm.get('cnpj');
    const idEstrangeiro = this.pessoaForm.get('idEstrangeiro');
    cpf?.valueChanges.pipe(debounceTime(400), distinctUntilChanged()).subscribe(value => this.lookupByDocumento(value, 'cpf'));
    cnpj?.valueChanges.pipe(debounceTime(400), distinctUntilChanged()).subscribe(value => this.lookupByDocumento(value, 'cnpj'));
    idEstrangeiro?.valueChanges.pipe(debounceTime(400), distinctUntilChanged()).subscribe(value => this.lookupByDocumento(value, 'idEstrangeiro'));
  }

  private lookupByDocumento(value: string | null | undefined, tipo: 'cpf' | 'cnpj' | 'idEstrangeiro') {
    if (this.autoFilling || this.mode === 'view') return;
    if (!this.isDocTypeActive(tipo)) return;
    const documento = (value || '').trim();
    if (!documento) {
      this.entityForm.patchValue({ pessoaId: null }, { emitEvent: false });
      return;
    }
    const normalized = this.normalizeDocumento(documento, tipo);
    if (tipo === 'cpf' && normalized.length !== 11) return;
    if (tipo === 'cnpj' && normalized.length !== 14) return;
    if (tipo === 'idEstrangeiro' && normalized.length < 5) return;
    this.pessoaService.findByDocumento(documento).subscribe({
      next: pessoa => {
        this.autoFilling = true;
        this.entityForm.patchValue({ pessoaId: pessoa.id }, { emitEvent: false });
        const tipoPessoa = pessoa.tipoPessoa || (pessoa.cnpj ? 'JURIDICA' : (pessoa.idEstrangeiro ? 'ESTRANGEIRA' : 'FISICA'));
        this.pessoaForm.patchValue({
          nome: pessoa.nome,
          apelido: pessoa.apelido || '',
          cpf: pessoa.cpf || '',
          cnpj: pessoa.cnpj || '',
          idEstrangeiro: pessoa.idEstrangeiro || '',
          tipoPessoa,
          ativo: pessoa.ativo
        }, { emitEvent: false });
        this.autoFilling = false;
      },
      error: () => {
        this.entityForm.patchValue({ pessoaId: null }, { emitEvent: false });
      }
    });
  }

  private normalizeDocumento(value: string, tipo: 'cpf' | 'cnpj' | 'idEstrangeiro') {
    if (tipo === 'idEstrangeiro') return value.trim();
    return value.replace(/\D/g, '');
  }

  private loadRegras(tipoId: number) {
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.metadataService.listCampos(tenantId, tipoId).subscribe({
      next: regras => this.applyRegras(regras),
      error: () => this.applyRegras([])
    });
  }

  private loadEntityConfig(tipoId: number) {
    const screenId = `entities-${tipoId}`;
    const rolesKeycloak = (JSON.parse(localStorage.getItem('roles') || '[]') as string[]);
    const rolesTenant = (JSON.parse(localStorage.getItem('tenantRoles') || '[]') as string[]);
    const roles = Array.from(new Set([...rolesKeycloak, ...rolesTenant])).join(',');
    const userId = localStorage.getItem('userId') || '';
    this.config.getForm(screenId, userId, roles).subscribe({
      next: cfg => this.applyEntityConfig(cfg?.configJson || '{}'),
      error: () => this.applyEntityConfig('{}')
    });
  }

  private applyEntityConfig(configJson: string) {
    try {
      const cfg = JSON.parse(configJson);
      const fieldCfg = cfg?.fields || {};
      this.entityRules = {
        alerta: {
          visible: fieldCfg?.alerta?.visible ?? true,
          required: fieldCfg?.alerta?.required ?? false,
          editable: fieldCfg?.alerta?.editable ?? true,
          label: fieldCfg?.alerta?.label ?? 'Alerta'
        }
      };
    } catch {
      this.entityRules = { alerta: { visible: true, required: false, editable: true, label: 'Alerta' } };
    }
    this.applyEntityValidators();
  }

  private applyEntityValidators() {
    const alerta = this.entityForm.get('alerta');
    if (!alerta) return;
    alerta.clearValidators();
    if (this.entityRules.alerta?.required) {
      alerta.addValidators(Validators.required);
    }
    alerta.updateValueAndValidity({ emitEvent: false });
    if (this.mode !== 'view') {
      if (this.entityRules.alerta?.editable === false) {
        alerta.disable({ emitEvent: false });
      } else {
        alerta.enable({ emitEvent: false });
      }
    }
  }

  private applyRegras(regras: TipoEntidadeCampoRegra[]) {
    const map: Record<string, PessoaFieldRule> = {};
    const toKey = (campo: string) => campo === 'id_estrangeiro' ? 'idEstrangeiro' : campo;
    regras.forEach(r => {
      const key = toKey(r.campo);
      const visible = r.habilitado && r.visivel;
      map[key] = {
        visible,
        required: visible ? r.requerido : false,
        editable: r.editavel,
        label: r.label
      };
    });
    this.regras = map;
    this.applyValidators();
  }

  private applyValidators() {
    const fields = ['nome', 'apelido', 'cpf', 'cnpj', 'idEstrangeiro'];
    fields.forEach(field => {
      const control = this.pessoaForm.get(field);
      if (!control) return;
      const isDocField = field === 'cpf' || field === 'cnpj' || field === 'idEstrangeiro';
      const isVisibleDoc = isDocField ? this.isDocTypeActive(field as any) : true;
      const required = isDocField ? isVisibleDoc : (this.regras[field]?.required ?? (field === 'nome'));
      control.clearValidators();
      if (required) {
        control.addValidators(Validators.required);
      }
      control.updateValueAndValidity({ emitEvent: false });
      if (this.mode !== 'view') {
        const editable = this.regras[field]?.editable ?? true;
        if (!editable || (isDocField && !isVisibleDoc)) {
          control.disable({ emitEvent: false });
        } else {
          control.enable({ emitEvent: false });
        }
      }
    });
  }

  save() {
    if (this.entityForm.invalid || this.pessoaForm.invalid) return;
    this.saving = true;
    const pessoaPayload = this.normalizePessoaPayload(this.pessoaForm.value);
    const existingPessoaId = this.entityForm.value.pessoaId;

    const upsertPessoa = existingPessoaId
      ? this.pessoaService.update(existingPessoaId, pessoaPayload)
      : this.pessoaService.create(pessoaPayload);

    upsertPessoa.pipe(finalize(() => this.saving = false)).subscribe({
      next: pessoa => {
        const entityPayload = {
          tipoEntidadeId: this.entityForm.value.tipoEntidadeId,
          pessoaId: pessoa.id,
          alerta: this.entityForm.value.alerta,
          ativo: this.entityForm.value.ativo
        };
        if (this.mode === 'new') {
          this.service.create(entityPayload).subscribe({
            next: data => {
              this.notify.success('Registro criado com sucesso.');
              this.router.navigate(['/entities', data.id]);
            },
            error: () => this.notify.error('Não foi possível criar o registro.')
          });
          return;
        }
        if (!this.registroId) return;
        this.service.update(this.registroId, entityPayload).subscribe({
          next: () => {
            this.notify.success('Registro atualizado com sucesso.');
            this.router.navigate(['/entities', this.registroId]);
          },
          error: () => this.notify.error('Não foi possível atualizar o registro.')
        });
      },
      error: () => this.notify.error('Não foi possível salvar os dados da pessoa.')
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
      this.service.delete(this.registroId!).pipe(finalize(() => this.deleting = false)).subscribe({
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

  private normalizePessoaPayload(payload: any) {
    const cleaned = { ...payload };
    const tipoPessoa = cleaned.tipoPessoa as string | undefined;
    if (tipoPessoa === 'FISICA') {
      cleaned.cnpj = null;
      cleaned.idEstrangeiro = null;
    } else if (tipoPessoa === 'JURIDICA') {
      cleaned.cpf = null;
      cleaned.idEstrangeiro = null;
    } else if (tipoPessoa === 'ESTRANGEIRA') {
      cleaned.cpf = null;
      cleaned.cnpj = null;
    }
    ['apelido', 'cpf', 'cnpj', 'idEstrangeiro'].forEach(key => {
      if (cleaned[key] === '') {
        cleaned[key] = null;
      }
    });
    return cleaned;
  }

  private getTipoPessoa(): 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA' {
    const value = (this.pessoaForm.get('tipoPessoa')?.value as string) || 'FISICA';
    if (value === 'JURIDICA' || value === 'ESTRANGEIRA') return value;
    return 'FISICA';
  }

  private isDocTypeActive(tipo: 'cpf' | 'cnpj' | 'idEstrangeiro') {
    const current = this.getTipoPessoa();
    if (tipo === 'cpf') return current === 'FISICA';
    if (tipo === 'cnpj') return current === 'JURIDICA';
    return current === 'ESTRANGEIRA';
  }

  private handleTipoPessoaChange(tipo: string) {
    if (this.autoFilling) return;
    const cpf = this.pessoaForm.get('cpf');
    const cnpj = this.pessoaForm.get('cnpj');
    const idEstrangeiro = this.pessoaForm.get('idEstrangeiro');
    if (tipo === 'FISICA') {
      cnpj?.reset('', { emitEvent: false });
      idEstrangeiro?.reset('', { emitEvent: false });
    } else if (tipo === 'JURIDICA') {
      cpf?.reset('', { emitEvent: false });
      idEstrangeiro?.reset('', { emitEvent: false });
    } else if (tipo === 'ESTRANGEIRA') {
      cpf?.reset('', { emitEvent: false });
      cnpj?.reset('', { emitEvent: false });
    }
    this.applyValidators();
  }
}

