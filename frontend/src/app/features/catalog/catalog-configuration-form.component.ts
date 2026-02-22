import { CommonModule } from '@angular/common';
import { Component, HostListener, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { Observable, of } from 'rxjs';
import { finalize, map, switchMap, tap } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { ConfigSectionShellComponent } from '../../shared/config-section-shell.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AgrupadorEmpresa } from '../configs/agrupador-empresa.service';
import { AgrupadorAfterSaveContext, AgrupadoresEmpresaComponent } from '../configs/agrupadores-empresa.component';
import {
  CatalogPriceRule,
  CatalogPriceRuleUpsert,
  CatalogPriceType,
  CatalogPricingService
} from './catalog-pricing.service';
import {
  CatalogConfiguration,
  CatalogConfigurationByGroup,
  CatalogConfigurationService,
  CatalogConfigurationType,
  CatalogNumberingMode,
  CatalogStockAdjustment,
  CatalogStockType
} from './catalog-configuration.service';
import {
  CatalogStockAdjustmentFormDialogComponent,
  CatalogStockAdjustmentFormDialogData
} from './catalog-stock-adjustment-form-dialog.component';
import { PriceBooksListComponent } from './price-books-list.component';
import { PriceVariantsListComponent } from './price-variants-list.component';

@Component({
  selector: 'app-catalog-configuration-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    ConfigSectionShellComponent,
    FieldSearchComponent,
    InlineLoaderComponent,
    AgrupadoresEmpresaComponent,
    PriceBooksListComponent,
    PriceVariantsListComponent
  ],
  templateUrl: './catalog-configuration-form.component.html',
  styleUrls: ['./catalog-configuration-form.component.css']
})
export class CatalogConfigurationFormComponent implements OnChanges {
  @Input({ required: true }) type: CatalogConfigurationType = 'PRODUCTS';
  @Input() initialTab: 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES' | null = null;
  activeTab: 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES' = 'GROUP_CONFIG';

  isMobile = false;
  mobileStockAdjustmentFiltersOpen = false;

  loading = false;
  groupsLoading = false;
  groupsError = '';
  error = '';
  config: CatalogConfiguration | null = null;
  groupRows: CatalogConfigurationByGroup[] = [];

  currentGroupId: number | null = null;
  currentGroupNome = '';
  currentGroupNumberingMode: CatalogNumberingMode = 'AUTOMATICA';
  currentGroupConfigSaving = false;
  groupPriceRules: CatalogPriceRule[] = [];
  groupPriceRulesLoading = false;
  groupPriceRulesSaving = false;
  groupPriceRulesError = '';
  readonly priceTypeOrder: CatalogPriceType[] = ['PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE'];
  readonly saveGroupCatalogSettings = (context: AgrupadorAfterSaveContext): Observable<unknown> =>
    this.saveGroupCatalogBundle(context);

  groupStockTypes: CatalogStockType[] = [];
  groupStockTypesLoading = false;
  groupStockTypesError = '';
  groupStockTypesSaving = false;
  selectedStockTypeId: number | null = null;
  stockTypeForm: {
    id: number | null;
    codigo: string;
    nome: string;
    ordem: number | null;
    active: boolean;
  } = {
    id: null,
    codigo: '',
    nome: '',
    ordem: null,
    active: true
  };

