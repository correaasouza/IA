import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import {
  CatalogPricingService,
  PriceBook,
  PriceVariant,
  SalePriceApplyMode,
  SalePriceApplyByGroupResponse,
  SalePriceBulkItem,
  SalePriceGridRow
} from './catalog-pricing.service';
import { CatalogConfigurationType } from './catalog-configuration.service';
import { CatalogCrudType } from './catalog-item.service';
import { CatalogGroupNode, CatalogGroupService } from './catalog-group.service';
import { CatalogGroupsTreeComponent } from './catalog-groups-tree.component';

@Component({
  selector: 'app-sale-price-grid',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    FieldSearchComponent,
    CatalogGroupsTreeComponent,
    AccessControlDirective
  ],
  templateUrl: './sale-price-grid.component.html',
  styleUrls: ['./sale-price-grid.component.css']
})
export class SalePriceGridComponent implements OnInit, OnDestroy, AfterViewInit {
  books: PriceBook[] = [];
  variants: PriceVariant[] = [];
  rows: SalePriceGridRow[] = [];
  displayedColumns = [
    'catalogType',
    'catalogItemId',
    'catalogItemName',
    'catalogGroupName',
    'catalogBasePrice',
    'tenantUnitId',
    'priceFinal',
    'acoes'
  ];

  loading = false;
  applyingGroup = false;
  loadingMoreRows = false;
  isMobile = false;
  mobileFiltersOpen = false;
  quickMaintenanceOpen = true;
  workspaceHeightPx: number | null = null;

  totalElements = 0;
  pageIndex = 0;
  pageSize = 40;
  hasMoreRows = true;
  selectedGroupId: number | null = null;
  includeChildrenFilter = true;
  groupOptions: Array<{ id: number; nome: string }> = [];

  searchOptions: FieldSearchOption[] = [
    { key: 'itemId', label: 'ID item' },
    { key: 'name', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['itemId', 'name'];

  filters = this.fb.group({
    catalogType: ['PRODUCTS' as CatalogConfigurationType, Validators.required],
    priceBookId: [0, Validators.required],
    variantId: ['']
  });

  groupApplyForm = this.fb.group({
    adjustmentKind: ['PERCENT' as SalePriceApplyMode, Validators.required],
    adjustmentValue: [0, Validators.required],
    overwriteExisting: [true]
  });
  applyAdjustmentInputRaw = '0,000';

  @ViewChild('stickyShell') stickyShell?: ElementRef<HTMLElement>;
  @ViewChild('workspaceShell') workspaceShell?: ElementRef<HTMLElement>;
  @ViewChild('rowsPane') rowsPane?: ElementRef<HTMLElement>;

  private edits = new Map<string, number | null>();
  private rowInputRaw = new Map<string, string>();
  private readonly savingRowKeys = new Set<string>();
  private gridLoadSequence = 0;
  private groupLoadSequence = 0;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly pricingService: CatalogPricingService,
    private readonly groupService: CatalogGroupService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.loadReferences();
    this.loadGroupOptionsForSelectedType();

    this.syncApplyAdjustmentInputRaw(Number(this.groupApplyForm.value.adjustmentValue ?? 0));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.deferStickyLayoutRefresh();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
    this.refreshStickyLayout();
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (!this.isMobile) return;
    const scrollTop = window.scrollY || document.documentElement.scrollTop || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
    const fullHeight = document.documentElement.scrollHeight || 0;
    if (scrollTop + viewportHeight >= fullHeight - 180) {
      this.loadGrid(false);
    }
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
    this.deferStickyLayoutRefresh();
  }

  toggleQuickMaintenance(): void {
    this.quickMaintenanceOpen = !this.quickMaintenanceOpen;
    this.deferStickyLayoutRefresh();
  }

  showAllGroups(): void {
    if (!this.selectedGroupId) return;
    this.selectedGroupId = null;
    this.loadGrid(true);
  }

  onGroupSelected(groupId: number | null): void {
    this.selectedGroupId = groupId;
    this.loadGrid(true);
    if (this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }

  onIncludeChildrenFilterChange(checked: boolean): void {
    this.includeChildrenFilter = !!checked;
    this.loadGrid(true);
  }

  onGroupsChanged(): void {
    this.loadGroupOptionsForSelectedType();
    this.loadGrid(true);
  }

  onRowsScroll(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target) return;
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 120) {
      this.loadGrid(false);
    }
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(item => item.key);
    this.loadGrid(true);
  }

