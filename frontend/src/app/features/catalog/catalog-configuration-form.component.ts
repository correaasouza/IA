import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfigSectionShellComponent } from '../../shared/config-section-shell.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AgrupadoresEmpresaComponent } from '../configs/agrupadores-empresa.component';
import { AgrupadorEmpresa } from '../configs/agrupador-empresa.service';
import {
  CatalogConfiguration,
  CatalogConfigurationByGroup,
  CatalogConfigurationService,
  CatalogConfigurationType,
  CatalogNumberingMode
} from './catalog-configuration.service';

@Component({
  selector: 'app-catalog-configuration-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatRadioModule,
    ConfigSectionShellComponent,
    InlineLoaderComponent,
    AgrupadoresEmpresaComponent
  ],
  templateUrl: './catalog-configuration-form.component.html',
  styleUrls: ['./catalog-configuration-form.component.css']
})
export class CatalogConfigurationFormComponent implements OnChanges {
  @Input({ required: true }) type: CatalogConfigurationType = 'PRODUCTS';
  @ViewChild(AgrupadoresEmpresaComponent) agrupadoresEmpresaComponent?: AgrupadoresEmpresaComponent;
  @ViewChild('groupConfigDialog') groupConfigDialogTpl?: TemplateRef<unknown>;

  loading = false;
  groupsLoading = false;
  groupsError = '';
  error = '';
  config: CatalogConfiguration | null = null;
  groupRows: CatalogConfigurationByGroup[] = [];
  groupModalId: number | null = null;
  groupModalNome = '';
  groupModalEmpresas: Array<{ empresaId: number; nome: string }> = [];
  groupModalNumberingMode: CatalogNumberingMode = 'AUTOMATICA';
  groupModalSaving = false;
  private groupDialogRef: MatDialogRef<unknown> | null = null;

  constructor(
      private service: CatalogConfigurationService,
      private notify: NotificationService,
      private dialog: MatDialog) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['type']) {
      this.load();
    }
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.service.get(this.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: data => {
          this.config = data;
          this.loadGroupRows();
        },
        error: err => {
          this.config = null;
          this.groupRows = [];
          this.error = err?.error?.detail || 'Nao foi possivel carregar a configuracao do catalogo.';
        }
      });
  }

  loadGroupRows(): void {
    if (!this.config?.id) {
      this.groupRows = [];
      this.groupsError = '';
      return;
    }
    this.groupsLoading = true;
    this.groupsError = '';
    this.service.listByGroup(this.type)
      .pipe(finalize(() => (this.groupsLoading = false)))
      .subscribe({
        next: rows => this.groupRows = rows || [],
        error: err => {
          this.groupRows = [];
          this.groupsError = err?.error?.detail || 'Nao foi possivel carregar configuracoes por agrupador.';
        }
      });
  }

  onGroupsChanged(): void {
    this.loadGroupRows();
  }

  openGroupConfigModal(group: AgrupadorEmpresa): void {
    if (!this.groupConfigDialogTpl) return;
    const existing = this.groupRows.find(item => item.agrupadorId === group.id);
    this.groupModalId = group.id;
    this.groupModalNome = group.nome;
    this.groupModalEmpresas = [...(group.empresas || [])];
    this.groupModalNumberingMode = (existing?.numberingMode || 'AUTOMATICA') as CatalogNumberingMode;
    this.groupModalSaving = false;

    this.groupDialogRef = this.dialog.open(this.groupConfigDialogTpl, {
      width: '560px',
      maxWidth: '95vw',
      autoFocus: false,
      restoreFocus: true
    });
    this.groupDialogRef.afterClosed().subscribe(() => {
      this.groupDialogRef = null;
      this.groupModalId = null;
      this.groupModalNome = '';
      this.groupModalEmpresas = [];
      this.groupModalNumberingMode = 'AUTOMATICA';
      this.groupModalSaving = false;
    });
  }

  closeGroupConfigModal(): void {
    if (this.groupDialogRef) {
      this.groupDialogRef.close();
      this.groupDialogRef = null;
    }
  }

  saveGroupConfigModal(): void {
    if (!this.groupModalId || this.groupModalSaving) return;
    this.groupModalSaving = true;
    this.service.updateByGroup(this.type, this.groupModalId, { numberingMode: this.groupModalNumberingMode })
      .pipe(finalize(() => (this.groupModalSaving = false)))
      .subscribe({
        next: updated => {
          const index = this.groupRows.findIndex(item => item.agrupadorId === updated.agrupadorId);
          if (index >= 0) {
            this.groupRows[index] = updated;
          } else {
            this.groupRows = [...this.groupRows, updated];
          }
          this.notify.success('Configuracao do agrupador atualizada.');
          this.closeGroupConfigModal();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a configuracao do agrupador.');
          this.loadGroupRows();
        }
      });
  }

  openGroupEditorFromModal(): void {
    if (!this.groupModalId || !this.agrupadoresEmpresaComponent) return;
    const group: AgrupadorEmpresa = {
      id: this.groupModalId,
      nome: this.groupModalNome || `Grupo ${this.groupModalId}`,
      ativo: true,
      empresas: [...(this.groupModalEmpresas || [])]
    };
    this.closeGroupConfigModal();
    setTimeout(() => this.agrupadoresEmpresaComponent?.startEditForm(group), 0);
  }

  typeLabel(): string {
    return this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos';
  }

  configReferenceName(): string {
    return `Catalogo ${this.typeLabel()}`;
  }

  groupReference(): string {
    if (!this.groupModalId) return 'Grupo';
    const name = (this.groupModalNome || '').trim();
    return name ? `Grupo #${this.groupModalId} - ${name}` : `Grupo #${this.groupModalId}`;
  }
}
