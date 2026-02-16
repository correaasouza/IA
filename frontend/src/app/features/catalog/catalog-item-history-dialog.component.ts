import { CommonModule } from '@angular/common';
import { Component, HostListener, Inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { finalize } from 'rxjs/operators';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { CatalogCrudType } from './catalog-item.service';
import {
  CatalogMovement,
  CatalogMovementMetricType,
  CatalogMovementOriginType,
  CatalogStockBalanceRow,
  CatalogStockConsolidatedRow,
  CatalogStockService
} from './catalog-stock.service';

export interface CatalogItemHistoryDialogData {
  type: CatalogCrudType;
  itemId: number;
  itemCodigo: number;
  itemNome: string;
}

@Component({
  selector: 'app-catalog-item-history-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    InlineLoaderComponent
  ],
  templateUrl: './catalog-item-history-dialog.component.html',
  styleUrls: ['./catalog-item-history-dialog.component.css']
})
export class CatalogItemHistoryDialogComponent implements OnInit {
  isMobile = false;
  mobileFiltersOpen = false;
  loading = false;
  error = '';

  stockRows: CatalogStockBalanceRow[] = [];
  stockConsolidatedRows: CatalogStockConsolidatedRow[] = [];
  ledgerEntries: CatalogMovement[] = [];
  ledgerDisplayEntries: CatalogMovement[] = [];
  ledgerDisplayLines: CatalogLedgerLineRow[] = [];
  ledgerPageIndex = 0;
  ledgerPageSize = 10;
  ledgerTotalElements = 0;
  ledgerSortOrder: 'RECENT' | 'OLDEST' = 'RECENT';

  ledgerOrigins: Array<{ value: CatalogMovementOriginType; label: string }> = [
    { value: 'MUDANCA_GRUPO', label: 'Mudanca de grupo' },
    { value: 'SYSTEM', label: 'Sistema' }
  ];
  ledgerMetrics: Array<{ value: CatalogMovementMetricType; label: string }> = [
    { value: 'QUANTIDADE', label: 'Quantidade' },
    { value: 'PRECO', label: 'Preco' }
  ];