  onCatalogTypeUiChange(rawValue: string): void {
    const normalized = this.normalizeCatalogType(rawValue) || 'PRODUCTS';
    const current = this.selectedCatalogType();
    if (current !== normalized) {
      this.filters.patchValue({ catalogType: normalized }, { emitEvent: true });
      return;
    }
    this.onCatalogTypeChanged();
  }

  onBookOrVariantUiChange(): void {
    this.edits.clear();
    this.loadGrid(true);
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count += 1;
    if (`${this.filters.value.variantId || ''}`.trim()) count += 1;
    if (this.selectedGroupId) count += 1;
    return count;
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['itemId', 'name'];
    this.selectedGroupId = null;
    this.includeChildrenFilter = true;
    this.filters.patchValue({ catalogType: 'PRODUCTS', variantId: '' }, { emitEvent: false });
    this.loadGroupOptionsForSelectedType();
    this.edits.clear();
    this.rowInputRaw.clear();
    this.loadGrid(true);
  }

  loadGrid(reset = true): void {
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.gridLoadSequence++;
      this.rows = [];
      this.totalElements = 0;
      this.pageIndex = 0;
      this.hasMoreRows = false;
      return;
    }
    if (!reset && this.loadingMoreRows) return;
    if (!reset && !this.hasMoreRows) return;
    const loadSeq = ++this.gridLoadSequence;

    if (reset) {
      this.pageIndex = 0;
      this.hasMoreRows = true;
      this.rows = [];
      this.totalElements = 0;
    }

    const variantId = this.selectedVariantId();
    const catalogType = this.filters.value.catalogType as CatalogConfigurationType;
    const term = this.searchTerm.trim();
    const catalogItemId = this.searchFields.includes('itemId') ? this.parseNumber(term) : null;
    const text = this.searchFields.includes('name') ? term : '';
    const targetPage = reset ? 0 : this.pageIndex;

