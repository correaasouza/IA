import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Observable } from 'rxjs';
import { forkJoin } from 'rxjs';
import { of } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { NotificationService } from '../../core/notifications/notification.service';
import { AgrupadoresEmpresaComponent } from '../configs/agrupadores-empresa.component';
import { AgrupadorEmpresa } from '../configs/agrupador-empresa.service';
import {
  EntityTypeConfigByGroupService,
  TipoEntidadeConfigPorAgrupador,
  EntidadeFormConfigByGroup,
  EntidadeFormGroupConfig,
  EntidadeFormFieldConfig
} from './entity-type-config-by-group.service';
import { EntityTypeService, TipoEntidade } from './entity-type.service';

@Component({
  selector: 'app-entity-type-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    InlineLoaderComponent,
    AccessControlDirective,
    AgrupadoresEmpresaComponent
  ],
  templateUrl: './entity-type-form.component.html',
  styleUrls: ['./entity-type-form.component.css']
})
export class EntityTypeFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo tipo de entidade';
  tipoEntidade: TipoEntidade | null = null;
  loading = false;
  saving = false;
  deleting = false;
  configLoading = false;
  configRows: TipoEntidadeConfigPorAgrupador[] = [];

  configTabGroupId: number | null = null;
  configTabGroupNome = '';
  configTabGroupEmpresas: Array<{ empresaId: number; nome: string }> = [];
  configTabObrigarUmTelefone = false;
  configTabFormConfig: EntidadeFormConfigByGroup | null = null;
  configTabFormLoading = false;
  configTabSaving = false;
  private configDraftsByGroupId: Record<number, { obrigarUmTelefone: boolean; groups: EntidadeFormGroupConfig[] }> = {};
  private configTabDirty = false;
  private configAutosaveTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly configNavIconByGroupKey: Record<string, string> = {
    DADOS_ENTIDADE: 'account_box',
    ENDERECOS: 'home',
    CONTATOS: 'contact_phone',
    DOCUMENTACAO: 'description',
    COMERCIAL_FISCAL: 'business_center',
    RH: 'groups',
    FAMILIARES_REFERENCIAS: 'family_restroom'
  };

  form = this.fb.group({
    nome: ['', Validators.required],
    ativo: [true, Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private service: EntityTypeService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService,
    private configByGroupService: EntityTypeConfigByGroupService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (idParam) {
      const id = Number(idParam);
      this.mode = isEdit ? 'edit' : 'view';
      this.title = this.mode === 'edit' ? 'Editar tipo de entidade' : 'Consultar tipo de entidade';
      this.load(id);
      return;
    }
    this.mode = 'new';
    this.title = 'Novo tipo de entidade';
    this.form.enable();
  }

  private load(id: number): void {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => (this.loading = false))).subscribe({
      next: data => {
        this.tipoEntidade = data;
        this.form.patchValue({
          nome: data.nome,
          ativo: data.ativo
        });
        this.applyModeState();
        this.loadConfigPorAgrupador();
      },
      error: () => this.notify.error('Nao foi possivel carregar o tipo de entidade.')
    });
  }

  toEdit(): void {
    if (!this.tipoEntidade) return;
    this.router.navigate(['/entity-types', this.tipoEntidade.id, 'edit']);
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      nome: (this.form.value.nome || '').trim(),
      ativo: !!this.form.value.ativo
    };
    this.saving = true;
    if (this.mode === 'new') {
      this.service.create(payload).pipe(finalize(() => (this.saving = false))).subscribe({
        next: () => {
          this.notify.success('Tipo de entidade criado.');
          this.router.navigateByUrl('/entity-types');
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar o tipo de entidade.')
      });
      return;
    }
    if (!this.tipoEntidade) {
      this.saving = false;
      return;
    }
    this.flushConfigAutosave();
    this.service.update(this.tipoEntidade.id, payload).pipe(finalize(() => (this.saving = false))).subscribe({
      next: updated => {
        this.tipoEntidade = updated;
        this.persistConfigTabOnMainSave(updated.id).subscribe({
          next: () => {
            this.notify.success('Tipo de entidade atualizado.');
            this.router.navigateByUrl('/entity-types');
          },
          error: (err: any) => this.notify.error(err?.error?.detail || 'Tipo atualizado, mas nao foi possivel salvar a configuracao da ficha.')
        });
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o tipo de entidade.')
    });
  }

  remove(): void {
    if (!this.tipoEntidade || this.mode === 'new') return;
    if (this.tipoEntidade.tipoPadrao) {
      this.notify.error('Tipos padrao do sistema nao podem ser excluidos.');
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir tipo de entidade',
        message: `Deseja excluir o tipo "${this.tipoEntidade.nome}"?`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.tipoEntidade) return;
      this.deleting = true;
      this.service.delete(this.tipoEntidade.id).pipe(finalize(() => (this.deleting = false))).subscribe({
        next: () => {
          this.notify.success('Tipo de entidade excluido.');
          this.router.navigateByUrl('/entity-types');
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o tipo de entidade.')
      });
    });
  }

  back(): void {
    this.router.navigateByUrl('/entity-types');
  }

  onAgrupadoresChanged(): void {
    this.loadConfigPorAgrupador();
  }

  onGroupEditStarted(group: AgrupadorEmpresa): void {
    this.flushConfigAutosave();
    this.stashCurrentConfigDraft();
    this.configTabGroupId = group.id;
    this.configTabGroupNome = group.nome;
    this.configTabGroupEmpresas = [...(group.empresas || [])];
    this.applyConfigFromRows(group.id);
    const draft = this.configDraftsByGroupId[group.id];
    if (draft) {
      this.configTabFormConfig = {
        tipoEntidadeId: this.tipoEntidade?.id || 0,
        agrupadorId: group.id,
        agrupadorNome: group.nome,
        obrigarUmTelefone: !!draft.obrigarUmTelefone,
        groups: this.cloneGroups(draft.groups)
      };
      this.configTabObrigarUmTelefone = !!draft.obrigarUmTelefone;
      this.configTabFormLoading = false;
      return;
    }
    this.loadFormConfigTab(group.id);
  }

  saveConfigTab(): void {
    if (!this.tipoEntidade?.id || !this.configTabGroupId || this.mode === 'view' || this.configTabSaving) return;
    this.configTabSaving = true;
    const groups = (this.configTabFormConfig?.groups || []).map(group => ({
      ...group,
      fields: (group.fields || []).map(field => ({ ...field }))
    }));
    this.configByGroupService.updateFormConfig(this.tipoEntidade.id, this.configTabGroupId, {
      obrigarUmTelefone: this.configTabObrigarUmTelefone,
      groups
    })
      .pipe(finalize(() => (this.configTabSaving = false)))
      .subscribe({
        next: updated => {
          this.configTabFormConfig = updated;
          this.configTabObrigarUmTelefone = !!updated.obrigarUmTelefone;
          const row = {
            agrupadorId: updated.agrupadorId,
            agrupadorNome: updated.agrupadorNome,
            obrigarUmTelefone: !!updated.obrigarUmTelefone,
            ativo: true
          } as TipoEntidadeConfigPorAgrupador;
          const index = this.configRows.findIndex(item => item.agrupadorId === updated.agrupadorId);
          if (index >= 0) {
            this.configRows[index] = row;
          } else {
            this.configRows = [...this.configRows, row];
          }
          this.notify.success('Configuracao do agrupador atualizada.');
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a configuracao do agrupador.');
          this.loadConfigPorAgrupador();
        }
      });
  }

  private persistConfigTabOnMainSave(tipoEntidadeId: number): Observable<unknown> {
    this.stashCurrentConfigDraft();
    const draftEntries = Object.entries(this.configDraftsByGroupId);
    if (!draftEntries.length) {
      return of(null);
    }
    const saves = draftEntries.map(([groupIdText, draft]) => {
      const groupId = Number(groupIdText);
      return this.configByGroupService.updateFormConfig(tipoEntidadeId, groupId, {
        obrigarUmTelefone: !!draft.obrigarUmTelefone,
        groups: this.cloneGroups(draft.groups)
      });
    });
    return forkJoin(saves);
  }

  configOriginName(): string {
    return (this.form.get('nome')?.value || this.tipoEntidade?.nome || '').trim();
  }

  loadConfigPorAgrupador(): void {
    if (!this.tipoEntidade?.id) {
      this.configRows = [];
      return;
    }
    this.configLoading = true;
    this.configByGroupService.list(this.tipoEntidade.id)
      .pipe(finalize(() => (this.configLoading = false)))
      .subscribe({
        next: data => {
          this.configRows = data || [];
          if (this.configTabGroupId) {
            this.applyConfigFromRows(this.configTabGroupId);
          }
        },
        error: () => {
          this.configRows = [];
          this.notify.error('Nao foi possivel carregar as configuracoes por agrupador.');
        }
      });
  }

  private applyConfigFromRows(groupId: number): void {
    const row = this.configRows.find(item => item.agrupadorId === groupId);
    this.configTabObrigarUmTelefone = !!row?.obrigarUmTelefone;
  }

  private loadFormConfigTab(groupId: number): void {
    if (!this.tipoEntidade?.id || !groupId) {
      this.configTabFormConfig = null;
      return;
    }
    this.configTabFormLoading = true;
    this.configByGroupService.getFormConfig(this.tipoEntidade.id, groupId)
      .pipe(finalize(() => (this.configTabFormLoading = false)))
      .subscribe({
        next: data => {
          this.configTabFormConfig = data;
          this.configTabObrigarUmTelefone = !!data?.obrigarUmTelefone;
          this.writeDraft(groupId, this.configTabObrigarUmTelefone, data?.groups || []);
        },
        error: err => {
          this.configTabFormConfig = null;
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a configuracao da ficha por agrupador.');
        }
      });
  }

  toggleGroupEnabled(group: EntidadeFormGroupConfig, value: boolean): void {
    group.enabled = !!value;
    this.onConfigChanged();
  }

  toggleFieldVisible(field: EntidadeFormFieldConfig, value: boolean): void {
    field.visible = !!value;
    this.onConfigChanged();
  }

  toggleFieldEditable(field: EntidadeFormFieldConfig, value: boolean): void {
    field.editable = !!value;
    this.onConfigChanged();
  }

  toggleFieldRequired(field: EntidadeFormFieldConfig, value: boolean): void {
    field.required = !!value;
    this.onConfigChanged();
  }

  configGroupSectionId(group: EntidadeFormGroupConfig, index: number): string {
    const key = (group?.groupKey || `grupo-${index}`).toLowerCase().replace(/[^a-z0-9_-]+/g, '-');
    return `cfg-group-${key}-${index}`;
  }

  configRulesSectionId(): string {
    return 'cfg-group-rules';
  }

  configNavItems(): Array<{ id: string; label: string; icon: string }> {
    const items: Array<{ id: string; label: string; icon: string }> = [
      { id: this.configRulesSectionId(), label: 'Regras', icon: 'rule' }
    ];
    const groups = this.configTabFormConfig?.groups || [];
    groups.forEach((group, index) => {
      const key = (group.groupKey || '').trim().toUpperCase();
      items.push({
        id: this.configGroupSectionId(group, index),
        label: (group.label || group.groupKey || `Grupo ${index + 1}`).trim(),
        icon: this.configNavIconByGroupKey[key] || 'view_list'
      });
    });
    return items;
  }

  scrollToConfigSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (!element) return;
    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  isContatosGroup(group: EntidadeFormGroupConfig | null | undefined): boolean {
    return ((group?.groupKey || '').trim().toUpperCase() === 'CONTATOS');
  }

  onConfigObrigarTelefoneChange(value: boolean): void {
    this.configTabObrigarUmTelefone = !!value;
    this.onConfigChanged();
  }

  private stashCurrentConfigDraft(): void {
    if (!this.configTabGroupId || !this.configTabFormConfig) return;
    this.writeDraft(this.configTabGroupId, this.configTabObrigarUmTelefone, this.configTabFormConfig.groups || []);
  }

  private onConfigChanged(): void {
    this.configTabDirty = true;
    this.stashCurrentConfigDraft();
    this.scheduleConfigAutosave();
  }

  private scheduleConfigAutosave(): void {
    if (this.configAutosaveTimer) {
      clearTimeout(this.configAutosaveTimer);
    }
    this.configAutosaveTimer = setTimeout(() => {
      this.persistCurrentConfigTab(true);
    }, 650);
  }

  private flushConfigAutosave(): void {
    if (this.configAutosaveTimer) {
      clearTimeout(this.configAutosaveTimer);
      this.configAutosaveTimer = null;
    }
    this.persistCurrentConfigTab(false);
  }

  private persistCurrentConfigTab(silent: boolean): void {
    if (!this.configTabDirty) return;
    if (!this.tipoEntidade?.id || !this.configTabGroupId || !this.configTabFormConfig) return;
    const groupId = this.configTabGroupId;
    const groups = this.cloneGroups(this.configTabFormConfig.groups || []);
    this.configTabSaving = true;
    this.configByGroupService.updateFormConfig(this.tipoEntidade.id, groupId, {
      obrigarUmTelefone: this.configTabObrigarUmTelefone,
      groups
    })
      .pipe(finalize(() => (this.configTabSaving = false)))
      .subscribe({
        next: updated => {
          this.configTabDirty = false;
          this.configTabFormConfig = updated;
          this.configTabObrigarUmTelefone = !!updated.obrigarUmTelefone;
          this.writeDraft(groupId, this.configTabObrigarUmTelefone, updated.groups || []);
          const row = {
            agrupadorId: updated.agrupadorId,
            agrupadorNome: updated.agrupadorNome,
            obrigarUmTelefone: !!updated.obrigarUmTelefone,
            ativo: true
          } as TipoEntidadeConfigPorAgrupador;
          const index = this.configRows.findIndex(item => item.agrupadorId === updated.agrupadorId);
          if (index >= 0) this.configRows[index] = row;
          else this.configRows = [...this.configRows, row];
          if (!silent) this.notify.success('Configuracao do agrupador salva.');
        },
        error: err => {
          if (!silent) this.notify.error(err?.error?.detail || 'Nao foi possivel salvar a configuracao do agrupador.');
        }
      });
  }

  private writeDraft(groupId: number, obrigarUmTelefone: boolean, groups: EntidadeFormGroupConfig[]): void {
    this.configDraftsByGroupId[groupId] = {
      obrigarUmTelefone: !!obrigarUmTelefone,
      groups: this.cloneGroups(groups)
    };
  }

  private cloneGroups(groups: EntidadeFormGroupConfig[]): EntidadeFormGroupConfig[] {
    return (groups || []).map(group => ({
      ...group,
      fields: (group.fields || []).map(field => ({ ...field }))
    }));
  }

  private applyModeState(): void {
    if (this.mode === 'view') {
      this.form.disable();
    } else {
      this.form.enable();
      if (this.tipoEntidade?.tipoPadrao) {
        this.form.controls.ativo.disable();
      }
    }
  }
}