  ledgerFilters = this.fb.group({
    origemTipo: [''],
    metricType: [''],
    estoqueTipoId: [''],
    filialId: [''],
    fromDate: [''],
    toDate: ['']
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: CatalogItemHistoryDialogData,
    private dialogRef: MatDialogRef<CatalogItemHistoryDialogComponent>,
    private fb: FormBuilder,
    private stockService: CatalogStockService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.refresh(true);
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  close(): void {
    this.dialogRef.close();
  }

  refresh(resetPage = false): void {
    if (resetPage) this.ledgerPageIndex = 0;
    this.loadBalancesAndLedger();
  }

  applyLedgerFilters(): void {
    this.ledgerPageIndex = 0;
    this.loadLedger();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    const value = this.ledgerFilters.value;
    let count = 0;
    if ((value.origemTipo || '').trim()) count++;
    if ((value.metricType || '').trim()) count++;
    if (`${value.estoqueTipoId || ''}`.trim()) count++;
    if (`${value.filialId || ''}`.trim()) count++;
    if ((value.fromDate || '').trim()) count++;
    if ((value.toDate || '').trim()) count++;
    return count;
  }

  clearLedgerFilters(): void {
    this.ledgerFilters.patchValue({
      origemTipo: '',
      metricType: '',
      estoqueTipoId: '',
      filialId: '',
      fromDate: '',
      toDate: ''
    });
    this.applyLedgerFilters();
  }

  hasLedgerFiltersActive(): boolean {
    const value = this.ledgerFilters.value;
    return !!((value.origemTipo || '').trim()
      || (value.metricType || '').trim()
      || `${value.estoqueTipoId || ''}`.trim()
      || `${value.filialId || ''}`.trim()
      || (value.fromDate || '').trim()
      || (value.toDate || '').trim());
  }

  activeLedgerFilterChips(): Array<{ key: string; label: string }> {
    const chips: Array<{ key: string; label: string }> = [];
    const value = this.ledgerFilters.value;

    const origem = (value.origemTipo || '').trim();
    if (origem) {
      chips.push({ key: 'origemTipo', label: `Origem: ${this.originLabel(origem)}` });
    }

    const metric = (value.metricType || '').trim();
    if (metric) {
      chips.push({ key: 'metricType', label: `Metrica: ${this.metricLabel(metric)}` });
    }

    const estoqueTipoId = this.toPositive((value.estoqueTipoId || '').trim());
    if (estoqueTipoId) {
      const stockType = this.ledgerStockTypeOptions().find(item => item.id === estoqueTipoId);
      chips.push({ key: 'estoqueTipoId', label: `Estoque: ${stockType?.label || '#' + estoqueTipoId}` });
    }

    const filialId = this.toPositive((value.filialId || '').trim());
    if (filialId) {
      const filial = this.ledgerFilialOptions().find(item => item.id === filialId);
      chips.push({ key: 'filialId', label: `Filial: ${filial?.label || '#' + filialId}` });
    }

    const fromDate = (value.fromDate || '').trim();
    if (fromDate) {
      chips.push({ key: 'fromDate', label: `De: ${this.formatDateLabel(fromDate)}` });
    }

    const toDate = (value.toDate || '').trim();
    if (toDate) {
      chips.push({ key: 'toDate', label: `Ate: ${this.formatDateLabel(toDate)}` });
    }

    return chips;
  }

  clearLedgerFilter(key: string): void {
    const control = this.ledgerFilters.get(key);
    if (!control) return;
    control.setValue('');
    this.applyLedgerFilters();
  }

  setLedgerSortOrder(value: 'RECENT' | 'OLDEST'): void {
    this.ledgerSortOrder = value === 'OLDEST' ? 'OLDEST' : 'RECENT';
    this.applyLedgerSort();
  }

  onLedgerPageChange(event: PageEvent): void {
    this.ledgerPageIndex = event.pageIndex;
    this.ledgerPageSize = event.pageSize;
    this.loadLedger();
  }

  ledgerStockTypeOptions(): Array<{ id: number; label: string }> {
    const map = new Map<number, string>();
    for (const row of this.stockConsolidatedRows || []) {
      map.set(row.estoqueTipoId, row.estoqueTipoNome || row.estoqueTipoCodigo || `#${row.estoqueTipoId}`);
    }
    return [...map.entries()]
      .map(([id, label]) => ({ id, label }))
      .sort((a, b) => a.label.localeCompare(b.label, 'pt-BR', { sensitivity: 'base' }));
  }

  ledgerFilialOptions(): Array<{ id: number; label: string }> {
    const map = new Map<number, string>();
    for (const row of this.stockRows || []) {
      map.set(row.filialId, row.filialNome || `Filial #${row.filialId}`);
    }
    return [...map.entries()]
      .map(([id, label]) => ({ id, label }))
      .sort((a, b) => a.label.localeCompare(b.label, 'pt-BR', { sensitivity: 'base' }));
  }

  originLabel(value: string | undefined | null): string {
    if (value === 'MUDANCA_GRUPO') return 'Mudanca de grupo';
    if (value === 'SYSTEM') return 'Sistema';
    return value || '-';
  }

  metricLabel(value: string): string {
    return value === 'PRECO' ? 'Preco' : 'Quantidade';
  }

  formatMetricValue(value: number | null): string {
    if (value === null || value === undefined) return '-';
    return value.toLocaleString('pt-BR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 6
    });
  }

  private loadBalancesAndLedger(): void {
    this.loading = true;
    this.error = '';
    this.stockService.getBalances(this.data.type, this.data.itemId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: view => {
          this.stockRows = view?.rows || [];
          this.stockConsolidatedRows = view?.consolidado || [];
          this.loadLedger();
        },
        error: err => {
          this.stockRows = [];
          this.stockConsolidatedRows = [];
          this.ledgerEntries = [];
          this.ledgerDisplayEntries = [];
          this.ledgerDisplayLines = [];
          this.ledgerTotalElements = 0;
          this.error = err?.error?.detail || 'Nao foi possivel carregar historico do item.';
        }
      });
  }

