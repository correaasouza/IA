import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, Inject, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { finalize } from 'rxjs/operators';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toDisplayDate, toIsoDate } from '../../shared/date-utils';
import { CatalogCrudType } from './catalog-item.service';
import {
  CatalogPriceType,
  CatalogPriceHistoryEntry,
  CatalogPricingService,
  PriceHistorySourceType
} from './catalog-pricing.service';
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
    MatTableModule,
    DateMaskDirective,
    InlineLoaderComponent
  ],
  templateUrl: './catalog-item-history-dialog.component.html',
  styleUrls: ['./catalog-item-history-dialog.component.css']
})
export class CatalogItemHistoryDialogComponent implements OnInit {
  @ViewChild('historyScrollContainer') historyScrollContainer?: ElementRef<HTMLElement>;
  isMobile = false;
  mobileFiltersOpen = false;
  loading = false;
  error = '';

  stockRows: CatalogStockBalanceRow[] = [];
  stockConsolidatedRows: CatalogStockConsolidatedRow[] = [];
  ledgerEntries: CatalogMovement[] = [];
  ledgerDisplayEntries: CatalogMovement[] = [];
  ledgerDisplayLines: CatalogLedgerLineRow[] = [];
  ledgerPageSize = 20;
  ledgerTotalElements = 0;
  ledgerHasMore = true;
  ledgerLoadingMore = false;
  ledgerSortOrder: 'RECENT' | 'OLDEST' = 'RECENT';
  private ledgerNextPage = 0;
  private ledgerLoadRevision = 0;

  ledgerOrigins: Array<{ value: CatalogMovementOriginType; label: string }> = [
    { value: 'MUDANCA_GRUPO', label: 'Mudanca de grupo' },
    { value: 'MOVIMENTO_ESTOQUE', label: 'Movimento de estoque' },
    { value: 'WORKFLOW_ACTION', label: 'Workflow (legado)' },
    { value: 'SYSTEM', label: 'Sistema' }
  ];
  ledgerMetrics: Array<{ value: CatalogMovementMetricType; label: string }> = [
    { value: 'QUANTIDADE', label: 'Quantidade' },
    { value: 'PRECO', label: 'Preco' }
  ];

  ledgerFilters = this.fb.group({
    origemTipo: [''],
    origemCodigo: [''],
    origemId: [''],
    movimentoTipo: [''],
    usuario: [''],
    metricType: [''],
    estoqueTipoId: [''],
    filialId: [''],
    fromDate: [''],
    toDate: ['']
  });

  priceHistoryEntries: CatalogPriceHistoryEntry[] = [];
  priceHistoryLoading = false;
  priceHistoryError = '';
  priceHistoryTotalElements = 0;
  priceHistoryPageSize = 50;
  priceHistoryHasMore = true;
  priceHistoryLoadingMore = false;
  private priceHistoryNextPage = 0;
  private priceHistoryLoadRevision = 0;

  priceSourceOptions: Array<{ value: PriceHistorySourceType; label: string }> = [
    { value: 'SALE_PRICE', label: 'Tabela de preco' },
    { value: 'CATALOG_ITEM_PRICE', label: 'Preco base' }
  ];

  priceTypeOptions: Array<{ value: CatalogPriceType; label: string }> = [
    { value: 'PURCHASE', label: 'Compra' },
    { value: 'COST', label: 'Custo' },
    { value: 'AVERAGE_COST', label: 'Custo medio' },
    { value: 'SALE_BASE', label: 'Venda base' }
  ];

