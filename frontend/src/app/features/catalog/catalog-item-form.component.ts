import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import {
  CatalogCrudType,
  CatalogItem,
  CatalogItemPrice,
  CatalogItemContext,
  CatalogPriceEditedField,
  CatalogItemPricePayload,
  CatalogItemPayload,
  CatalogItemService
} from './catalog-item.service';
import { TenantUnit, UnitsService } from '../units/units.service';
import {
  CatalogMovementMetricType,
  CatalogMovementOriginType,
  CatalogMovement,
  CatalogStockBalanceRow,
  CatalogStockConsolidatedRow,
  CatalogStockService
} from './catalog-stock.service';
import { CatalogItemHistoryDialogComponent } from './catalog-item-history-dialog.component';
import { CatalogPriceRule, CatalogPricingService, SalePriceByItemRow } from './catalog-pricing.service';

@Component({
  selector: 'app-catalog-item-form',
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
  templateUrl: './catalog-item-form.component.html',
  styleUrls: ['./catalog-item-form.component.css']
})
export class CatalogItemFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  type: CatalogCrudType = 'PRODUCTS';
  titlePlural = 'Produtos';
  titleSingular = 'produto';
  activeTab: 'GERAL' | 'PRECOS' | 'ESTOQUE' = 'GERAL';

  itemId: number | null = null;
  context: CatalogItemContext | null = null;
  contextWarning = '';

  unitOptions: TenantUnit[] = [];
  catalogGroupIdLocked: number | null = null;
  catalogGroupNomeLocked = '-';
  unidadeAlternativaTenantUnitIdLocked: string | null = null;
  fatorConversaoAlternativaLocked: number | null = null;

  loading = false;
  saving = false;
  deleting = false;
  codigoInfo = 'Gerado ao salvar';
  stockLoading = false;
  ledgerLoading = false;
  stockError = '';
  stockRows: CatalogStockBalanceRow[] = [];
  stockConsolidatedRows: CatalogStockConsolidatedRow[] = [];
  selectedConsolidatedStockTypeId: number | null = null;
  stockDetailRows: CatalogStockBalanceRow[] = [];
  ledgerEntries: CatalogMovement[] = [];
  ledgerDisplayEntries: CatalogMovement[] = [];
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
  itemPrices: Array<CatalogItemPrice & { lastEditedField?: CatalogPriceEditedField }> = [];
  readonly priceTypeOrder: Array<CatalogItemPrice['priceType']> = ['PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE'];
  priceRulesLoading = false;
  priceRulesError = '';
  salePriceRowsLoading = false;
  salePriceRowsError = '';
  salePriceRows: SalePriceByItemRow[] = [];
  private pricePreviewRequestId = 0;
  private readonly priceRuleByType = new Map<CatalogItemPrice['priceType'], CatalogPriceRule>();

  form = this.fb.group({
    codigo: [null as number | null],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', [Validators.maxLength(255)]],
    tenantUnitId: ['', [Validators.required]],
    ativo: [true, Validators.required]
  });

  ledgerFilters = this.fb.group({
    origemTipo: [''],
    metricType: [''],
    estoqueTipoId: [''],
    filialId: [''],
    fromDate: [''],
    toDate: ['']
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private itemService: CatalogItemService,
    private pricingService: CatalogPricingService,
    private unitsService: UnitsService,
    private stockService: CatalogStockService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.type = (data['type'] || 'PRODUCTS') as CatalogCrudType;
      this.titlePlural = data['title'] || (this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos');
      this.titleSingular = data['singular'] || (this.type === 'PRODUCTS' ? 'produto' : 'servico');
      this.resolveMode();
      this.loadContextAndData();
    });
  }

  toEdit(): void {
    if (!this.itemId) return;
    this.router.navigate([`/catalog/${this.routeSegment()}/${this.itemId}/edit`]);
  }

  back(): void {
    this.router.navigate([`/catalog/${this.routeSegment()}`]);
  }

  save(): void {
    if (this.mode === 'view' || !this.context?.vinculado) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.buildPayload();
    this.saving = true;

    if (this.mode === 'new') {
      this.itemService.create(this.type, payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.notify.success(`${this.titleLabel()} criado.`);
            this.router.navigate([`/catalog/${this.routeSegment()}`]);
          },
          error: err => this.notify.error(err?.error?.detail || `Nao foi possivel criar ${this.titleLabelLower()}.`)
        });
      return;
    }

    if (!this.itemId) {
      this.saving = false;
      return;
    }

    this.itemService.update(this.type, this.itemId, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success(`${this.titleLabel()} atualizado.`);
          this.router.navigate([`/catalog/${this.routeSegment()}`]);
        },
        error: err => this.notify.error(err?.error?.detail || `Nao foi possivel atualizar ${this.titleLabelLower()}.`)
      });
  }

  remove(): void {
    if (!this.itemId || this.mode === 'new') return;
    if (!confirm(`Excluir ${this.titleLabelLower()} codigo ${this.codigoInfo}?`)) return;

    this.deleting = true;
    this.itemService.delete(this.type, this.itemId)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success(`${this.titleLabel()} excluido.`);
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || `Nao foi possivel excluir ${this.titleLabelLower()}.`)
      });
  }

  setAtivoFromHeader(nextValue: boolean): void {
    this.form.controls.ativo.setValue(!!nextValue);
  }

  cadastroTitle(): string {
    return `Cadastro de ${this.titlePlural}`;
  }

  subtitle(): string {
    if (this.mode === 'new') return `Novo ${this.titleSingular}`;
    if (this.mode === 'edit') return `Editar ${this.titleSingular}`;
    return `Consultar ${this.titleSingular}`;
  }

  private resolveMode(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');

    if (!idParam) {
      this.mode = 'new';
      this.itemId = null;
      this.codigoInfo = 'Gerado ao salvar';
      this.activeTab = 'GERAL';
      this.priceRulesError = '';
      this.catalogGroupIdLocked = null;
      this.catalogGroupNomeLocked = '-';
      this.unidadeAlternativaTenantUnitIdLocked = null;
      this.fatorConversaoAlternativaLocked = null;
      this.itemPrices = this.defaultPriceRows();
      this.clearSalePriceRows();
      return;
    }

    this.itemId = Number(idParam);
    this.mode = isEdit ? 'edit' : 'view';
  }

  private loadContextAndData(): void {
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      this.clearSalePriceRows();
      this.clearStockData();
      this.form.disable();
      return;
    }

    this.loading = true;
    this.itemService.contextoEmpresa(this.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: context => {
          this.context = context;
          if (!context.vinculado) {
            this.contextWarning = context.mensagem || 'Empresa sem grupo configurado para este catalogo.';
            this.clearSalePriceRows();
            this.clearStockData();
            this.form.disable();
            return;
          }

          this.contextWarning = '';
          this.loadPriceRules(context.agrupadorId, () => {
            this.loadUnitOptions(() => {
              this.applyNumberingMode();
              if (this.itemId) {
                this.loadItem(this.itemId);
              } else {
                this.itemPrices = this.defaultPriceRows();
                this.clearSalePriceRows();
                this.clearStockData();
                this.applyModeState();
              }
            });
          });
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
          this.clearSalePriceRows();
          this.clearStockData();
          this.form.disable();
        }
      });
  }

  private loadItem(id: number): void {
    this.loading = true;
    this.itemService.get(this.type, id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: item => {
          this.patchForm(item);
          this.loadSalePriceRows(item.id, item.tenantUnitId || null);
          this.applyModeState();
          this.refreshStockData();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar item do catalogo.');
          this.clearSalePriceRows();
          this.clearStockData();
          this.back();
        }
      });
  }

  private patchForm(item: CatalogItem): void {
    this.codigoInfo = String(item.codigo || '');
    this.form.patchValue({
      codigo: item.codigo,
      nome: item.nome,
      descricao: item.descricao || '',
      tenantUnitId: item.tenantUnitId || '',
      ativo: !!item.ativo
    });
    this.catalogGroupIdLocked = item.catalogGroupId || null;
    this.catalogGroupNomeLocked = item.catalogGroupNome || '-';
    this.unidadeAlternativaTenantUnitIdLocked = item.unidadeAlternativaTenantUnitId || null;
    this.fatorConversaoAlternativaLocked = item.fatorConversaoAlternativa ?? null;
    this.itemPrices = this.normalizePriceRows(item.prices || []);
    this.ensureUnitOption(item.tenantUnitId, item.tenantUnitSigla, item.tenantUnitNome);
  }

  private applyModeState(): void {
    if (!this.context?.vinculado) {
      this.form.disable();
      return;
    }

    if (this.mode === 'view') {
      this.form.disable();
      return;
    }

    this.form.enable();
    this.applyNumberingMode();
  }

  private applyNumberingMode(): void {
    const codigoControl = this.form.controls.codigo;
    if (this.context?.numberingMode === 'MANUAL') {
      codigoControl.addValidators([Validators.required, Validators.min(1)]);
      if (this.mode !== 'view') {
        codigoControl.enable({ emitEvent: false });
      }
    } else {
      codigoControl.clearValidators();
      if (this.mode === 'new') {
        codigoControl.setValue(null, { emitEvent: false });
      }
      codigoControl.disable({ emitEvent: false });
    }
    codigoControl.updateValueAndValidity({ emitEvent: false });
  }

  private buildPayload(): CatalogItemPayload {
    const codigoValue = Number(this.form.value.codigo || 0);
    const codigo = this.context?.numberingMode === 'MANUAL' && Number.isFinite(codigoValue) && codigoValue > 0
      ? codigoValue
      : null;
    const tenantUnitId = `${this.form.value.tenantUnitId || ''}`.trim();

    return {
      codigo,
      nome: (this.form.value.nome || '').trim(),
      descricao: (this.form.value.descricao || '').trim() || null,
      catalogGroupId: this.catalogGroupIdLocked,
      tenantUnitId,
      unidadeAlternativaTenantUnitId: this.unidadeAlternativaTenantUnitIdLocked,
      fatorConversaoAlternativa: this.fatorConversaoAlternativaLocked,
      prices: this.buildPriceInputsPayload(),
      ativo: !!this.form.value.ativo
    };
  }

  priceTypeLabel(value: CatalogItemPrice['priceType']): string {
    if (value === 'PURCHASE') return 'Compra';
    if (value === 'COST') return 'Custo';
    if (value === 'AVERAGE_COST') return 'Custo Medio';
    return 'Venda Base';
  }

  priceCardLabel(priceType: CatalogItemPrice['priceType']): string {
    const rule = this.ruleFor(priceType);
    const customName = (rule?.customName || '').trim();
    return customName || this.priceTypeLabel(priceType);
  }

  priceCalculationDescription(row: CatalogItemPrice): string {
    const rule = this.ruleFor(row.priceType);
    if (!rule || rule.baseMode !== 'BASE_PRICE' || !rule.basePriceType) {
      return '';
    }
    const target = this.priceCardLabel(row.priceType);
    const base = this.priceCardLabel(rule.basePriceType);
    if (row.adjustmentKind === 'PERCENT') {
      const percent = this.roundToScale(row.adjustmentValue, 3).toLocaleString('pt-BR', {
        minimumFractionDigits: 3,
        maximumFractionDigits: 3
      });
      return `Formula: ${target} = ${base} x (1 + ${percent}%). ${this.lockModeDescription(rule.uiLockMode)}`;
    }
    const fixed = this.moneyInputValue(row.adjustmentValue);
    return `Formula: ${target} = ${base} + R$ ${fixed}. ${this.lockModeDescription(rule.uiLockMode)}`;
  }

  showAdjustmentSection(row: CatalogItemPrice): boolean {
    return this.hasBaseRule(row.priceType);
  }

  canEditPrice(row: CatalogItemPrice): boolean {
    if (this.mode === 'view') return false;
    const rule = this.ruleFor(row.priceType);
    if (!rule) return true;
    return rule.uiLockMode === 'II' || rule.uiLockMode === 'III';
  }

  canEditAdjustment(row: CatalogItemPrice): boolean {
    if (this.mode === 'view') return false;
    if (!this.hasBaseRule(row.priceType)) return false;
    const rule = this.ruleFor(row.priceType);
    if (!rule) return true;
    return rule.uiLockMode === 'I' || rule.uiLockMode === 'III';
  }

  canEditAdjustmentKind(row: CatalogItemPrice): boolean {
    return this.canEditAdjustment(row);
  }

  onPriceFinalChanged(index: number, rawValue: string): void {
    const current = this.itemPrices[index];
    if (!current) return;
    if (!this.canEditPrice(current)) return;
    const parsed = this.parseLocalizedNumber(rawValue);
    current.priceFinal = parsed !== null && parsed >= 0 ? this.roundToScale(parsed, 2) : 0;
    current.lastEditedField = 'PRICE';
    this.requestPricePreview();
  }

  onAdjustmentChanged(index: number, rawValue: string): void {
    const current = this.itemPrices[index];
    if (!current) return;
    if (!this.canEditAdjustment(current)) return;
    const parsed = this.parseLocalizedNumber(rawValue);
    const scale = current.adjustmentKind === 'PERCENT' ? 3 : 2;
    current.adjustmentValue = parsed !== null ? this.roundToScale(parsed, scale) : 0;
    current.lastEditedField = 'ADJUSTMENT';
    this.requestPricePreview();
  }

  onAdjustmentKindChanged(index: number, rawValue: string): void {
    const current = this.itemPrices[index];
    if (!current) return;
    if (!this.canEditAdjustmentKind(current)) return;
    current.adjustmentKind = rawValue === 'PERCENT' ? 'PERCENT' : 'FIXED';
    const scale = current.adjustmentKind === 'PERCENT' ? 3 : 2;
    current.adjustmentValue = this.roundToScale(current.adjustmentValue, scale);
    current.lastEditedField = 'ADJUSTMENT';
    this.requestPricePreview();
  }

  moneyInputValue(value: number | null | undefined): string {
    const normalized = this.roundToScale(value, 2);
    return normalized.toLocaleString('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  adjustmentInputValue(row: CatalogItemPrice): string {
    const scale = row.adjustmentKind === 'PERCENT' ? 3 : 2;
    const normalized = this.roundToScale(row.adjustmentValue, scale);
    return normalized.toLocaleString('pt-BR', {
      minimumFractionDigits: scale,
      maximumFractionDigits: scale
    });
  }

  salePriceVariantLabel(row: SalePriceByItemRow): string {
    if (row.variantId == null) return 'Base';
    const name = (row.variantName || '').trim() || `Variacao #${row.variantId}`;
    return row.variantActive === false ? `${name} (inativa)` : name;
  }

  salePriceSourceLabel(source: SalePriceByItemRow['source']): string {
    if (source === 'EXACT_VARIANT') return 'Variação exata';
    if (source === 'BOOK_BASE') return 'Base da tabela';
    if (source === 'CATALOG_BASE') return 'Venda Base do item';
    if (source === 'INACTIVE_VARIANT_FALLBACK') return 'Variação inativa (base da tabela)';
    return 'Manual';
  }

  private loadUnitOptions(onLoaded: () => void): void {
    this.unitsService.listTenantUnits().subscribe({
      next: rows => {
        this.unitOptions = [...(rows || [])].sort((a, b) => {
          const aLabel = `${a.sigla || ''} ${a.nome || ''}`.trim();
          const bLabel = `${b.sigla || ''} ${b.nome || ''}`.trim();
          return aLabel.localeCompare(bLabel, 'pt-BR', { sensitivity: 'base' });
        });
        if (this.unitOptions.length === 0) {
          this.contextWarning = 'Cadastre ao menos uma unidade em Configuracoes > Unidades Medida para usar o catalogo.';
          this.form.disable();
          this.clearStockData();
          return;
        }
        if (this.mode === 'new' && !(this.form.value.tenantUnitId || '').trim()) {
          const defaultUnit = this.unitOptions.find(unit => !!unit.padrao) || this.unitOptions[0];
          if (defaultUnit?.id) {
            this.form.controls.tenantUnitId.setValue(defaultUnit.id, { emitEvent: false });
          }
        }
        onLoaded();
      },
      error: err => {
        this.unitOptions = [];
        this.contextWarning = err?.error?.detail || 'Nao foi possivel carregar unidades do locatario.';
        this.form.disable();
        this.clearStockData();
      }
    });
  }

  private ensureUnitOption(id: string | undefined | null, sigla?: string | null, nome?: string | null): void {
    const unitId = `${id || ''}`.trim();
    if (!unitId || this.unitOptions.some(unit => unit.id === unitId)) {
      return;
    }
    this.unitOptions = [
      ...this.unitOptions,
      {
        id: unitId,
        tenantId: 0,
        unidadeOficialId: '',
        unidadeOficialCodigo: '',
        unidadeOficialDescricao: '',
        unidadeOficialAtiva: true,
        sigla: (sigla || '').trim() || '-',
        nome: (nome || '').trim() || 'Unidade sem descricao',
        fatorParaOficial: 1,
        systemMirror: false,
        padrao: false
      }
    ];
  }

  private toPositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private routeSegment(): string {
    return this.type === 'PRODUCTS' ? 'products' : 'services';
  }

  private normalizePriceRows(rows: CatalogItemPrice[]): Array<CatalogItemPrice & { lastEditedField?: CatalogPriceEditedField }> {
    const byType = new Map<CatalogItemPrice['priceType'], CatalogItemPrice>();
    for (const item of rows || []) {
      byType.set(item.priceType, item);
    }
    return this.priceTypeOrder.map(type => {
      const row = byType.get(type);
      const rule = this.ruleFor(type);
      const hasBase = rule?.baseMode === 'BASE_PRICE';
      const adjustmentKind = hasBase
        ? (row?.adjustmentKind || rule?.adjustmentKindDefault || 'FIXED')
        : 'FIXED';
      const adjustmentScale = adjustmentKind === 'PERCENT' ? 3 : 2;
      const adjustmentValue = hasBase
        ? this.roundToScale(row?.adjustmentValue ?? rule?.adjustmentDefault ?? 0, adjustmentScale)
        : 0;
      return {
        priceType: type,
        priceFinal: this.roundToScale(row?.priceFinal, 2),
        adjustmentKind,
        adjustmentValue,
        lastEditedField: 'ADJUSTMENT'
      };
    });
  }

  private defaultPriceRows(): Array<CatalogItemPrice & { lastEditedField?: CatalogPriceEditedField }> {
    return this.normalizePriceRows([]);
  }

  setTab(tab: 'GERAL' | 'PRECOS' | 'ESTOQUE'): void {
    if (tab === 'ESTOQUE' && !this.canOpenStockTab()) return;
    this.activeTab = tab;
  }

  isTabActive(tab: 'GERAL' | 'PRECOS' | 'ESTOQUE'): boolean {
    return this.activeTab === tab;
  }

  canOpenStockTab(): boolean {
    return this.showStockSections();
  }

  stockTabDisabledLabel(): string {
    if (!this.context?.vinculado) return 'Aba Estoque disponivel apenas para empresa vinculada a um grupo.';
    if (!this.itemId) return 'Salve o cadastro para visualizar saldos e historico.';
    return '';
  }

  showStockSections(): boolean {
    return !!this.itemId && !!this.context?.vinculado;
  }

  refreshStockData(): void {
    if (!this.showStockSections() || !this.itemId) {
      this.clearStockData();
      return;
    }
    this.loadStockBalances(this.itemId);
  }

  openHistoryDialog(): void {
    if (!this.itemId) return;
    const parsedCodigo = Number(this.form.value.codigo || this.codigoInfo || 0);
    const itemCodigo = Number.isFinite(parsedCodigo) ? parsedCodigo : 0;
    const itemNome = (this.form.value.nome || '').trim() || '-';
    this.dialog.open(CatalogItemHistoryDialogComponent, {
      width: '1200px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        type: this.type,
        itemId: this.itemId,
        itemCodigo,
        itemNome
      }
    });
  }

  prevLedgerPage(): void {
    if (this.ledgerPageIndex <= 0 || !this.itemId) return;
    this.ledgerPageIndex--;
    this.loadLedger(this.itemId);
  }

  nextLedgerPage(): void {
    if (!this.itemId || !this.canNextLedgerPage()) return;
    this.ledgerPageIndex++;
    this.loadLedger(this.itemId);
  }

  canNextLedgerPage(): boolean {
    return (this.ledgerPageIndex + 1) * this.ledgerPageSize < this.ledgerTotalElements;
  }

  applyLedgerFilters(): void {
    if (!this.itemId) return;
    this.ledgerPageIndex = 0;
    this.loadLedger(this.itemId);
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

  setLedgerSortOrder(value: 'RECENT' | 'OLDEST'): void {
    this.ledgerSortOrder = value === 'OLDEST' ? 'OLDEST' : 'RECENT';
    this.applyLedgerSort();
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
      chips.push({
        key: 'estoqueTipoId',
        label: `Estoque: ${stockType?.label || '#' + estoqueTipoId}`
      });
    }

    const filialId = this.toPositive((value.filialId || '').trim());
    if (filialId) {
      const filial = this.ledgerFilialOptions().find(item => item.id === filialId);
      chips.push({
        key: 'filialId',
        label: `Filial: ${filial?.label || '#' + filialId}`
      });
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

  hasLedgerFiltersActive(): boolean {
    const value = this.ledgerFilters.value;
    return !!((value.origemTipo || '').trim()
      || (value.metricType || '').trim()
      || `${value.estoqueTipoId || ''}`.trim()
      || `${value.filialId || ''}`.trim()
      || (value.fromDate || '').trim()
      || (value.toDate || '').trim());
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

  ledgerRangeLabel(): string {
    if (this.ledgerTotalElements <= 0) return '0 de 0';
    const start = this.ledgerPageIndex * this.ledgerPageSize + 1;
    const end = Math.min((this.ledgerPageIndex + 1) * this.ledgerPageSize, this.ledgerTotalElements);
    return `${start}-${end} de ${this.ledgerTotalElements}`;
  }

  originLabel(value: string | undefined | null): string {
    if (value === 'MUDANCA_GRUPO') return 'Mudanca de grupo';
    if (value === 'SYSTEM') return 'Sistema';
    return value || '-';
  }

  metricLabel(value: string): string {
    return value === 'PRECO' ? 'Preco' : 'Quantidade';
  }

  selectConsolidatedStockType(stockTypeId: number | null | undefined): void {
    const normalized = Number(stockTypeId || 0);
    if (!Number.isFinite(normalized) || normalized <= 0) return;
    this.selectedConsolidatedStockTypeId = normalized;
    this.syncStockDetailRows();
  }

  isConsolidatedStockTypeSelected(stockTypeId: number | null | undefined): boolean {
    const normalized = Number(stockTypeId || 0);
    return !!normalized && normalized === this.selectedConsolidatedStockTypeId;
  }

  selectedConsolidatedStockTypeLabel(): string {
    const selectedId = this.selectedConsolidatedStockTypeId;
    if (!selectedId) return '-';
    const selected = this.stockConsolidatedRows.find(row => row.estoqueTipoId === selectedId);
    if (!selected) return `#${selectedId}`;
    return selected.estoqueTipoNome || selected.estoqueTipoCodigo || `#${selectedId}`;
  }

  private loadStockBalances(itemId: number): void {
    this.stockLoading = true;
    this.stockError = '';
    this.stockService.getBalances(this.type, itemId)
      .pipe(finalize(() => (this.stockLoading = false)))
      .subscribe({
        next: view => {
          this.stockRows = view?.rows || [];
          this.stockConsolidatedRows = view?.consolidado || [];
          this.syncSelectedConsolidatedStockType();
          this.syncStockDetailRows();
        },
        error: err => {
          this.stockRows = [];
          this.stockConsolidatedRows = [];
          this.selectedConsolidatedStockTypeId = null;
          this.stockDetailRows = [];
          this.stockError = err?.error?.detail || 'Nao foi possivel carregar os saldos de estoque.';
        }
      });
  }

  private loadLedger(itemId: number): void {
    const filters = this.ledgerFilters.value;
    this.ledgerLoading = true;
    this.stockService.getLedger(this.type, itemId, {
      page: this.ledgerPageIndex,
      size: this.ledgerPageSize,
      origemTipo: (filters.origemTipo || '').trim() as CatalogMovementOriginType | '',
      metricType: (filters.metricType || '').trim() as CatalogMovementMetricType | '',
      estoqueTipoId: this.toPositive((filters.estoqueTipoId || '').trim()),
      filialId: this.toPositive((filters.filialId || '').trim()),
      fromDate: this.toLedgerDateIsoStart((filters.fromDate || '').trim()),
      toDate: this.toLedgerDateIsoEnd((filters.toDate || '').trim())
    }).pipe(finalize(() => (this.ledgerLoading = false)))
      .subscribe({
        next: payload => {
          this.ledgerEntries = payload?.content || [];
          this.applyLedgerSort();
          this.ledgerTotalElements = this.extractTotalElements(payload);
        },
        error: err => {
          this.ledgerEntries = [];
          this.ledgerDisplayEntries = [];
          this.ledgerTotalElements = 0;
          this.stockError = err?.error?.detail || 'Nao foi possivel carregar o historico de movimentacoes.';
        }
      });
  }

  private clearStockData(): void {
    this.stockRows = [];
    this.stockConsolidatedRows = [];
    this.selectedConsolidatedStockTypeId = null;
    this.stockDetailRows = [];
    this.ledgerEntries = [];
    this.ledgerDisplayEntries = [];
    this.ledgerTotalElements = 0;
    this.ledgerPageIndex = 0;
    this.stockError = '';
    this.stockLoading = false;
    this.ledgerLoading = false;
    this.ensureActiveTab();
  }

  private syncSelectedConsolidatedStockType(): void {
    const selectedId = this.selectedConsolidatedStockTypeId;
    if (selectedId && this.stockConsolidatedRows.some(row => row.estoqueTipoId === selectedId)) {
      return;
    }
    const first = this.stockConsolidatedRows[0];
    this.selectedConsolidatedStockTypeId = first ? first.estoqueTipoId : null;
  }

  private syncStockDetailRows(): void {
    const selectedId = this.selectedConsolidatedStockTypeId;
    if (!selectedId) {
      this.stockDetailRows = [];
      return;
    }

    this.stockDetailRows = [...(this.stockRows || [])]
      .filter(row => row.estoqueTipoId === selectedId)
      .sort((a, b) => {
        const aName = (a.filialNome || '').trim();
        const bName = (b.filialNome || '').trim();
        const byName = aName.localeCompare(bName, 'pt-BR', { sensitivity: 'base' });
        if (byName !== 0) return byName;
        return Number(a.filialId || 0) - Number(b.filialId || 0);
      });
  }

  private extractTotalElements(payload: any): number {
    if (typeof payload?.totalElements === 'number') return payload.totalElements;
    if (typeof payload?.page?.totalElements === 'number') return payload.page.totalElements;
    return (payload?.content || []).length;
  }

  private applyLedgerSort(): void {
    const sorted = [...(this.ledgerEntries || [])].sort((a, b) => {
      const aTime = new Date(a?.dataHoraMovimentacao || 0).getTime();
      const bTime = new Date(b?.dataHoraMovimentacao || 0).getTime();
      if (aTime !== bTime) {
        return this.ledgerSortOrder === 'OLDEST' ? aTime - bTime : bTime - aTime;
      }
      const aid = Number(a?.id || 0);
      const bid = Number(b?.id || 0);
      return this.ledgerSortOrder === 'OLDEST' ? aid - bid : bid - aid;
    });
    this.ledgerDisplayEntries = sorted;
  }

  private formatDateLabel(value: string): string {
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) return value;
    return parsed.toLocaleDateString('pt-BR');
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

  private titleLabel(): string {
    return this.type === 'PRODUCTS' ? 'Produto' : 'Servico';
  }

  private titleLabelLower(): string {
    return this.type === 'PRODUCTS' ? 'produto' : 'servico';
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }

  private ensureActiveTab(): void {
    if (this.activeTab === 'ESTOQUE' && !this.canOpenStockTab()) {
      this.activeTab = 'GERAL';
    }
  }

  private loadSalePriceRows(catalogItemId: number, tenantUnitId?: string | null): void {
    const targetItemId = Number(catalogItemId || 0);
    if (!targetItemId) {
      this.clearSalePriceRows();
      return;
    }
    this.salePriceRowsLoading = true;
    this.salePriceRowsError = '';
    this.pricingService.listSalePricesByItem(this.type, targetItemId, tenantUnitId || null)
      .pipe(finalize(() => (this.salePriceRowsLoading = false)))
      .subscribe({
        next: rows => {
          this.salePriceRows = rows || [];
        },
        error: err => {
          this.salePriceRows = [];
          this.salePriceRowsError = err?.error?.detail || 'Nao foi possivel carregar precos de venda por tabela e variacao.';
        }
      });
  }

  private clearSalePriceRows(): void {
    this.salePriceRows = [];
    this.salePriceRowsLoading = false;
    this.salePriceRowsError = '';
  }

  private loadPriceRules(agrupadorId: number | null | undefined, onLoaded: () => void): void {
    const targetGroupId = Number(agrupadorId || 0);
    if (!targetGroupId) {
      this.priceRuleByType.clear();
      this.priceRulesError = '';
      this.priceRulesLoading = false;
      onLoaded();
      return;
    }

    this.priceRulesLoading = true;
    this.priceRulesError = '';
    this.pricingService.listPriceRules(this.type, targetGroupId)
      .pipe(finalize(() => (this.priceRulesLoading = false)))
      .subscribe({
        next: rows => {
          this.priceRuleByType.clear();
          for (const row of this.sortRules(rows || [])) {
            this.priceRuleByType.set(row.priceType, row);
          }
          this.itemPrices = this.normalizePriceRows(this.itemPrices || []);
          onLoaded();
        },
        error: err => {
          this.priceRuleByType.clear();
          this.priceRulesError = err?.error?.detail || 'Nao foi possivel carregar regras de preco do agrupador.';
          this.itemPrices = this.normalizePriceRows(this.itemPrices || []);
          onLoaded();
        }
      });
  }

  private sortRules(rows: CatalogPriceRule[]): CatalogPriceRule[] {
    return [...(rows || [])]
      .sort((a, b) => this.priceTypeOrder.indexOf(a.priceType) - this.priceTypeOrder.indexOf(b.priceType));
  }

  private ruleFor(priceType: CatalogItemPrice['priceType']): CatalogPriceRule | null {
    return this.priceRuleByType.get(priceType) || null;
  }

  private hasBaseRule(priceType: CatalogItemPrice['priceType']): boolean {
    const rule = this.ruleFor(priceType);
    return !!rule && rule.baseMode === 'BASE_PRICE';
  }

  private lockModeDescription(mode: CatalogPriceRule['uiLockMode']): string {
    if (mode === 'I') return 'Modo I: ajuste manual.';
    if (mode === 'II') return 'Modo II: ajuste bloqueado.';
    if (mode === 'III') return 'Modo III: sincronismo entre valor e ajuste.';
    return 'Modo IV: recalculo automatico.';
  }

  private parseLocalizedNumber(rawValue: string | number | null | undefined): number | null {
    if (rawValue === null || rawValue === undefined) return null;
    const raw = `${rawValue}`.trim();
    if (!raw) return null;
    const sanitized = raw.replace(/\s/g, '');
    const normalized = sanitized.includes(',')
      ? sanitized.replace(/\./g, '').replace(',', '.')
      : sanitized.replace(/,/g, '');
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private roundToScale(value: number | null | undefined, scale: number): number {
    const parsed = Number(value ?? 0);
    if (!Number.isFinite(parsed)) return 0;
    const factor = Math.pow(10, scale);
    return Math.round(parsed * factor) / factor;
  }

  private buildPriceInputsPayload(): CatalogItemPricePayload[] {
    return this.itemPrices.map(row => ({
      priceType: row.priceType,
      priceFinal: this.roundToScale(row.priceFinal, 2),
      adjustmentKind: this.hasBaseRule(row.priceType)
        ? row.adjustmentKind
        : 'FIXED',
      adjustmentValue: this.hasBaseRule(row.priceType)
        ? this.roundToScale(
          row.adjustmentValue,
          row.adjustmentKind === 'PERCENT' ? 3 : 2
        )
        : 0,
      lastEditedField: this.hasBaseRule(row.priceType)
        ? (row.lastEditedField || 'ADJUSTMENT')
        : 'PRICE'
    }));
  }

  private requestPricePreview(): void {
    if (this.mode === 'view' || !this.context?.vinculado) {
      return;
    }
    const payload = this.buildPriceInputsPayload();
    if (!payload.length) {
      return;
    }
    const currentRequestId = ++this.pricePreviewRequestId;
    this.itemService.previewPrices(this.type, {
      catalogItemId: this.itemId,
      prices: payload
    }).subscribe({
      next: rows => {
        if (currentRequestId !== this.pricePreviewRequestId) {
          return;
        }
        this.applyPricePreviewRows(rows || []);
      },
      error: () => {}
    });
  }

  private applyPricePreviewRows(rows: CatalogItemPrice[]): void {
    const lastEditedByType = new Map(this.itemPrices.map(row => [row.priceType, row.lastEditedField || 'ADJUSTMENT']));
    this.itemPrices = this.normalizePriceRows(rows).map(row => ({
      ...row,
      lastEditedField: lastEditedByType.get(row.priceType) || row.lastEditedField || 'ADJUSTMENT'
    }));
  }
}