  private loadLedger(): void {
    const filters = this.ledgerFilters.value;
    this.loading = true;
    this.stockService.getLedger(this.data.type, this.data.itemId, {
      page: this.ledgerPageIndex,
      size: this.ledgerPageSize,
      origemTipo: (filters.origemTipo || '').trim() as CatalogMovementOriginType | '',
      metricType: (filters.metricType || '').trim() as CatalogMovementMetricType | '',
      estoqueTipoId: this.toPositive((filters.estoqueTipoId || '').trim()),
      filialId: this.toPositive((filters.filialId || '').trim()),
      fromDate: this.toLedgerDateIsoStart((filters.fromDate || '').trim()),
      toDate: this.toLedgerDateIsoEnd((filters.toDate || '').trim())
    }).pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: payload => {
          this.ledgerEntries = payload?.content || [];
          this.ledgerTotalElements = this.extractTotalElements(payload);
          this.applyLedgerSort();
        },
        error: err => {
          this.ledgerEntries = [];
          this.ledgerDisplayEntries = [];
          this.ledgerDisplayLines = [];
          this.ledgerTotalElements = 0;
          this.error = err?.error?.detail || 'Nao foi possivel carregar historico do item.';
        }
      });
  }

  private applyLedgerSort(): void {
    this.ledgerDisplayEntries = [...(this.ledgerEntries || [])].sort((a, b) => {
      const aTime = new Date(a?.dataHoraMovimentacao || 0).getTime();
      const bTime = new Date(b?.dataHoraMovimentacao || 0).getTime();
      if (aTime !== bTime) {
        return this.ledgerSortOrder === 'OLDEST' ? aTime - bTime : bTime - aTime;
      }
      const aid = Number(a?.id || 0);
      const bid = Number(b?.id || 0);
      return this.ledgerSortOrder === 'OLDEST' ? aid - bid : bid - aid;
    });
    this.syncLedgerDisplayLines();
  }

  private syncLedgerDisplayLines(): void {
    const flattened: CatalogLedgerLineRow[] = [];
    for (const movement of this.ledgerDisplayEntries || []) {
      if (!movement?.lines?.length) {
        flattened.push({
          movementId: movement.id,
          origemMovimentacaoTipo: movement.origemMovimentacaoTipo,
          dataHoraMovimentacao: movement.dataHoraMovimentacao,
          observacao: movement.observacao || '',
          metricType: '',
          estoqueTipoNome: '-',
          filialNome: '-',
          beforeValue: null,
          delta: null,
          afterValue: null
        });
        continue;
      }

      for (const line of movement.lines) {
        flattened.push({
          movementId: movement.id,
          origemMovimentacaoTipo: movement.origemMovimentacaoTipo,
          dataHoraMovimentacao: movement.dataHoraMovimentacao,
          observacao: movement.observacao || '',
          metricType: line.metricType,
          estoqueTipoNome: line.estoqueTipoNome || line.estoqueTipoCodigo || `#${line.estoqueTipoId}`,
          filialNome: line.filialNome || `Filial #${line.filialId}`,
          beforeValue: line.beforeValue,
          delta: line.delta,
          afterValue: line.afterValue
        });
      }
    }
    this.ledgerDisplayLines = flattened;
  }

  private extractTotalElements(payload: any): number {
    if (typeof payload?.totalElements === 'number') return payload.totalElements;
    if (typeof payload?.page?.totalElements === 'number') return payload.page.totalElements;
    return (payload?.content || []).length;
  }

  private toPositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private toLedgerDateIsoStart(value: string): string | undefined {
    if (!value) return undefined;
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
  }

  private toLedgerDateIsoEnd(value: string): string | undefined {
    if (!value) return undefined;
    const date = new Date(`${value}T23:59:59`);
    return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
  }

  private formatDateLabel(value: string): string {
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) return value;
    return parsed.toLocaleDateString('pt-BR');
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }
}

interface CatalogLedgerLineRow {
  movementId: number;
  origemMovimentacaoTipo: CatalogMovementOriginType;
  dataHoraMovimentacao: string;
  observacao: string;
  metricType: CatalogMovementMetricType | '';
  estoqueTipoNome: string;
  filialNome: string;
  beforeValue: number | null;
  delta: number | null;
  afterValue: number | null;
}
