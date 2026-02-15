import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { EntityRecordService, RegistroEntidade, RegistroEntidadeContexto, RegistroEntidadePayload } from './entity-record.service';
import { EntityTypeService } from '../entity-types/entity-type.service';
import { EntityTypeAccessService } from './entity-type-access.service';

@Component({
  selector: 'app-entity-record-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    InlineLoaderComponent
  ],
  templateUrl: './entity-record-form.component.html',
  styleUrls: ['./entity-record-form.component.css']
})
export class EntityRecordFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Nova entidade';
  tipoNome = '';
  tipoEntidadeId = 0;
  entidadeId: number | null = null;
  contexto: RegistroEntidadeContexto | null = null;
  contextoWarning = '';
  loading = false;
  saving = false;
  deleting = false;
  selectedGrupoIdFromRoute: number | null = null;
  currentGrupoEntidadeId: number | null = null;

  form = this.fb.group({
    codigo: [{ value: '', disabled: true }],
    ativo: [true, Validators.required],
    pessoaNome: ['', [Validators.required, Validators.maxLength(200)]],
    pessoaApelido: ['', [Validators.maxLength(200)]],
    tipoRegistro: ['CPF', Validators.required],
    registroFederal: ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private service: EntityRecordService,
    private notify: NotificationService,
    private typeService: EntityTypeService,
    private typeAccess: EntityTypeAccessService
  ) {}

  ngOnInit(): void {
    this.tipoEntidadeId = Number(this.route.snapshot.queryParamMap.get('tipoEntidadeId') || 0);
    if (!this.tipoEntidadeId) {
      this.notify.error('Tipo de entidade nao informado.');
      this.back();
      return;
    }
    const idParam = this.route.snapshot.paramMap.get('id');
    this.selectedGrupoIdFromRoute = this.toNumber(this.route.snapshot.queryParamMap.get('grupoId'));
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (idParam) {
      this.entidadeId = Number(idParam);
      this.mode = isEdit ? 'edit' : 'view';
      this.title = this.mode === 'edit' ? 'Editar entidade' : 'Consultar entidade';
    } else {
      this.mode = 'new';
      this.title = 'Nova entidade';
    }

    this.loadTipoAndContinue();
  }

  toEdit(): void {
    if (!this.entidadeId) return;
    this.router.navigate(['/entities', this.entidadeId, 'edit'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
  }

  goGroups(): void {
    if (!this.tipoEntidadeId) return;
    this.router.navigate(['/entities/groups'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
  }

  save(): void {
    if (this.mode === 'view' || !this.contexto?.vinculado) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload: RegistroEntidadePayload = {
      grupoEntidadeId: this.resolveGrupoEntidadeIdForSave(),
      ativo: !!this.form.value.ativo,
      pessoa: {
        nome: (this.form.value.pessoaNome || '').trim(),
        apelido: (this.form.value.pessoaApelido || '').trim() || undefined,
        tipoRegistro: (this.form.value.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO',
        registroFederal: (this.form.value.registroFederal || '').trim()
      }
    };
    this.saving = true;
    if (this.mode === 'new') {
      this.service.create(this.tipoEntidadeId, payload).pipe(finalize(() => (this.saving = false))).subscribe({
        next: created => {
          this.notify.success('Entidade criada.');
          this.router.navigate(['/entities', created.id], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar entidade.')
      });
      return;
    }
    if (!this.entidadeId) {
      this.saving = false;
      return;
    }
    this.service.update(this.tipoEntidadeId, this.entidadeId, payload).pipe(finalize(() => (this.saving = false))).subscribe({
      next: updated => {
        this.notify.success('Entidade atualizada.');
        this.router.navigate(['/entities', updated.id], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar entidade.')
    });
  }

  remove(): void {
    if (!this.entidadeId || this.mode === 'new') return;
    if (!confirm(`Excluir entidade codigo ${this.form.getRawValue().codigo || ''}?`)) return;
    this.deleting = true;
    this.service.delete(this.tipoEntidadeId, this.entidadeId).pipe(finalize(() => (this.deleting = false))).subscribe({
      next: () => {
        this.notify.success('Entidade excluida.');
        this.back();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir entidade.')
    });
  }

  back(): void {
    this.router.navigate(['/entities'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId || null } });
  }

  cadastroTitle(): string {
    const nome = this.formatTipoNome(this.tipoNome);
    return nome ? `Cadastro ${nome}` : 'Cadastro de Entidades';
  }

  private resolveContextAndLoad(): void {
    this.loading = true;
    this.service.contextoEmpresa(this.tipoEntidadeId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ctx => {
          this.contexto = ctx;
          if (!ctx.vinculado) {
          this.contextoWarning = ctx.mensagem || 'Empresa sem grupo para o tipo selecionado.';
          this.form.disable();
          return;
        }
        this.contextoWarning = '';
        if (this.entidadeId) {
          this.loadEntidade(this.entidadeId);
        } else {
            this.applyModeState();
          }
        },
        error: err => {
          this.contexto = null;
          this.contextoWarning = err?.error?.detail || 'Nao foi possivel resolver contexto da empresa.';
          this.form.disable();
        }
      });
  }

  private loadEntidade(id: number): void {
    this.loading = true;
    this.service.get(this.tipoEntidadeId, id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: entity => {
          this.patchForm(entity);
          this.applyModeState();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar entidade.');
          this.back();
        }
      });
  }

  private patchForm(entity: RegistroEntidade): void {
    this.currentGrupoEntidadeId = entity.grupoEntidadeId || null;
    this.form.patchValue({
      codigo: entity.codigo.toString(),
      ativo: entity.ativo,
      pessoaNome: entity.pessoa.nome,
      pessoaApelido: entity.pessoa.apelido || '',
      tipoRegistro: entity.pessoa.tipoRegistro,
      registroFederal: entity.pessoa.registroFederal
    });
  }

  private applyModeState(): void {
    if (!this.contexto?.vinculado) {
      this.form.disable();
      return;
    }
    if (this.mode === 'view') {
      this.form.disable();
      return;
    }
    this.form.enable();
    this.form.controls.codigo.disable();
    if (this.mode === 'new') {
      this.form.controls.codigo.setValue('Gerado ao salvar');
    }
  }

  private toNumber(value: unknown): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private resolveGrupoEntidadeIdForSave(): number | null {
    if (this.mode === 'new') {
      return this.selectedGrupoIdFromRoute;
    }
    return this.currentGrupoEntidadeId;
  }

  private loadTipoAndContinue(): void {
    this.typeService.get(this.tipoEntidadeId).subscribe({
      next: tipo => {
        this.tipoNome = (tipo?.nome || '').trim();
        if (!this.typeAccess.canAccessType(tipo)) {
          this.notify.error('Voce nao possui acesso ao tipo de entidade selecionado.');
          this.back();
          return;
        }
        this.resolveContextAndLoad();
      },
      error: () => {
        this.tipoNome = '';
        if (!this.typeAccess.canAccessType({ id: this.tipoEntidadeId, codigoSeed: null })) {
          this.notify.error('Voce nao possui acesso ao tipo de entidade selecionado.');
          this.back();
          return;
        }
        this.resolveContextAndLoad();
      }
    });
  }

  private formatTipoNome(value: string): string {
    const raw = (value || '').trim();
    if (!raw) return '';
    if (raw === raw.toUpperCase()) {
      return raw
        .toLowerCase()
        .split(' ')
        .filter(part => !!part)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
    }
    return raw;
  }
}
