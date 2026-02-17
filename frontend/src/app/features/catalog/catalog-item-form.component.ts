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
  CatalogItemContext,
  CatalogItemPayload,
  CatalogItemService
} from './catalog-item.service';
import { CatalogGroupNode, CatalogGroupService } from './catalog-group.service';
import {
  CatalogMovementMetricType,
  CatalogMovementOriginType,
  CatalogMovement,
  CatalogStockBalanceRow,
  CatalogStockConsolidatedRow,
  CatalogStockService
} from './catalog-stock.service';
import { CatalogItemHistoryDialogComponent } from './catalog-item-history-dialog.component';

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
  activeTab: 'GERAL' | 'ESTOQUE' = 'GERAL';

  itemId: number | null = null;
  context: CatalogItemContext | null = null;
  contextWarning = '';

  groups: Array<{ id: number; nome: string }> = [];

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

  form = this.fb.group({
    codigo: [null as number | null],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', [Validators.maxLength(255)]],
    catalogGroupId: [null as number | null],
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
    private groupService: CatalogGroupService,
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
          next: created => {
            this.notify.success(`${this.titleLabel()} criado.`);
            this.router.navigate([`/catalog/${this.routeSegment()}/${created.id}`]);
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
        next: updated => {
          this.notify.success(`${this.titleLabel()} atualizado.`);
          this.router.navigate([`/catalog/${this.routeSegment()}/${updated.id}`]);
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

  ativoHeaderLabel(): string {
    return this.form.value.ativo ? 'Ativo' : 'Inativo';
  }

  cadastroTitle(): string {
    return `Cadastro de ${this.titlePlural}`;
  }

  subtitle(): string {
    if (this.mode === 'new') return `Novo ${this.titleSingular}`;
    if (this.mode === 'edit') return `Editar ${this.titleSingular}`;
    return `Consultar ${this.titleSingular}`;
  }

  codigoHint(): string {
    if (this.context?.numberingMode === 'AUTOMATICA') {
      return this.mode === 'new' ? 'Gerado automaticamente ao salvar.' : 'Codigo gerado automaticamente.';
    }
    return 'Codigo informado manualmente.';
  }

  private resolveMode(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');

    if (!idParam) {
      this.mode = 'new';
      this.itemId = null;
      this.codigoInfo = 'Gerado ao salvar';
      this.activeTab = 'GERAL';
      return;
    }

    this.itemId = Number(idParam);
    this.mode = isEdit ? 'edit' : 'view';
  }

  private loadContextAndData(): void {
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
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
            this.clearStockData();
            this.form.disable();
            return;
          }

          this.contextWarning = '';
          this.loadGroups();
          this.applyNumberingMode();

          if (this.itemId) {
            this.loadItem(this.itemId);
          } else {
            this.clearStockData();
            this.applyModeState();
          }
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
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
          this.applyModeState();
          this.refreshStockData();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar item do catalogo.');
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
      catalogGroupId: item.catalogGroupId || null,
      ativo: !!item.ativo
    });
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

  private loadGroups(): void {
    this.groupService.tree(this.type).subscribe({
      next: tree => {
        this.groups = this.flatten(tree || []);
      },
      error: () => {
        this.groups = [];
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

  private buildPayload(): CatalogItemPayload {
    const codigoValue = Number(this.form.value.codigo || 0);
    const codigo = this.context?.numberingMode === 'MANUAL' && Number.isFinite(codigoValue) && codigoValue > 0
      ? codigoValue
      : null;

    return {
      codigo,
      nome: (this.form.value.nome || '').trim(),
      descricao: (this.form.value.descricao || '').trim() || null,
      catalogGroupId: this.toPositive(this.form.value.catalogGroupId),
      ativo: !!this.form.value.ativo
    };
  }

  private toPositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private routeSegment(): string {
    return this.type === 'PRODUCTS' ? 'products' : 'services';
  }

  setTab(tab: 'GERAL' | 'ESTOQUE'): void {
    if (tab === 'ESTOQUE' && !this.canOpenStockTab()) return;
    this.activeTab = tab;
  }

  isTabActive(tab: 'GERAL' | 'ESTOQUE'): boolean {
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
}
