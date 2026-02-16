import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import {
  CatalogConfigurationService,
  CatalogConfigurationType,
  CatalogStockAdjustment,
  CatalogStockAdjustmentScopeOption,
  CatalogStockAdjustmentUpsertPayload
} from './catalog-configuration.service';

export interface CatalogStockAdjustmentFormDialogData {
  type: CatalogConfigurationType;
  adjustment?: CatalogStockAdjustment | null;
}

@Component({
  selector: 'app-catalog-stock-adjustment-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatSelectModule
  ],
  templateUrl: './catalog-stock-adjustment-form-dialog.component.html',
  styleUrls: ['./catalog-stock-adjustment-form-dialog.component.css']
})
export class CatalogStockAdjustmentFormDialogComponent implements OnInit {
  loading = false;
  saving = false;
  error = '';
  options: CatalogStockAdjustmentScopeOption[] = [];

  form: {
    nome: string;
    tipo: 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
    ordem: number | null;
    active: boolean;
    origemKey: string | null;
    destinoKey: string | null;
  } = {
    nome: '',
    tipo: 'ENTRADA',
    ordem: null,
    active: true,
    origemKey: null,
    destinoKey: null
  };

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: CatalogStockAdjustmentFormDialogData,
    private dialogRef: MatDialogRef<CatalogStockAdjustmentFormDialogComponent>,
    private service: CatalogConfigurationService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.applyAdjustmentToForm(this.data.adjustment || null);
    this.loadOptions();
  }

  close(saved = false): void {
    this.dialogRef.close(saved);
  }

  title(): string {
    return this.data.adjustment?.id ? 'Editar tipo de ajuste de estoque' : 'Novo tipo de ajuste de estoque';
  }

  codeLabel(): string {
    return this.data.adjustment?.codigo || 'Automatico por locatario';
  }

  originOptions(): CatalogStockAdjustmentScopeOption[] {
    return this.options || [];
  }

  destinationOptions(): CatalogStockAdjustmentScopeOption[] {
    return this.options || [];
  }

  requiresOrigin(): boolean {
    return this.form.tipo === 'SAIDA' || this.form.tipo === 'TRANSFERENCIA';
  }

  requiresDestination(): boolean {
    return this.form.tipo === 'ENTRADA' || this.form.tipo === 'TRANSFERENCIA';
  }

  onTypeChanged(): void {
    if (!this.requiresOrigin()) {
      this.form.origemKey = null;
    }
    if (!this.requiresDestination()) {
      this.form.destinoKey = null;
    }
  }

  canSave(): boolean {
    if (this.saving || this.loading) return false;
    if (!(this.form.nome || '').trim()) return false;
    if (this.requiresOrigin() && !this.form.origemKey) return false;
    if (this.requiresDestination() && !this.form.destinoKey) return false;
    if (this.form.tipo === 'TRANSFERENCIA' && this.form.origemKey && this.form.destinoKey && this.form.origemKey === this.form.destinoKey) {
      return false;
    }
    return true;
  }

  save(): void {
    if (!this.canSave()) return;
    const payload = this.buildPayload();
    this.saving = true;

    const request$ = this.data.adjustment?.id
      ? this.service.updateStockAdjustmentByType(this.data.type, this.data.adjustment.id, payload)
      : this.service.createStockAdjustmentByType(this.data.type, payload);

    request$
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success(this.data.adjustment?.id ? 'Tipo de ajuste atualizado.' : 'Tipo de ajuste cadastrado.');
          this.close(true);
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel salvar o tipo de ajuste.');
        }
      });
  }

  scopeLabel(key: string | null): string {
    if (!key) return '-';
    const option = this.findOptionByKey(key);
    return option?.label || key;
  }

  private loadOptions(): void {
    this.loading = true;
    this.error = '';
    this.service.listStockAdjustmentScopeOptionsByType(this.data.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.options = rows || [];
          this.ensureExistingSelection();
        },
        error: err => {
          this.options = [];
          this.error = err?.error?.detail || 'Nao foi possivel carregar opcoes de estoque.';
        }
      });
  }

  private applyAdjustmentToForm(adjustment: CatalogStockAdjustment | null): void {
    if (!adjustment) return;
    this.form.nome = adjustment.nome || '';
    this.form.tipo = (adjustment.tipo || 'ENTRADA') as 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
    this.form.ordem = adjustment.ordem ?? null;
    this.form.active = adjustment.active !== false;
    this.form.origemKey = this.toKey(
      adjustment.estoqueOrigemAgrupadorId,
      adjustment.estoqueOrigemTipoId,
      adjustment.estoqueOrigemFilialId);
    this.form.destinoKey = this.toKey(
      adjustment.estoqueDestinoAgrupadorId,
      adjustment.estoqueDestinoTipoId,
      adjustment.estoqueDestinoFilialId);
    this.onTypeChanged();
  }

  private ensureExistingSelection(): void {
    if (this.form.origemKey && !this.findOptionByKey(this.form.origemKey)) {
      this.form.origemKey = null;
    }
    if (this.form.destinoKey && !this.findOptionByKey(this.form.destinoKey)) {
      this.form.destinoKey = null;
    }
  }

  private buildPayload(): CatalogStockAdjustmentUpsertPayload {
    const payload: CatalogStockAdjustmentUpsertPayload = {
      nome: (this.form.nome || '').trim(),
      tipo: this.form.tipo,
      ordem: this.form.ordem,
      active: this.form.active
    };

    const origem = this.parseKey(this.form.origemKey);
    const destino = this.parseKey(this.form.destinoKey);

    payload.estoqueOrigemAgrupadorId = origem?.agrupadorId ?? null;
    payload.estoqueOrigemTipoId = origem?.estoqueTipoId ?? null;
    payload.estoqueOrigemFilialId = origem?.filialId ?? null;
    payload.estoqueDestinoAgrupadorId = destino?.agrupadorId ?? null;
    payload.estoqueDestinoTipoId = destino?.estoqueTipoId ?? null;
    payload.estoqueDestinoFilialId = destino?.filialId ?? null;

    return payload;
  }

  private toKey(agrupadorId: number | null, estoqueTipoId: number | null, filialId: number | null): string | null {
    if (!agrupadorId || !estoqueTipoId || !filialId) return null;
    return `${agrupadorId}|${estoqueTipoId}|${filialId}`;
  }

  private parseKey(key: string | null): { agrupadorId: number; estoqueTipoId: number; filialId: number } | null {
    if (!key) return null;
    const parts = key.split('|').map(item => Number(item || 0));
    if (parts.length !== 3 || parts.some(item => !Number.isFinite(item) || item <= 0)) return null;
    const [agrupadorId, estoqueTipoId, filialId] = parts;
    if (!agrupadorId || !estoqueTipoId || !filialId) return null;
    return {
      agrupadorId,
      estoqueTipoId,
      filialId
    };
  }

  private findOptionByKey(key: string): CatalogStockAdjustmentScopeOption | undefined {
    return (this.options || []).find(option => this.toKey(option.agrupadorId, option.estoqueTipoId, option.filialId) === key);
  }
}