    this.loading = reset;
    this.loadingMoreRows = true;
    this.pricingService.gridSalePrices(
      bookId,
      variantId,
      catalogType,
      text || null,
      catalogItemId,
      this.selectedGroupId,
      this.includeChildrenFilter,
      targetPage,
      this.pageSize
    )
      .pipe(finalize(() => {
        if (loadSeq !== this.gridLoadSequence) return;
        this.loading = false;
        this.loadingMoreRows = false;
      }))
      .subscribe({
        next: page => {
          if (loadSeq !== this.gridLoadSequence) return;
          const incoming = this.normalizeGridRows(page?.content || []);
          this.rows = reset ? incoming : [...this.rows, ...incoming];
          if (reset) {
            this.edits.clear();
            this.rowInputRaw.clear();
          }
          const total = Number(page?.totalElements || 0);
          if (total > 0) {
            this.totalElements = total;
            this.hasMoreRows = this.rows.length < this.totalElements;
          } else {
            this.totalElements = this.rows.length + (incoming.length >= this.pageSize ? 1 : 0);
            this.hasMoreRows = incoming.length >= this.pageSize;
          }
          this.pageIndex = targetPage + 1;
          this.deferStickyLayoutRefresh();
          this.ensureRowsFillViewport();
        },
        error: err => {
          if (loadSeq !== this.gridLoadSequence) return;
          if (reset) {
            this.rows = [];
            this.totalElements = 0;
            this.pageIndex = 0;
          }
          this.hasMoreRows = false;
          this.edits.clear();
          this.deferStickyLayoutRefresh();
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a grade de precos.');
        }
      });
  }

  setEditedPrice(row: SalePriceGridRow, rawValue: string): void {
    const key = this.rowKey(row);
    const parsed = this.parseLocalizedDecimal(`${rawValue ?? ''}`);
    if (parsed == null) {
      if (row.priceFinal == null) {
        this.edits.delete(key);
      } else {
        this.edits.set(key, null);
      }
      return;
    }
    const value = this.roundToScale(parsed, 2);
    if (!Number.isFinite(value) || value < 0) {
      this.edits.delete(key);
      return;
    }
    const current = row.priceFinal == null ? null : Number(row.priceFinal);
    if (current != null && Math.abs(current - value) < 0.000001) {
      this.edits.delete(key);
      return;
    }
    this.edits.set(key, value);
  }

  hasPendingRowChange(row: SalePriceGridRow): boolean {
    return this.edits.has(this.rowKey(row));
  }

  isSavingRow(row: SalePriceGridRow): boolean {
    return this.savingRowKeys.has(this.rowKey(row));
  }

  saveRow(row: SalePriceGridRow): void {
    const key = this.rowKey(row);
    if (!this.edits.has(key) || this.savingRowKeys.has(key)) {
      return;
    }
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.notify.error('Selecione a tabela de preco.');
      return;
    }

    const variantId = this.selectedVariantId();
    const edited = this.edits.get(key);
    const item: SalePriceBulkItem = {
      catalogType: row.catalogType,
      catalogItemId: row.catalogItemId,
      tenantUnitId: row.tenantUnitId || null,
      priceFinal: edited == null ? null : Number(edited)
    };

    this.savingRowKeys.add(key);
    this.pricingService.bulkUpsertSalePrices(bookId, variantId, [item])
      .pipe(finalize(() => this.savingRowKeys.delete(key)))
      .subscribe({
        next: savedRows => {
          const saved = (savedRows || [])[0];
          if (saved) {
            row.id = saved.id ?? row.id ?? null;
            row.priceFinal = saved.priceFinal ?? edited ?? null;
          } else {
            row.id = null;
            row.priceFinal = null;
          }
          this.notify.success('Preco salvo.');
          this.edits.delete(key);
          this.rowInputRaw.delete(key);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar preco.')
      });
  }

  remove(row: SalePriceGridRow): void {
    if (!row.id) return;
    if (!confirm('Excluir este preco de venda?')) return;
    this.pricingService.deleteSalePrice(row.id).subscribe({
      next: () => {
        this.notify.success('Preco removido.');
        this.loadGrid(true);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover preco.')
    });
  }

  priceInputValue(row: SalePriceGridRow): number | null {
    const key = this.rowKey(row);
    if (this.edits.has(key)) {
      return this.edits.get(key) ?? null;
    }
    return row.priceFinal ?? null;
  }

  priceStatusLabel(row: SalePriceGridRow): string {
    return this.priceInputValue(row) == null ? '(sem preco definido)' : '';
  }

  priceInputDisplay(row: SalePriceGridRow): string {
    const key = this.rowKey(row);
    if (this.rowInputRaw.has(key)) {
      return this.rowInputRaw.get(key) || '';
    }
    const value = this.priceInputValue(row);
    return value == null ? '' : this.formatLocalized(value, 2);
  }

  onRowPriceInput(row: SalePriceGridRow, rawValue: string): void {
    const key = this.rowKey(row);
    this.rowInputRaw.set(key, rawValue ?? '');
    this.setEditedPrice(row, rawValue);
  }

  onRowPriceBlur(row: SalePriceGridRow): void {
    const key = this.rowKey(row);
    const raw = this.rowInputRaw.get(key) ?? '';
    const parsed = this.parseLocalizedDecimal(raw);
    if (parsed == null) {
      this.rowInputRaw.set(key, '');
      this.setEditedPrice(row, '');
      return;
    }
    const normalized = this.roundToScale(parsed, 2);
    this.rowInputRaw.set(key, this.formatLocalized(normalized, 2));
    this.setEditedPrice(row, `${normalized}`);
  }

  applyByGroup(): void {
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.notify.error('Selecione a tabela de preco.');
      return;
    }
    if (this.groupApplyForm.invalid) {
      this.groupApplyForm.markAllAsTouched();
      return;
    }
    const selectedType = this.selectedCatalogType();
    if (!selectedType) {
      this.notify.error('Selecione o tipo no filtro para aplicar por grupo.');
      return;
    }

    const variantId = this.selectedVariantId();
    const term = this.searchTerm.trim();
    const catalogItemId = this.searchFields.includes('itemId') ? this.parseNumber(term) : null;
    const text = this.searchFields.includes('name') ? term : '';
    const payload = {
      priceBookId: bookId,
      variantId,
      catalogType: selectedType,
      catalogGroupId: this.selectedGroupId || null,
      text: text || null,
      catalogItemId,
      adjustmentKind: this.selectedApplyMode(),
      adjustmentValue: this.normalizedAdjustmentValue(),
      includeChildren: this.includeChildrenFilter,
      overwriteExisting: this.groupApplyForm.value.overwriteExisting !== false
    };

    this.applyingGroup = true;
    this.pricingService.applySalePricesByGroup(payload)
      .pipe(finalize(() => (this.applyingGroup = false)))
      .subscribe({
        next: response => {
          this.notify.success(this.applySummary(response));
          this.loadGrid(true);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel aplicar valor na lista.')
      });
  }

  selectedCatalogType(): CatalogConfigurationType | null {
    const value = this.filters.value.catalogType as CatalogConfigurationType | null;
    if (value === 'PRODUCTS' || value === 'SERVICES') return value;
    return 'PRODUCTS';
  }

  selectedCatalogTypeLabel(): string {
    const value = this.selectedCatalogType();
    if (value === 'PRODUCTS') return 'Produtos';
    if (value === 'SERVICES') return 'Servicos';
    return 'Produtos';
  }

  groupTreeType(): CatalogCrudType {
    return (this.selectedCatalogType() || 'PRODUCTS') as CatalogCrudType;
  }

  selectedGroupLabel(): string {
    if (!this.selectedGroupId) return 'Todos';
    return this.groupOptions.find(item => item.id === this.selectedGroupId)?.nome || `Grupo #${this.selectedGroupId}`;
  }

  priceColumnLabel(): string {
    const bookId = Number(this.filters.value.priceBookId || 0);
    const bookName = this.books.find(item => item.id === bookId)?.name || 'Tabela';
    const variantId = this.selectedVariantId();
    const variantName = variantId == null
      ? 'Base'
      : (this.variants.find(item => item.id === variantId)?.name || `Variacao #${variantId}`);
    return `Preco ${bookName} (${variantName})`;
  }

  selectedApplyMode(): SalePriceApplyMode {
    return this.groupApplyForm.value.adjustmentKind === 'FIXED' ? 'FIXED' : 'PERCENT';
  }

  setApplyMode(mode: SalePriceApplyMode): void {
    this.groupApplyForm.patchValue({ adjustmentKind: mode }, { emitEvent: false });
    this.syncApplyAdjustmentInputRaw(Number(this.groupApplyForm.value.adjustmentValue ?? 0));
  }

  isPercentualApplyMode(): boolean {
    return this.selectedApplyMode() === 'PERCENT';
  }

  applyAdjustmentLabel(): string {
    return this.isPercentualApplyMode() ? 'Percentual sobre' : 'Valor sobre';
  }

  applyAdjustmentStep(): string {
    return this.isPercentualApplyMode() ? '0.001' : '0.01';
  }

  applyAdjustmentInputClass(): string {
    return this.isPercentualApplyMode() ? 'app-qty-input' : 'app-money-input';
  }

  onApplyAdjustmentInput(rawValue: string): void {
    this.applyAdjustmentInputRaw = rawValue ?? '';
    const parsed = this.parseLocalizedDecimal(this.applyAdjustmentInputRaw);
    if (parsed == null) {
      this.groupApplyForm.patchValue({ adjustmentValue: null }, { emitEvent: false });
      return;
    }
    this.groupApplyForm.patchValue({ adjustmentValue: this.roundToScale(parsed, this.applyAdjustmentScale()) }, { emitEvent: false });
  }

  onApplyAdjustmentBlur(): void {
    const parsed = this.parseLocalizedDecimal(this.applyAdjustmentInputRaw);
    const value = parsed == null ? 0 : parsed;
    const rounded = this.roundToScale(value, this.applyAdjustmentScale());
    this.groupApplyForm.patchValue({ adjustmentValue: rounded }, { emitEvent: false });
    this.syncApplyAdjustmentInputRaw(rounded);
  }

  private selectedVariantId(): number | null {
    const variantRaw = `${this.filters.value.variantId || ''}`.trim();
    return variantRaw ? Number(variantRaw) : null;
  }

  private applySummary(response: SalePriceApplyByGroupResponse): string {
    return `Aplicado na lista: ${response.processedItems} item(ns) processado(s), ${response.createdItems} criado(s), `
      + `${response.updatedItems} atualizado(s), ${response.skippedWithoutBasePrice} sem Venda Base, `
      + `${response.skippedExisting} existente(s) ignorado(s).`;
  }

  private onCatalogTypeChanged(): void {
    this.selectedGroupId = null;
    this.loadGroupOptionsForSelectedType();
    this.edits.clear();
    this.loadGrid(true);
  }

  private loadReferences(): void {
    this.pricingService.listBooks().subscribe({
      next: rows => {
        this.books = rows || [];
        const defaultBook = this.books.find(item => item.defaultBook) || this.books[0];
        this.filters.patchValue({ priceBookId: defaultBook?.id || 0 }, { emitEvent: false });
        if (defaultBook?.id) {
          this.loadGrid(true);
        } else {
          this.rows = [];
          this.totalElements = 0;
          this.pageIndex = 0;
          this.hasMoreRows = false;
          this.edits.clear();
        }
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel carregar tabelas de preco.')
    });

    this.pricingService.listVariants().subscribe({
      next: rows => {
        this.variants = rows || [];
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel carregar variacoes de preco.')
    });
  }

  private loadGroupOptionsForSelectedType(): void {
    const selectedType = this.selectedCatalogType();
    const loadSeq = ++this.groupLoadSequence;
    if (!selectedType) {
      this.groupOptions = [];
      return;
    }
    this.groupService.tree(this.groupTreeType()).subscribe({
      next: tree => {
        if (loadSeq !== this.groupLoadSequence) return;
        this.groupOptions = this.flatten(tree || []);
        if (this.selectedGroupId && !this.groupOptions.some(item => item.id === this.selectedGroupId)) {
          this.selectedGroupId = null;
        }
      },
      error: () => {
        if (loadSeq !== this.groupLoadSequence) return;
        this.groupOptions = [];
      }
    });
  }

  private flatten(
    nodes: CatalogGroupNode[],
    acc: Array<{ id: number; nome: string }> = [],
    prefix = ''
  ): Array<{ id: number; nome: string }> {
    for (const node of nodes || []) {
      const label = prefix ? `${prefix} / ${node.nome}` : node.nome;
      acc.push({ id: node.id, nome: label });
      this.flatten(node.children || [], acc, label);
    }
    return acc;
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
      return;
    }
    this.workspaceHeightPx = null;
  }

  private deferStickyLayoutRefresh(): void {
    setTimeout(() => this.refreshStickyLayout(), 0);
  }

  private refreshStickyLayout(): void {
    if (typeof window === 'undefined') return;
    if (this.isMobile) {
      this.workspaceHeightPx = null;
      return;
    }
    const workspaceEl = this.workspaceShell?.nativeElement;
    if (!workspaceEl) return;

    const workspaceRect = workspaceEl.getBoundingClientRect();
    const viewportBottom = window.innerHeight || document.documentElement.clientHeight || 0;
    const drawerContent = workspaceEl.closest('.mat-drawer-content') as HTMLElement | null;
    const drawerBottom = drawerContent?.getBoundingClientRect().bottom;
    const containerBottom = Number.isFinite(drawerBottom as number) ? Number(drawerBottom) : viewportBottom;
    const availableHeight = Math.max(300, Math.floor(containerBottom - workspaceRect.top - 11));
    this.workspaceHeightPx = availableHeight;
  }

  private ensureRowsFillViewport(): void {
    setTimeout(() => {
      if (this.loadingMoreRows || !this.hasMoreRows) return;
      const pane = this.rowsPane?.nativeElement;
      if (pane && pane.scrollHeight <= pane.clientHeight + 4) {
        this.loadGrid(false);
        return;
      }
      if (this.isMobile) {
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
        const fullHeight = document.documentElement.scrollHeight || 0;
        if (fullHeight <= viewportHeight + 120) {
          this.loadGrid(false);
        }
      }
    }, 0);
  }

  private parseNumber(value: unknown): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private rowKey(row: SalePriceGridRow): string {
    return `${row.catalogType}:${row.catalogItemId}:${row.tenantUnitId || ''}`;
  }

  private normalizedAdjustmentValue(): number {
    const raw = Number(this.groupApplyForm.value.adjustmentValue || 0);
    if (!Number.isFinite(raw)) return 0;
    return this.roundToScale(raw, this.applyAdjustmentScale());
  }

  private applyAdjustmentScale(): number {
    return this.isPercentualApplyMode() ? 3 : 2;
  }

  private syncApplyAdjustmentInputRaw(value: number): void {
    const safe = Number.isFinite(value) ? value : 0;
    this.applyAdjustmentInputRaw = this.formatLocalized(safe, this.applyAdjustmentScale());
  }

  private parseLocalizedDecimal(rawValue: string): number | null {
    const trimmed = `${rawValue ?? ''}`.trim();
    if (!trimmed) return null;
    const hasComma = trimmed.includes(',');
    let normalized = trimmed.replace(/\s/g, '');
    if (hasComma) {
      normalized = normalized.replace(/\./g, '').replace(',', '.');
    } else {
      normalized = normalized.replace(/,/g, '');
    }
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private roundToScale(value: number, scale: number): number {
    if (!Number.isFinite(value)) return 0;
    return Number(value.toFixed(scale));
  }

  private formatLocalized(value: number, scale: number): string {
    const safe = Number.isFinite(value) ? value : 0;
    return safe.toLocaleString('pt-BR', {
      minimumFractionDigits: scale,
      maximumFractionDigits: scale
    });
  }

  private normalizeCatalogType(rawValue: unknown): CatalogConfigurationType | null {
    const value = `${rawValue ?? ''}`.trim().toUpperCase();
    if (value.startsWith('PRODUCT')) return 'PRODUCTS';
    if (value.startsWith('SERVICE')) return 'SERVICES';
    return null;
  }

  private normalizeGridRows(incomingRaw: SalePriceGridRow[]): SalePriceGridRow[] {
    const selectedType = this.selectedCatalogType();
    const incomingNormalized = (incomingRaw || []).map(row => {
      const normalizedType = this.normalizeCatalogType((row as any)?.catalogType);
      return {
        ...row,
        catalogType: normalizedType || selectedType || 'PRODUCTS'
      } as SalePriceGridRow;
    });
    return selectedType
      ? incomingNormalized.filter(row => row.catalogType === selectedType)
      : incomingNormalized;
  }
}
