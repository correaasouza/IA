import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { AgrupadoresEmpresaComponent } from '../configs/agrupadores-empresa.component';
import { AgrupadorEmpresa } from '../configs/agrupador-empresa.service';
import { EntityTypeConfigByGroupService, TipoEntidadeConfigPorAgrupador } from './entity-type-config-by-group.service';
import { EntityTypeService, TipoEntidade } from './entity-type.service';

@Component({
  selector: 'app-entity-type-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    InlineLoaderComponent,
    AgrupadoresEmpresaComponent
  ],
  templateUrl: './entity-type-form.component.html',
  styleUrls: ['./entity-type-form.component.css']
})
export class EntityTypeFormComponent implements OnInit {
  @ViewChild('configPorGrupoDialog') configPorGrupoDialogTpl?: TemplateRef<unknown>;
  @ViewChild(AgrupadoresEmpresaComponent) agrupadoresEmpresaComponent?: AgrupadoresEmpresaComponent;

  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo tipo de entidade';
  tipoEntidade: TipoEntidade | null = null;
  loading = false;
  saving = false;
  deleting = false;
  configLoading = false;
  configRows: TipoEntidadeConfigPorAgrupador[] = [];
  configModalGroupId: number | null = null;
  configModalGroupNome = '';
  configModalObrigarUmTelefone = false;
  configModalGroupEmpresas: Array<{ empresaId: number; nome: string }> = [];
  configModalSaving = false;
  private configDialogRef: MatDialogRef<unknown> | null = null;

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
        next: created => {
          this.notify.success('Tipo de entidade criado.');
          this.router.navigate(['/entity-types', created.id]);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar o tipo de entidade.')
      });
      return;
    }
    if (!this.tipoEntidade) {
      this.saving = false;
      return;
    }
    this.service.update(this.tipoEntidade.id, payload).pipe(finalize(() => (this.saving = false))).subscribe({
      next: updated => {
        this.tipoEntidade = updated;
        this.notify.success('Tipo de entidade atualizado.');
        this.router.navigate(['/entity-types', updated.id]);
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

  openConfigPorGrupoModal(group: AgrupadorEmpresa): void {
    if (!this.tipoEntidade?.id || !this.configPorGrupoDialogTpl) return;
    const existing = this.configRows.find(row => row.agrupadorId === group.id);
    this.configModalGroupId = group.id;
    this.configModalGroupNome = group.nome;
    this.configModalObrigarUmTelefone = !!existing?.obrigarUmTelefone;
    this.configModalGroupEmpresas = [...(group.empresas || [])];
    this.configModalSaving = false;

    this.configDialogRef = this.dialog.open(this.configPorGrupoDialogTpl, {
      width: '560px',
      maxWidth: '95vw',
      autoFocus: false,
      restoreFocus: true
    });
    this.configDialogRef.afterClosed().subscribe(() => {
      this.configDialogRef = null;
      this.configModalGroupId = null;
      this.configModalGroupNome = '';
      this.configModalGroupEmpresas = [];
      this.configModalSaving = false;
    });
  }

  configOriginName(): string {
    return (this.form.get('nome')?.value || this.tipoEntidade?.nome || '').trim();
  }

  configOriginReference(): string {
    if (!this.tipoEntidade?.id) return 'Tipo de entidade';
    const name = this.configOriginName();
    if (name) {
      return `Tipo de entidade #${this.tipoEntidade.id} - ${name}`;
    }
    return `Tipo de entidade #${this.tipoEntidade.id}`;
  }

  configGroupReference(): string {
    if (!this.configModalGroupId) return 'Grupo';
    const name = (this.configModalGroupNome || '').trim();
    if (name) {
      return `Grupo #${this.configModalGroupId} - ${name}`;
    }
    return `Grupo #${this.configModalGroupId}`;
  }

  closeConfigPorGrupoModal(): void {
    if (this.configDialogRef) {
      this.configDialogRef.close();
      this.configDialogRef = null;
    }
  }

  saveConfigPorGrupoModal(): void {
    if (!this.tipoEntidade?.id || !this.configModalGroupId || this.mode === 'view') return;
    const agrupadorId = this.configModalGroupId;
    this.configModalSaving = true;
    this.configByGroupService.update(this.tipoEntidade.id, agrupadorId, this.configModalObrigarUmTelefone)
      .pipe(finalize(() => {
        this.configModalSaving = false;
      }))
      .subscribe({
        next: updated => {
          const index = this.configRows.findIndex(item => item.agrupadorId === updated.agrupadorId);
          if (index >= 0) {
            this.configRows[index] = updated;
          } else {
            this.configRows = [...this.configRows, updated];
          }
          this.notify.success('Configuracao do agrupador atualizada.');
          this.closeConfigPorGrupoModal();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a configuracao do agrupador.');
          this.loadConfigPorAgrupador();
        }
      });
  }

  openAgrupadorFormFromConfigModal(): void {
    if (!this.configModalGroupId || !this.agrupadoresEmpresaComponent) return;
    const group: AgrupadorEmpresa = {
      id: this.configModalGroupId,
      nome: this.configModalGroupNome || `Grupo ${this.configModalGroupId}`,
      ativo: true,
      empresas: (this.configModalGroupEmpresas || []).map(item => ({
        empresaId: item.empresaId,
        nome: item.nome
      }))
    };
    this.closeConfigPorGrupoModal();
    setTimeout(() => this.agrupadoresEmpresaComponent?.startEditForm(group), 0);
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
        next: data => this.configRows = data || [],
        error: () => {
          this.configRows = [];
          this.notify.error('Nao foi possivel carregar as configuracoes por agrupador.');
        }
      });
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