  stockAdjustmentCount = 0;
  stockAdjustmentCountLoading = false;
  stockAdjustmentCountError = '';
  stockAdjustmentRows: CatalogStockAdjustment[] = [];
  deletingStockAdjustmentId: number | null = null;
  stockAdjustmentSearchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Codigo' },
    { key: 'nome', label: 'Nome' }
  ];
  stockAdjustmentSearchFields = ['codigo', 'nome'];
  stockAdjustmentSearchTerm = '';
  stockAdjustmentStatusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';

  constructor(
    private service: CatalogConfigurationService,
    private pricingService: CatalogPricingService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {
    this.updateViewportMode();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['type']) {
      this.activeTab = this.resolveInitialTab();
      this.resetCurrentGroup();
      this.load();
      return;
    }
    if (changes['initialTab']) {
      this.activeTab = this.resolveInitialTab();
    }
  }

  setTab(tab: 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES'): void {
    this.activeTab = tab;
  }

  isTabActive(tab: 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES'): boolean {
    return this.activeTab === tab;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
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
          this.loadStockAdjustments();
        },
        error: err => {
          this.config = null;
          this.groupRows = [];
          this.stockAdjustmentRows = [];
          this.stockAdjustmentCount = 0;
          this.stockAdjustmentCountLoading = false;
          this.stockAdjustmentCountError = '';
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
    if (this.currentGroupId) {
      this.loadGroupStockTypes(this.currentGroupId, this.selectedStockTypeId);
    }
  }

  onGroupEditStarted(group: AgrupadorEmpresa): void {
    this.currentGroupId = group.id;
    this.currentGroupNome = group.nome;
    const existing = this.groupRows.find(item => item.agrupadorId === group.id);
    this.currentGroupNumberingMode = (existing?.numberingMode || 'AUTOMATICA') as CatalogNumberingMode;
    this.loadGroupPriceRules(group.id);
    this.loadGroupStockTypes(group.id);
  }

  loadGroupPriceRules(groupId?: number | null): void {
    const targetGroupId = Number(groupId || this.currentGroupId || 0);
    if (!targetGroupId) {
      this.groupPriceRules = [];
      this.groupPriceRulesError = '';
      return;
    }
    this.groupPriceRulesLoading = true;
    this.groupPriceRulesError = '';
    this.pricingService.listPriceRules(this.type, targetGroupId)
      .pipe(finalize(() => (this.groupPriceRulesLoading = false)))
      .subscribe({
        next: rows => this.groupPriceRules = this.sortRules(rows || []),
        error: err => {
          this.groupPriceRules = [];
          this.groupPriceRulesError = err?.error?.detail || 'Nao foi possivel carregar regras de preco.';
        }
      });
  }

  priceTypeLabel(value: CatalogPriceType): string {
    if (value === 'PURCHASE') return 'Compra';
    if (value === 'COST') return 'Custo';
    if (value === 'AVERAGE_COST') return 'Custo Medio';
    return 'Venda Base';
  }

  baseOptionsFor(rule: CatalogPriceRule): CatalogPriceType[] {
    return this.priceTypeOrder.filter(type => type !== rule.priceType);
  }

  onBaseModeChanged(rule: CatalogPriceRule): void {
    if (!rule) return;
    if (rule.baseMode === 'NONE') {
      rule.basePriceType = null;
      rule.adjustmentKindDefault = 'FIXED';
      rule.adjustmentDefault = 0;
      rule.uiLockMode = 'II';
      return;
    }
    if (!rule.basePriceType || rule.basePriceType === rule.priceType) {
      const option = this.baseOptionsFor(rule)[0];
      rule.basePriceType = option || null;
    }
  }

  adjustmentKindLabel(value: 'FIXED' | 'PERCENT'): string {
    return value === 'PERCENT' ? 'Percentual' : 'Fixo';
  }

  adjustmentValueLabel(kind: 'FIXED' | 'PERCENT'): string {
    return kind === 'PERCENT' ? 'Percentual padrao' : 'Valor fixo padrao';
  }

  adjustmentValueStep(kind: 'FIXED' | 'PERCENT'): string {
    return kind === 'PERCENT' ? '0.001' : '0.01';
  }

  uiLockModeLabel(value: 'I' | 'II' | 'III' | 'IV'): string {
    if (value === 'I') return 'Modo I - Valor bloqueado / ajuste livre';
    if (value === 'II') return 'Modo II - Valor livre / ajuste bloqueado';
    if (value === 'III') return 'Modo III - Ambos livres com sincronismo';
    return 'Modo IV - Ambos bloqueados com recalc automatico';
  }

  loadGroupStockTypes(groupId?: number | null, preferredId?: number | null): void {
    const targetGroupId = Number(groupId || this.currentGroupId || 0);
    if (!targetGroupId) {
      this.groupStockTypes = [];
      this.groupStockTypesError = '';
      this.selectedStockTypeId = null;
      this.resetStockTypeForm();
      return;
    }

    this.groupStockTypesLoading = true;
    this.groupStockTypesError = '';
    this.service.listStockTypesByGroup(this.type, targetGroupId)
      .pipe(finalize(() => (this.groupStockTypesLoading = false)))
      .subscribe({
        next: rows => {
          this.groupStockTypes = rows || [];
          if (!this.groupStockTypes.length) {
            this.startNewStockType();
            return;
          }
          const targetId = preferredId ?? this.selectedStockTypeId;
          const selected = this.groupStockTypes.find(item => item.id === targetId) || this.groupStockTypes[0];
          if (selected) {
            this.selectStockType(selected);
            return;
          }
          this.startNewStockType();
        },
        error: err => {
          this.groupStockTypes = [];
          this.groupStockTypesError = err?.error?.detail || 'Nao foi possivel carregar tipos de estoque.';
          this.selectedStockTypeId = null;
          this.resetStockTypeForm();
        }
      });
  }

  startNewStockType(): void {
    this.selectedStockTypeId = null;
    this.stockTypeForm = {
      id: null,
      codigo: '',
      nome: '',
      ordem: this.nextStockTypeOrder(),
      active: true
    };
  }

  selectStockType(row: CatalogStockType): void {
    this.selectedStockTypeId = row.id;
    this.stockTypeForm = {
      id: row.id,
      codigo: row.codigo,
      nome: row.nome,
      ordem: row.ordem,
      active: row.active
    };
  }

  canSaveStockTypeForm(): boolean {
    return !!this.currentGroupId
      && !this.groupStockTypesSaving
      && !!(this.stockTypeForm.codigo || '').trim()
      && !!(this.stockTypeForm.nome || '').trim();
  }

  saveStockTypeFicha(): void {
    if (!this.currentGroupId || !this.canSaveStockTypeForm()) return;

    const payload = {
      codigo: (this.stockTypeForm.codigo || '').trim(),
      nome: (this.stockTypeForm.nome || '').trim(),
      ordem: this.stockTypeForm.ordem,
      active: this.stockTypeForm.active
    };

    this.groupStockTypesSaving = true;
    const request$ = this.stockTypeForm.id
      ? this.service.updateStockTypeByGroup(this.type, this.currentGroupId, this.stockTypeForm.id, payload)
      : this.service.createStockTypeByGroup(this.type, this.currentGroupId, payload);

    request$
      .pipe(finalize(() => (this.groupStockTypesSaving = false)))
      .subscribe({
        next: saved => {
          this.notify.success(this.stockTypeForm.id ? 'Tipo de estoque atualizado.' : 'Tipo de estoque adicionado.');
          this.loadGroupStockTypes(this.currentGroupId, saved?.id ?? null);
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel salvar tipo de estoque.');
          this.loadGroupStockTypes(this.currentGroupId, this.stockTypeForm.id);
        }
      });
  }

  toggleMobileStockAdjustmentFilters(): void {
    this.mobileStockAdjustmentFiltersOpen = !this.mobileStockAdjustmentFiltersOpen;
  }

  activeStockAdjustmentFiltersCount(): number {
    let count = 0;
    if ((this.stockAdjustmentSearchTerm || '').trim()) count++;
    if (this.stockAdjustmentStatusFilter !== 'ALL') count++;
    return count;
  }

  onStockAdjustmentSearchChange(value: FieldSearchValue): void {
    this.stockAdjustmentSearchTerm = (value.term || '').trim();
    this.stockAdjustmentSearchFields = value.fields.length
      ? value.fields
      : this.stockAdjustmentSearchOptions.map(option => option.key);
  }

  onStockAdjustmentStatusChange(rawValue: string): void {
    this.stockAdjustmentStatusFilter = rawValue === 'ACTIVE' || rawValue === 'INACTIVE' ? rawValue : 'ALL';
  }

  filteredStockAdjustmentRows(): CatalogStockAdjustment[] {
    const term = (this.stockAdjustmentSearchTerm || '').trim().toLowerCase();
    let rows = [...(this.stockAdjustmentRows || [])];

    if (this.stockAdjustmentStatusFilter === 'ACTIVE') {
      rows = rows.filter(item => item.active);
    } else if (this.stockAdjustmentStatusFilter === 'INACTIVE') {
      rows = rows.filter(item => !item.active);
    }

    if (term) {
      rows = rows.filter(item => {
        const byCodigo = this.stockAdjustmentSearchFields.includes('codigo')
          && (item.codigo || '').toLowerCase().includes(term);
        const byNome = this.stockAdjustmentSearchFields.includes('nome')
          && (item.nome || '').toLowerCase().includes(term);
        return byCodigo || byNome;
      });
    }

    return rows;
  }

  openNewStockAdjustment(): void {
    this.openStockAdjustmentForm({ type: this.type, adjustment: null });
  }

  openEditStockAdjustment(row: CatalogStockAdjustment): void {
    this.openStockAdjustmentForm({ type: this.type, adjustment: row });
  }

  removeStockAdjustment(row: CatalogStockAdjustment): void {
    if (!row?.id || this.deletingStockAdjustmentId) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Remover tipo de ajuste',
        message: `Confirma remover o tipo de ajuste "${row.codigo} - ${row.nome}"?`,
        cancelText: 'Cancelar',
        confirmText: 'Remover',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.performRemoveStockAdjustment(row.id);
    });
  }

  stockAdjustmentTypeLabel(tipo: string): string {
    if (tipo === 'ENTRADA') return 'Entrada';
    if (tipo === 'SAIDA') return 'Saida';
    if (tipo === 'TRANSFERENCIA') return 'Transferencia';
    return tipo || '-';
  }

  typeLabel(): string {
    return this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos';
  }

  configReferenceName(): string {
    return `Catalogo ${this.typeLabel()}`;
  }

  private loadStockAdjustments(): void {
    this.stockAdjustmentCountLoading = true;
    this.stockAdjustmentCountError = '';
    this.stockAdjustmentRows = [];
    this.service.listStockAdjustmentsByType(this.type)
      .pipe(finalize(() => (this.stockAdjustmentCountLoading = false)))
      .subscribe({
        next: rows => {
          const sorted = [...(rows || [])].sort((a, b) => {
            const ao = Number(a?.ordem || 0);
            const bo = Number(b?.ordem || 0);
            if (ao !== bo) return ao - bo;
            return (a?.nome || '').localeCompare((b?.nome || ''), 'pt-BR');
          });
          this.stockAdjustmentRows = sorted;
          this.stockAdjustmentCount = sorted.length;
        },
        error: err => {
          this.stockAdjustmentRows = [];
          this.stockAdjustmentCount = 0;
          this.stockAdjustmentCountError = err?.error?.detail || 'Nao foi possivel carregar ajustes de estoque.';
        }
      });
  }

  private resetCurrentGroup(): void {
    this.currentGroupId = null;
    this.currentGroupNome = '';
    this.currentGroupNumberingMode = 'AUTOMATICA';
    this.groupPriceRules = [];
    this.groupPriceRulesLoading = false;
    this.groupPriceRulesSaving = false;
    this.groupPriceRulesError = '';
    this.groupStockTypes = [];
    this.groupStockTypesLoading = false;
    this.groupStockTypesError = '';
    this.groupStockTypesSaving = false;
    this.selectedStockTypeId = null;
    this.stockAdjustmentRows = [];
    this.stockAdjustmentCount = 0;
    this.stockAdjustmentCountLoading = false;
    this.stockAdjustmentCountError = '';
    this.deletingStockAdjustmentId = null;
    this.stockAdjustmentSearchTerm = '';
    this.stockAdjustmentSearchFields = this.stockAdjustmentSearchOptions.map(option => option.key);
    this.stockAdjustmentStatusFilter = 'ALL';
    this.mobileStockAdjustmentFiltersOpen = false;
    this.resetStockTypeForm();
  }

  private resetStockTypeForm(): void {
    this.stockTypeForm = {
      id: null,
      codigo: '',
      nome: '',
      ordem: null,
      active: true
    };
  }

  private nextStockTypeOrder(): number {
    if (!this.groupStockTypes.length) return 1;
    const max = Math.max(...this.groupStockTypes.map(item => Number(item.ordem || 0)));
    return (Number.isFinite(max) ? max : 0) + 1;
  }

  private openStockAdjustmentForm(data: CatalogStockAdjustmentFormDialogData): void {
    const ref = this.dialog.open(CatalogStockAdjustmentFormDialogComponent, {
      width: '920px',
      maxWidth: '95vw',
      maxHeight: 'calc(100dvh - 124px)',
      position: { top: '104px' },
      autoFocus: false,
      restoreFocus: true,
      panelClass: 'catalog-stock-adjustment-form-dialog-panel',
      data
    });
    ref.afterClosed().subscribe(saved => {
      if (!saved) return;
      this.loadStockAdjustments();
    });
  }

  private performRemoveStockAdjustment(id: number): void {
    this.deletingStockAdjustmentId = id;
    this.service.deleteStockAdjustmentByType(this.type, id)
      .pipe(finalize(() => (this.deletingStockAdjustmentId = null)))
      .subscribe({
        next: () => {
          this.notify.success('Tipo de ajuste removido.');
          this.loadStockAdjustments();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel remover tipo de ajuste.');
        }
      });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileStockAdjustmentFiltersOpen = false;
    }
  }

  private resolveInitialTab(): 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES' {
    if (this.initialTab === 'PRICE_BOOKS') return 'PRICE_BOOKS';
    if (this.initialTab === 'PRICE_VARIANTS') return 'PRICE_VARIANTS';
    if (this.initialTab === 'STOCK_ADJUSTMENT_TYPES') return 'STOCK_ADJUSTMENT_TYPES';
    return 'GROUP_CONFIG';
  }

  private saveGroupCatalogBundle(context: AgrupadorAfterSaveContext): Observable<unknown> {
    const groupId = Number(context?.groupId || 0);
    if (!groupId) {
      return of(null);
    }
    if (context?.isCreate) {
      return of(null);
    }

    const numberingMode = this.resolveGroupNumberingMode(groupId);
    const rulesPayload = this.resolveGroupRulesPayload(groupId);

    this.currentGroupConfigSaving = true;
    this.groupPriceRulesSaving = true;

    return this.service.updateByGroup(this.type, groupId, { numberingMode })
      .pipe(
        tap(updated => this.mergeGroupRow(updated)),
        switchMap(() =>
          rulesPayload.length > 0
            ? this.pricingService.upsertPriceRules(this.type, groupId, rulesPayload)
            : of([] as CatalogPriceRule[])
        ),
        tap(rows => {
          if (this.currentGroupId === groupId) {
            this.groupPriceRules = this.sortRules(rows || []);
          }
        }),
        map(() => null),
        finalize(() => {
          this.currentGroupConfigSaving = false;
          this.groupPriceRulesSaving = false;
        })
      );
  }

  private resolveGroupNumberingMode(groupId: number): CatalogNumberingMode {
    if (this.currentGroupId === groupId) {
      return this.currentGroupNumberingMode;
    }
    const row = this.groupRows.find(item => item.agrupadorId === groupId);
    return (row?.numberingMode || 'AUTOMATICA') as CatalogNumberingMode;
  }

  private resolveGroupRulesPayload(groupId: number): CatalogPriceRuleUpsert[] {
    if (this.currentGroupId !== groupId) {
      return [];
    }
    return this.toPriceRulePayload(this.groupPriceRules || []);
  }

  private toPriceRulePayload(rules: CatalogPriceRule[]): CatalogPriceRuleUpsert[] {
    return (rules || []).map(rule => ({
      priceType: rule.priceType,
      customName: (rule.customName || '').trim() || null,
      baseMode: rule.baseMode,
      basePriceType: rule.baseMode === 'BASE_PRICE' ? rule.basePriceType || null : null,
      adjustmentKindDefault: rule.baseMode === 'BASE_PRICE' ? rule.adjustmentKindDefault : 'FIXED',
      adjustmentDefault: rule.baseMode === 'BASE_PRICE' ? Number(rule.adjustmentDefault || 0) : 0,
      uiLockMode: rule.baseMode === 'NONE' ? 'II' : rule.uiLockMode,
      active: !!rule.active
    }));
  }

  private mergeGroupRow(updated: CatalogConfigurationByGroup): void {
    const index = this.groupRows.findIndex(item => item.agrupadorId === updated.agrupadorId);
    if (index >= 0) {
      this.groupRows[index] = updated;
      return;
    }
    this.groupRows = [...this.groupRows, updated];
  }

  private sortRules(rows: CatalogPriceRule[]): CatalogPriceRule[] {
    return [...(rows || [])]
      .sort((a, b) => this.priceTypeOrder.indexOf(a.priceType) - this.priceTypeOrder.indexOf(b.priceType));
  }
}