  priceFilters = this.fb.group({
    sourceType: [''],
    priceType: [''],
    priceBookId: [''],
    text: [''],
    fromDate: [''],
    toDate: ['']
  });

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: CatalogItemHistoryDialogData,
    private dialogRef: MatDialogRef<CatalogItemHistoryDialogComponent>,
    private fb: FormBuilder,
    private stockService: CatalogStockService,
    private pricingService: CatalogPricingService
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
    if (resetPage) {
      this.resetLedgerPagination();
      this.resetPriceHistoryPagination();
    }
    this.loadBalancesAndLedger();
  }

  applyLedgerFilters(): void {
    this.resetLedgerPagination();
    this.loadLedger(false);
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    const value = this.ledgerFilters.value;
    let count = 0;
    if ((value.origemTipo || '').trim()) count++;
    if ((value.origemCodigo || '').trim()) count++;
    if (`${value.origemId || ''}`.trim()) count++;
    if ((value.movimentoTipo || '').trim()) count++;
    if ((value.usuario || '').trim()) count++;
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
      origemCodigo: '',
      origemId: '',
      movimentoTipo: '',
      usuario: '',
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
      || (value.origemCodigo || '').trim()
      || `${value.origemId || ''}`.trim()
      || (value.movimentoTipo || '').trim()
      || (value.usuario || '').trim()
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

    const origemCodigo = (value.origemCodigo || '').trim();
    if (origemCodigo) {
      chips.push({ key: 'origemCodigo', label: `Cod. origem: ${origemCodigo}` });
    }

    const origemId = this.toPositive((value.origemId || '').trim());
    if (origemId) {
      chips.push({ key: 'origemId', label: `Id origem: ${origemId}` });
    }

    const movimentoTipo = (value.movimentoTipo || '').trim();
    if (movimentoTipo) {
      chips.push({ key: 'movimentoTipo', label: `Tipo movimento: ${movimentoTipo}` });
    }

    const usuario = (value.usuario || '').trim();
    if (usuario) {
      chips.push({ key: 'usuario', label: `Usuario: ${usuario}` });
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

  onDialogScroll(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 180) {
      if (this.ledgerHasMore && !this.ledgerLoadingMore && !this.loading) {
        this.loadLedger(true);
      }
      if (this.priceHistoryHasMore && !this.priceHistoryLoading && !this.priceHistoryLoadingMore) {
        this.loadPriceHistory(true);
      }
    }
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
    if (value === 'MOVIMENTO_ESTOQUE') return 'Movimento de estoque';
    if (value === 'WORKFLOW_ACTION') return 'Workflow (legado)';
    if (value === 'SYSTEM') return 'Sistema';
    return value || '-';
  }

  originSummary(row: CatalogLedgerLineRow): string {
    const baseLabel = this.originLabel(row.origemMovimentacaoTipo);
    const idLabel = row.origemMovimentacaoId || row.movementId;
    const codeLabel = (row.origemMovimentacaoCodigo || '').trim();
    const movementType = (row.movimentoTipo || '').trim();
    if (row.origemMovimentacaoTipo === 'MOVIMENTO_ESTOQUE') {
      const typeChunk = movementType ? ` (${movementType})` : '';
      const originChunk = codeLabel ? ` #${codeLabel}` : ` #${idLabel}`;
      return `${baseLabel}${typeChunk}${originChunk}`;
    }
    if (codeLabel) {
      return `${baseLabel} #${codeLabel}`;
    }
    return `${baseLabel} #${idLabel}`;
  }

  metricLabel(value: string): string {
    return value === 'PRECO' ? 'Preco' : 'Quantidade';
  }

  formatMetricValue(value: number | null, metricType?: CatalogMovementMetricType | '' | null): string {
    if (value === null || value === undefined) return '-';
    const decimalPlaces = metricType === 'PRECO' ? 2 : 3;
    return value.toLocaleString('pt-BR', {
      minimumFractionDigits: decimalPlaces,
      maximumFractionDigits: decimalPlaces
    });
  }

  metricValueClass(metricType?: CatalogMovementMetricType | '' | null): string {
    return metricType === 'PRECO' ? 'app-money-value' : 'app-qty-value';
  }

  priceActionLabel(value?: string | null): string {
    if (value === 'CREATE') return 'Criacao';
    if (value === 'UPDATE') return 'Atualizacao';
    if (value === 'DELETE') return 'Exclusao';
    return value || '-';
  }

  priceSourceLabel(value?: string | null): string {
    if (value === 'SALE_PRICE') return 'Tabela de preco';
    if (value === 'CATALOG_ITEM_PRICE') return 'Preco base';
    return value || '-';
  }

  priceTypeLabel(value?: string | null): string {
    if (value === 'PURCHASE') return 'Compra';
    if (value === 'COST') return 'Custo';
    if (value === 'AVERAGE_COST') return 'Custo medio';
    if (value === 'SALE_BASE') return 'Venda base';
    return value || '-';
  }

  applyPriceFilters(): void {
    this.resetPriceHistoryPagination();
    this.loadPriceHistory();
  }

  clearPriceFilters(): void {
    this.priceFilters.patchValue({
      sourceType: '',
      priceType: '',
      priceBookId: '',
      text: '',
      fromDate: '',
      toDate: ''
    });
    this.resetPriceHistoryPagination();
    this.loadPriceHistory();
  }

  hasPriceFiltersActive(): boolean {
    const value = this.priceFilters.value;
    return !!((value.sourceType || '').trim()
      || (value.priceType || '').trim()
      || `${value.priceBookId || ''}`.trim()
      || (value.text || '').trim()
      || (value.fromDate || '').trim()
      || (value.toDate || '').trim());
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
          this.resetPriceHistoryPagination();
          this.loadPriceHistory();
        },
        error: err => {
          this.stockRows = [];
          this.stockConsolidatedRows = [];
          this.ledgerEntries = [];
          this.ledgerDisplayEntries = [];
          this.ledgerDisplayLines = [];
          this.ledgerTotalElements = 0;
          this.ledgerHasMore = false;
          this.priceHistoryEntries = [];
          this.priceHistoryTotalElements = 0;
          this.priceHistoryHasMore = false;
          this.error = err?.error?.detail || 'Nao foi possivel carregar historico do item.';
        }
      });
  }

  private loadLedger(append = false): void {
    if (this.ledgerLoadingMore) {
      return;
    }
    if (append && !this.ledgerHasMore) {
      return;
    }
    const filters = this.ledgerFilters.value;
    const currentPage = append ? this.ledgerNextPage : 0;
    const currentRevision = this.ledgerLoadRevision;
    this.loading = !append;
    this.ledgerLoadingMore = true;
    this.error = '';
    this.stockService.getLedger(this.data.type, this.data.itemId, {
      page: currentPage,
      size: this.ledgerPageSize,
      origemTipo: (filters.origemTipo || '').trim() as CatalogMovementOriginType | '',
      origemCodigo: (filters.origemCodigo || '').trim(),
      origemId: this.toPositive((filters.origemId || '').trim()),
      movimentoTipo: (filters.movimentoTipo || '').trim(),
      usuario: (filters.usuario || '').trim(),
      metricType: (filters.metricType || '').trim() as CatalogMovementMetricType | '',
      estoqueTipoId: this.toPositive((filters.estoqueTipoId || '').trim()),
      filialId: this.toPositive((filters.filialId || '').trim()),
      fromDate: this.normalizeDateInput((filters.fromDate || '').trim()),
      toDate: this.normalizeDateInput((filters.toDate || '').trim()),
      tzOffsetMinutes: this.timezoneOffsetMinutes()
    }).pipe(finalize(() => {
      if (currentRevision !== this.ledgerLoadRevision) {
        return;
      }
      this.loading = false;
      this.ledgerLoadingMore = false;
    }))
      .subscribe({
        next: payload => {
          if (currentRevision !== this.ledgerLoadRevision) {
            return;
          }
          const incoming = this.extractContent(payload);
          this.ledgerEntries = append
            ? this.mergeLedgerEntries(this.ledgerEntries, incoming)
            : incoming;
          this.ledgerTotalElements = this.extractTotalElements(payload);
          this.ledgerNextPage = currentPage + 1;
          this.ledgerHasMore = this.computeHasMore(incoming.length);
          this.applyLedgerSort();
        },
        error: err => {
          if (currentRevision !== this.ledgerLoadRevision) {
            return;
          }
          this.ledgerEntries = [];
          this.ledgerDisplayEntries = [];
          this.ledgerDisplayLines = [];
          this.ledgerTotalElements = 0;
          this.ledgerHasMore = false;
          this.error = err?.error?.detail || 'Nao foi possivel carregar historico do item.';
        }
      });
  }

  private loadPriceHistory(append = false): void {
    if (this.priceHistoryLoadingMore) {
      return;
    }
    if (append && !this.priceHistoryHasMore) {
      return;
    }
    const filters = this.priceFilters.value;
    const currentPage = append ? this.priceHistoryNextPage : 0;
    const currentRevision = this.priceHistoryLoadRevision;
    this.priceHistoryLoading = !append;
    this.priceHistoryLoadingMore = true;
    this.priceHistoryError = '';
    this.pricingService.getPriceHistory(this.data.type, this.data.itemId, {
      sourceType: (filters.sourceType || '').trim() as PriceHistorySourceType | '',
      priceType: (filters.priceType || '').trim() as CatalogPriceType | '',
      priceBookId: this.toPositive((filters.priceBookId || '').trim()),
      text: this.normalizeText((filters.text || '').trim()),
      fromDate: this.normalizeDateInput((filters.fromDate || '').trim()),
      toDate: this.normalizeDateInput((filters.toDate || '').trim()),
      tzOffsetMinutes: this.timezoneOffsetMinutes(),
      page: currentPage,
      size: this.priceHistoryPageSize
    }).pipe(finalize(() => {
      if (currentRevision !== this.priceHistoryLoadRevision) {
        return;
      }
      this.priceHistoryLoading = false;
      this.priceHistoryLoadingMore = false;
    }))
      .subscribe({
        next: page => {
          if (currentRevision !== this.priceHistoryLoadRevision) {
            return;
          }
          const incoming = page?.content || [];
          this.priceHistoryEntries = append
            ? this.mergePriceHistoryEntries(this.priceHistoryEntries, incoming)
            : incoming;
          this.priceHistoryTotalElements = typeof page?.totalElements === 'number'
            ? page.totalElements
            : this.priceHistoryEntries.length;
          this.priceHistoryNextPage = currentPage + 1;
          this.priceHistoryHasMore = this.computePriceHistoryHasMore(incoming.length);
        },
        error: err => {
          if (currentRevision !== this.priceHistoryLoadRevision) {
            return;
          }
          this.priceHistoryEntries = [];
          this.priceHistoryTotalElements = 0;
          this.priceHistoryHasMore = false;
          this.priceHistoryError = err?.error?.detail || 'Nao foi possivel carregar historico de preco.';
        }
      });
  }

  private extractContent(payload: any): CatalogMovement[] {
    if (Array.isArray(payload?.content)) {
      return payload.content;
    }
    if (Array.isArray(payload?.page?.content)) {
      return payload.page.content;
    }
    if (Array.isArray(payload?.items)) {
      return payload.items;
    }
    return [];
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
    this.ensureLedgerFillViewport();
  }

  private syncLedgerDisplayLines(): void {
    const flattened: CatalogLedgerLineRow[] = [];
    const lineFilters = this.currentLedgerLineFilters();
    for (const movement of this.ledgerDisplayEntries || []) {
      if (!movement?.lines?.length) {
        if (lineFilters.hasActiveLineFilter) {
          continue;
        }
        flattened.push({
          movementId: movement.id,
          origemMovimentacaoTipo: movement.origemMovimentacaoTipo,
          origemMovimentacaoCodigo: movement.origemMovimentacaoCodigo || null,
          origemMovimentacaoId: movement.origemMovimentacaoId || null,
          movimentoTipo: movement.movimentoTipo || null,
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
        if (!this.matchesLedgerLineFilters(line, lineFilters)) {
          continue;
        }
        flattened.push({
          movementId: movement.id,
          origemMovimentacaoTipo: movement.origemMovimentacaoTipo,
          origemMovimentacaoCodigo: movement.origemMovimentacaoCodigo || null,
          origemMovimentacaoId: movement.origemMovimentacaoId || null,
          movimentoTipo: movement.movimentoTipo || null,
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

  private resetLedgerPagination(): void {
    this.ledgerLoadRevision += 1;
    this.ledgerNextPage = 0;
    this.ledgerTotalElements = 0;
    this.ledgerHasMore = true;
    this.ledgerEntries = [];
    this.ledgerDisplayEntries = [];
    this.ledgerDisplayLines = [];
    this.ledgerLoadingMore = false;
  }

  private resetPriceHistoryPagination(): void {
    this.priceHistoryLoadRevision += 1;
    this.priceHistoryNextPage = 0;
    this.priceHistoryTotalElements = 0;
    this.priceHistoryHasMore = true;
    this.priceHistoryEntries = [];
    this.priceHistoryLoadingMore = false;
  }

  private mergeLedgerEntries(existing: CatalogMovement[], incoming: CatalogMovement[]): CatalogMovement[] {
    const merged = [...(existing || [])];
    const seenIds = new Set<number>(
      (existing || [])
        .map(item => Number(item?.id || 0))
        .filter(id => id > 0));
    for (const item of incoming || []) {
      const id = Number(item?.id || 0);
      if (id > 0 && seenIds.has(id)) {
        continue;
      }
      if (id > 0) {
        seenIds.add(id);
      }
      merged.push(item);
    }
    return merged;
  }

  private mergePriceHistoryEntries(existing: CatalogPriceHistoryEntry[], incoming: CatalogPriceHistoryEntry[]): CatalogPriceHistoryEntry[] {
    const merged = [...(existing || [])];
    const seenIds = new Set<number>(
      (existing || [])
        .map(item => Number(item?.id || 0))
        .filter(id => id > 0));
    for (const item of incoming || []) {
      const id = Number(item?.id || 0);
      if (id > 0 && seenIds.has(id)) {
        continue;
      }
      if (id > 0) {
        seenIds.add(id);
      }
      merged.push(item);
    }
    return merged;
  }

  private computeHasMore(lastIncomingSize: number): boolean {
    if (this.ledgerTotalElements > 0) {
      return this.ledgerEntries.length < this.ledgerTotalElements;
    }
    return lastIncomingSize >= this.ledgerPageSize;
  }

  private computePriceHistoryHasMore(lastIncomingSize: number): boolean {
    if (this.priceHistoryTotalElements > 0) {
      return this.priceHistoryEntries.length < this.priceHistoryTotalElements;
    }
    return lastIncomingSize >= this.priceHistoryPageSize;
  }

  private currentLedgerLineFilters(): {
    metricType: CatalogMovementMetricType | '';
    estoqueTipoId: number | null;
    filialId: number | null;
    hasActiveLineFilter: boolean;
  } {
    const value = this.ledgerFilters.value;
    const metricType = (value.metricType || '').trim() as CatalogMovementMetricType | '';
    const estoqueTipoId = this.toPositive((value.estoqueTipoId || '').trim());
    const filialId = this.toPositive((value.filialId || '').trim());
    return {
      metricType,
      estoqueTipoId,
      filialId,
      hasActiveLineFilter: !!(metricType || estoqueTipoId || filialId)
    };
  }

  private matchesLedgerLineFilters(
    line: {
      metricType?: CatalogMovementMetricType | '';
      estoqueTipoId?: number | null;
      filialId?: number | null;
    },
    filters: {
      metricType: CatalogMovementMetricType | '';
      estoqueTipoId: number | null;
      filialId: number | null;
    }
  ): boolean {
    if (filters.metricType && line.metricType !== filters.metricType) {
      return false;
    }
    if (filters.estoqueTipoId && line.estoqueTipoId !== filters.estoqueTipoId) {
      return false;
    }
    if (filters.filialId && line.filialId !== filters.filialId) {
      return false;
    }
    return true;
  }

  private ensureLedgerFillViewport(): void {
    setTimeout(() => {
      const container = this.historyScrollContainer?.nativeElement;
      if (!container) {
        return;
      }
      if (container.scrollHeight <= container.clientHeight + 8) {
        if (!this.loading && !this.ledgerLoadingMore && this.ledgerHasMore) {
          this.loadLedger(true);
        }
        if (!this.priceHistoryLoading && !this.priceHistoryLoadingMore && this.priceHistoryHasMore) {
          this.loadPriceHistory(true);
        }
      }
    }, 0);
  }

  private toPositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private normalizeText(value: string): string | undefined {
    const normalized = (value || '').trim();
    return normalized || undefined;
  }

  private normalizeDateInput(value: string): string | undefined {
    const normalized = (value || '').trim();
    if (!normalized) {
      return undefined;
    }
    if (!isValidDateInput(normalized)) {
      return undefined;
    }
    return toIsoDate(normalized);
  }

  private timezoneOffsetMinutes(): number {
    return new Date().getTimezoneOffset();
  }

  private formatDateLabel(value: string): string {
    const normalized = (value || '').trim();
    if (!normalized || !isValidDateInput(normalized)) {
      return value;
    }
    return toDisplayDate(normalized);
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
  origemMovimentacaoCodigo?: string | null;
  origemMovimentacaoId?: number | null;
  movimentoTipo?: string | null;
  dataHoraMovimentacao: string;
  observacao: string;
  metricType: CatalogMovementMetricType | '';
  estoqueTipoNome: string;
  filialNome: string;
  beforeValue: number | null;
  delta: number | null;
  afterValue: number | null;
}
