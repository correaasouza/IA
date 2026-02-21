import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Inject, Input, OnDestroy, OnInit, Optional, Output, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { BehaviorSubject, EMPTY, Subject, combineLatest, merge } from 'rxjs';
import { catchError, distinctUntilChanged, exhaustMap, finalize, map, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { NotificationService } from '../../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../../shared/field-search/field-search.component';
import {
  CatalogGroupTreeNode,
  CatalogGroupTreeParams,
  CatalogItemSummary,
  CatalogSearchPage,
  CatalogSearchParams,
  MovimentoItemAllowedUnit,
  MovimentoTipoItemTemplate,
  MovementOperationService
} from '../movement-operation.service';

interface GroupNodeView {
  id: number;
  nome: string;
  parentId: number | null;
  nivel: number;
  path: string;
  breadcrumb: string | null;
  hasChildren: boolean;
  expanded: boolean;
  loadedChildren: boolean;
  loadingChildren: boolean;
  children: GroupNodeView[];
}

export interface MovimentoCatalogSelectorSelectedItemState {
  movementItemTypeId: number;
  catalogItemId: number;
  tenantUnitId?: string | null;
  unidade?: string | null;
  allowedUnits?: MovimentoItemAllowedUnit[];
  unitsLoading?: boolean;
  quantidade: number;
  valorUnitario: number | null;
  observacao: string | null;
  catalogType: 'PRODUCTS' | 'SERVICES';
  codigo: number;
  nome: string;
  descricao?: string | null;
}

export interface MovimentoCatalogSelectorDialogState {
  movementItemTypeId: number | null;
  q: string;
  searchFields?: string[] | null;
  status?: 'all' | 'ativo' | 'inativo' | null;
  groupId: number | null;
  groupPath: string | null;
  groupBreadcrumb: string | null;
  includeDescendants: boolean;
  ativo: boolean;
  selectedItems: MovimentoCatalogSelectorSelectedItemState[];
}

export interface MovimentoCatalogSelectorResultItem {
  movementItemTypeId: number;
  catalogItemId: number;
  tenantUnitId?: string | null;
  unidade?: string | null;
  quantidade: number;
  valorUnitario: number | null;
  observacao: string | null;
  catalogType: 'PRODUCTS' | 'SERVICES';
  codigo: number;
  nome: string;
  descricao?: string | null;
}

export interface MovimentoCatalogSelectorDialogResult {
  state: MovimentoCatalogSelectorDialogState;
  items: MovimentoCatalogSelectorResultItem[];
}

export interface MovimentoCatalogSelectorDialogData {
  movementType: string;
  movementConfigId: number;
  itemTypes: MovimentoTipoItemTemplate[];
  mode?: 'add' | 'edit';
  openSearchOnInit?: boolean;
  state?: MovimentoCatalogSelectorDialogState | null;
}

interface CatalogSearchFilters {
  movementItemTypeId: number;
  q: string;
  groupId: number | null;
  includeDescendants: boolean;
  ativo: boolean | null;
}

@Component({
  selector: 'app-movimento-catalog-selector-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule,
    FieldSearchComponent
  ],
  templateUrl: './movimento-catalog-selector-dialog.component.html',
  styleUrls: ['./movimento-catalog-selector-dialog.component.css']
})
export class MovimentoCatalogSelectorDialogComponent implements OnInit, OnDestroy {
  @Input() embedded = false;
  @Input() panelData: MovimentoCatalogSelectorDialogData | null = null;
  @Output() confirmSelection = new EventEmitter<MovimentoCatalogSelectorDialogResult>();
  @Output() closeSelection = new EventEmitter<void>();
  @Output() selectorStateChange = new EventEmitter<MovimentoCatalogSelectorDialogState>();
  @ViewChild(FieldSearchComponent) private fieldSearchComponent?: FieldSearchComponent;

  readonly movementItemTypeControl = new FormControl<number | null>(null, [Validators.required]);
  readonly searchControl = new FormControl<string>('', { nonNullable: true });
  readonly includeDescendantsControl = new FormControl<boolean>(true, { nonNullable: true });
  readonly statusControl = new FormControl<'all' | 'ativo' | 'inativo'>('ativo', { nonNullable: true });

  readonly pageSize = 30;
  readonly searchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Codigo' },
    { key: 'nome', label: 'Nome' },
    { key: 'descricao', label: 'Descricao' }
  ];
  searchFields: string[] = this.searchOptions.map(option => option.key);
  catalogSearchModalOpen = false;
  isMobileView = false;
  mobileSidebarOpen = false;

  groupNodes: GroupNodeView[] = [];
  loadingTree = false;
  treeErrorMessage = '';

  loading = false;
  loadingMore = false;
  errorMessage = '';
  totalElements = 0;
  totalPages = 0;
  pageNumber = -1;
  items: CatalogItemSummary[] = [];

  selectedGroupId: number | null = null;
  selectedGroupPath: string | null = null;
  selectedGroupBreadcrumb: string | null = null;

  private readonly selectedGroupId$ = new BehaviorSubject<number | null>(null);
  private readonly nextPage$ = new Subject<void>();
  private readonly reloadSearch$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  private readonly selectedItems = new Map<string, MovimentoCatalogSelectorSelectedItemState>();
  private currentMovementItemTypeId: number | null = null;
  private data!: MovimentoCatalogSelectorDialogData;

  constructor(
      @Optional() @Inject(MAT_DIALOG_DATA) private readonly dialogData: MovimentoCatalogSelectorDialogData | null,
      @Optional() private readonly dialogRef: MatDialogRef<MovimentoCatalogSelectorDialogComponent, MovimentoCatalogSelectorDialogResult> | null,
      private readonly movementOperationService: MovementOperationService,
      private readonly notificationService: NotificationService) {
  }

  ngOnInit(): void {
    this.updateViewportState();
    this.data = this.resolveData();
    this.applyInitialState(this.data.state ?? null);
    this.bindItemTypeWatcher();
    this.bindSearchPipeline();
    if (this.currentData().openSearchOnInit) {
      setTimeout(() => this.openCatalogSearchModal(), 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  itemTypes(): MovimentoTipoItemTemplate[] {
    return this.currentData().itemTypes || [];
  }

  selectedCount(): number {
    return this.selectedItems.size;
  }

  primaryActionLabel(): string {
    return this.currentData().mode === 'edit' ? 'Salvar item' : 'Novo item';
  }

  isEditMode(): boolean {
    return this.currentData().mode === 'edit';
  }

  saveItemLabel(): string {
    return 'Salvar item';
  }

  fieldSearchScope(): string {
    return this.searchFields.length === 1 ? this.searchFields[0]! : 'all';
  }

  selectedItemsList(): MovimentoCatalogSelectorSelectedItemState[] {
    return [...this.selectedItems.values()];
  }

  canLoadMore(): boolean {
    if (this.loading || this.loadingMore) {
      return false;
    }
    if (this.pageNumber < 0) {
      return false;
    }
    return this.pageNumber + 1 < this.totalPages;
  }

  onResultsScroll(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining <= 160 && this.canLoadMore()) {
      this.nextPage$.next();
    }
  }

  retrySearch(): void {
    this.reloadSearch$.next();
  }

  onFieldSearchChange(value: FieldSearchValue): void {
    this.searchFields = value?.fields?.length ? [...value.fields] : this.searchOptions.map(option => option.key);
    this.searchControl.setValue((value?.term || '').trim());
  }

  openCatalogSearchModal(): void {
    this.catalogSearchModalOpen = true;
    this.mobileSidebarOpen = !this.isMobileView;
    this.reloadSearch$.next();
  }

  closeCatalogSearchModal(): void {
    this.catalogSearchModalOpen = false;
    this.mobileSidebarOpen = false;
  }

  confirmCatalogSelectionFromModal(): void {
    if (this.selectedCount() <= 0) {
      this.notificationService.info('Selecione um item antes de continuar.');
      return;
    }
    this.closeCatalogSearchModal();
  }

  resetCatalogFilters(): void {
    this.searchFields = this.searchOptions.map(option => option.key);
    this.searchControl.setValue('');
    this.includeDescendantsControl.setValue(true);
    this.statusControl.setValue('ativo');
    this.clearGroupFilter();
    this.fieldSearchComponent?.clear();
    this.reloadSearch$.next();
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchControl.value || '').trim()) {
      count += 1;
    }
    if (this.selectedGroupId) {
      count += 1;
    }
    if (this.includeDescendantsControl.value === false) {
      count += 1;
    }
    if (this.statusControl.value !== 'ativo') {
      count += 1;
    }
    return count;
  }

  toggleMobileSidebar(): void {
    this.mobileSidebarOpen = !this.mobileSidebarOpen;
  }

  isGroupSelected(node: GroupNodeView): boolean {
    return this.selectedGroupId === node.id;
  }

  selectGroup(node: GroupNodeView): void {
    this.selectedGroupId = node.id;
    this.selectedGroupPath = node.path;
    this.selectedGroupBreadcrumb = node.breadcrumb || node.nome;
    this.selectedGroupId$.next(node.id);
  }

  clearGroupFilter(): void {
    this.selectedGroupId = null;
    this.selectedGroupPath = null;
    this.selectedGroupBreadcrumb = null;
    this.selectedGroupId$.next(null);
  }

  toggleGroup(node: GroupNodeView): void {
    if (!node.hasChildren) {
      this.selectGroup(node);
      return;
    }
    if (node.expanded) {
      node.expanded = false;
      return;
    }
    if (node.loadedChildren) {
      node.expanded = true;
      return;
    }
    this.loadGroupChildren(node);
  }

  isItemSelected(item: CatalogItemSummary): boolean {
    const movementItemTypeId = this.normalizePositive(this.movementItemTypeControl.value);
    if (!movementItemTypeId) {
      return false;
    }
    return this.selectedItems.has(this.selectionKey(movementItemTypeId, item.id));
  }

  toggleItem(item: CatalogItemSummary, checked: boolean): void {
    const movementItemTypeId = this.normalizePositive(this.movementItemTypeControl.value);
    if (!movementItemTypeId) {
      return;
    }
    const key = this.selectionKey(movementItemTypeId, item.id);
    if (!checked) {
      this.selectedItems.delete(key);
      return;
    }
    for (const existingKey of this.selectedItems.keys()) {
      if (existingKey !== key) {
        this.selectedItems.delete(existingKey);
      }
    }
    const itemType = this.itemTypes().find(current => current.tipoItemId === movementItemTypeId);
    const valorUnitario = itemType?.cobrar === false ? 0 : null;
    this.selectedItems.set(key, {
      movementItemTypeId,
      catalogItemId: item.id,
      tenantUnitId: null,
      unidade: item.unidade ?? null,
      allowedUnits: [],
      unitsLoading: true,
      quantidade: 1,
      valorUnitario,
      observacao: null,
      catalogType: item.catalogType,
      codigo: item.codigo,
      nome: item.nome,
      descricao: item.descricao ?? null
    });
    const selected = this.selectedItems.get(key);
    if (selected) {
      this.loadAllowedUnitsForSelected(selected, selected.unidade || null);
    }
  }

  selectAndConfirmItem(item: CatalogItemSummary): void {
    if (!this.normalizePositive(this.movementItemTypeControl.value)) {
      this.notificationService.info('Selecione o tipo de item antes de continuar.');
      return;
    }
    this.toggleItem(item, true);
    this.confirmCatalogSelectionFromModal();
  }


  updateSelectedQuantidade(selected: MovimentoCatalogSelectorSelectedItemState, value: unknown): void {
    selected.quantidade = this.toScaledNumber(value, 3, 0) ?? 0;
  }

  updateSelectedValorUnitario(selected: MovimentoCatalogSelectorSelectedItemState, value: unknown): void {
    const parsed = this.toScaledNumber(value, 2, null);
    selected.valorUnitario = parsed == null ? null : Math.max(0, parsed);
  }

  updateSelectedObservacao(selected: MovimentoCatalogSelectorSelectedItemState, value: string): void {
    const normalized = (value || '').trim();
    selected.observacao = normalized ? normalized : null;
  }

  updateSelectedUnit(selected: MovimentoCatalogSelectorSelectedItemState, tenantUnitId: string | null): void {
    const normalized = `${tenantUnitId || ''}`.trim();
    selected.tenantUnitId = normalized || null;
    const option = (selected.allowedUnits || []).find(item => item.tenantUnitId === selected.tenantUnitId);
    selected.unidade = option?.sigla || null;
  }

  removeSelected(selected: MovimentoCatalogSelectorSelectedItemState): void {
    const key = this.selectionKey(selected.movementItemTypeId, selected.catalogItemId);
    this.selectedItems.delete(key);
  }

  cancel(): void {
    const result: MovimentoCatalogSelectorDialogResult = {
      state: this.captureState(),
      items: []
    };
    if (this.dialogRef) {
      this.dialogRef.close(result);
    }
    if (this.embedded) {
      this.closeSelection.emit();
    }
    this.selectorStateChange.emit(result.state);
  }

  addSelected(): void {
    const items = [...this.selectedItems.values()];
    if (!items.length) {
      this.notificationService.info('Selecione ao menos um item para adicionar.');
      return;
    }
    for (const item of items) {
      if (!item.movementItemTypeId || item.movementItemTypeId <= 0) {
        this.notificationService.error('Selecione o tipo de item antes de adicionar.');
        return;
      }
      if (!item.catalogItemId || item.catalogItemId <= 0) {
        this.notificationService.error('Selecione itens validos para adicionar.');
        return;
      }
      if (!item.tenantUnitId) {
        this.notificationService.error(`Selecione a unidade do item ${item.codigo} - ${item.nome}.`);
        return;
      }
      if (!Number.isFinite(item.quantidade) || item.quantidade <= 0) {
        this.notificationService.error(`Quantidade invalida para o item ${item.codigo} - ${item.nome}.`);
        return;
      }
      if (item.valorUnitario != null && item.valorUnitario < 0) {
        this.notificationService.error(`Valor unitario invalido para o item ${item.codigo} - ${item.nome}.`);
        return;
      }
    }

    const resultItems: MovimentoCatalogSelectorResultItem[] = items.map(item => ({
      movementItemTypeId: item.movementItemTypeId,
      catalogItemId: item.catalogItemId,
      tenantUnitId: item.tenantUnitId ?? null,
      unidade: item.unidade ?? null,
      quantidade: this.toScaledNumber(item.quantidade, 3, 0) ?? 0,
      valorUnitario: this.toScaledNumber(item.valorUnitario, 2, null),
      observacao: item.observacao,
      catalogType: item.catalogType,
      codigo: item.codigo,
      nome: item.nome,
      descricao: item.descricao ?? null
    }));

    const result: MovimentoCatalogSelectorDialogResult = {
      state: this.captureState(),
      items: resultItems
    };
    if (this.dialogRef) {
      this.dialogRef.close(result);
    }
    this.confirmSelection.emit(result);
    this.selectorStateChange.emit(result.state);
    if (this.embedded) {
      this.selectedItems.clear();
    }
  }

  trackByGroupId(_: number, item: GroupNodeView): number {
    return item.id;
  }

  trackByCatalogItemId(_: number, item: CatalogItemSummary): number {
    return item.id;
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportState();
  }

  private bindItemTypeWatcher(): void {
    this.movementItemTypeControl.valueChanges
      .pipe(
        startWith(this.movementItemTypeControl.value),
        map(value => this.normalizePositive(value)),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe(movementItemTypeId => {
        const hasChangedType = this.currentMovementItemTypeId != null && this.currentMovementItemTypeId !== movementItemTypeId;
        this.currentMovementItemTypeId = movementItemTypeId;

        this.groupNodes = [];
        this.loadingTree = false;
        this.treeErrorMessage = '';

        if (hasChangedType) {
          this.clearGroupFilter();
          this.selectedItems.clear();
        }

        if (!movementItemTypeId) {
          return;
        }
        this.loadRootGroups(movementItemTypeId);
      });
  }

  private bindSearchPipeline(): void {
    const searchText$ = this.searchControl.valueChanges.pipe(
      startWith(this.searchControl.value),
      map(value => (value || '').trim()),
      distinctUntilChanged()
    );

    const filters$ = combineLatest([
      this.movementItemTypeControl.valueChanges.pipe(startWith(this.movementItemTypeControl.value), map(value => this.normalizePositive(value))),
      searchText$,
      this.includeDescendantsControl.valueChanges.pipe(startWith(this.includeDescendantsControl.value)),
      this.statusControl.valueChanges.pipe(startWith(this.statusControl.value)),
      this.selectedGroupId$.asObservable()
    ]).pipe(
      map(([movementItemTypeId, q, includeDescendants, status, groupId]) => ({
        movementItemTypeId,
        q,
        includeDescendants,
        ativo: this.resolveAtivoFromStatus(status),
        groupId
      })),
      map(filters => {
        if (!filters.movementItemTypeId) {
          return null;
        }
        return {
          movementItemTypeId: filters.movementItemTypeId,
          q: filters.q,
          includeDescendants: filters.includeDescendants,
          ativo: filters.ativo,
          groupId: filters.groupId
        } as CatalogSearchFilters;
      }),
      distinctUntilChanged((previous, current) => JSON.stringify(previous) === JSON.stringify(current))
    );

    filters$
      .pipe(
        switchMap(filters => {
          this.resetSearchState();
          if (!filters) {
            return EMPTY;
          }
          return merge(
            this.reloadSearch$.pipe(startWith(void 0), map(() => true)),
            this.nextPage$.pipe(map(() => false))
          ).pipe(
            exhaustMap(firstPage => this.fetchPage(filters, firstPage))
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe();
  }

  private fetchPage(filters: CatalogSearchFilters, firstPage: boolean) {
    const data = this.currentData();
    const nextPage = firstPage ? 0 : this.pageNumber + 1;
    if (!firstPage && (this.pageNumber < 0 || this.pageNumber + 1 >= this.totalPages)) {
      return EMPTY;
    }

    const params: CatalogSearchParams = {
      movementType: data.movementType,
      movementConfigId: data.movementConfigId,
      movementItemTypeId: filters.movementItemTypeId,
      q: filters.q,
      groupId: filters.groupId,
      includeDescendants: filters.includeDescendants,
      ativo: filters.ativo == null ? undefined : filters.ativo,
      page: nextPage,
      size: this.pageSize
    };

    if (firstPage) {
      this.loading = true;
      this.errorMessage = '';
    } else {
      this.loadingMore = true;
    }

    return this.movementOperationService.searchCatalogItems(params)
      .pipe(
        map(page => this.applyPage(page, firstPage)),
        catchError(err => {
          this.errorMessage = err?.error?.detail || 'Nao foi possivel buscar itens de catalogo.';
          if (firstPage) {
            this.items = [];
            this.totalElements = 0;
            this.totalPages = 0;
            this.pageNumber = -1;
          }
          return EMPTY;
        }),
        finalize(() => {
          if (firstPage) {
            this.loading = false;
          } else {
            this.loadingMore = false;
          }
        })
      );
  }

  private applyPage(page: CatalogSearchPage, firstPage: boolean): void {
    const content = page?.content || [];
    if (firstPage) {
      this.items = content.slice();
    } else {
      this.items = [...this.items, ...content];
    }
    this.totalElements = Number(page?.totalElements || 0);
    this.totalPages = Number(page?.totalPages || 0);
    this.pageNumber = Number(page?.number ?? 0);
  }

  private resetSearchState(): void {
    this.items = [];
    this.totalElements = 0;
    this.totalPages = 0;
    this.pageNumber = -1;
    this.errorMessage = '';
  }

  private loadRootGroups(movementItemTypeId: number): void {
    const data = this.currentData();
    this.loadingTree = true;
    this.treeErrorMessage = '';
    const params: CatalogGroupTreeParams = {
      movementType: data.movementType,
      movementConfigId: data.movementConfigId,
      movementItemTypeId
    };

    this.movementOperationService.loadCatalogGroupChildren(params)
      .pipe(
        finalize(() => (this.loadingTree = false)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: nodes => {
          this.groupNodes = (nodes || []).map(node => this.toGroupNodeView(node));
        },
        error: err => {
          this.groupNodes = [];
          this.treeErrorMessage = err?.error?.detail || 'Nao foi possivel carregar grupos de catalogo.';
        }
      });
  }

  private loadGroupChildren(parent: GroupNodeView): void {
    const data = this.currentData();
    const movementItemTypeId = this.normalizePositive(this.movementItemTypeControl.value);
    if (!movementItemTypeId) {
      return;
    }
    parent.loadingChildren = true;
    const params: CatalogGroupTreeParams = {
      movementType: data.movementType,
      movementConfigId: data.movementConfigId,
      movementItemTypeId,
      parentId: parent.id
    };

    this.movementOperationService.loadCatalogGroupChildren(params)
      .pipe(finalize(() => (parent.loadingChildren = false)))
      .subscribe({
        next: nodes => {
          parent.children = (nodes || []).map(node => this.toGroupNodeView(node));
          parent.loadedChildren = true;
          parent.expanded = true;
        },
        error: err => {
          this.notificationService.error(err?.error?.detail || 'Nao foi possivel carregar subgrupos.');
        }
      });
  }

  private toGroupNodeView(node: CatalogGroupTreeNode): GroupNodeView {
    return {
      id: node.id,
      nome: node.nome,
      parentId: node.parentId ?? null,
      nivel: node.nivel,
      path: node.path,
      breadcrumb: node.breadcrumb ?? null,
      hasChildren: node.hasChildren,
      expanded: false,
      loadedChildren: false,
      loadingChildren: false,
      children: []
    };
  }

  private applyInitialState(state: MovimentoCatalogSelectorDialogState | null): void {
    if (!state) {
      return;
    }
    this.movementItemTypeControl.setValue(this.normalizePositive(state.movementItemTypeId));
    this.searchControl.setValue(state.q || '');
    this.searchFields = state.searchFields?.length ? [...state.searchFields] : this.searchOptions.map(option => option.key);
    this.includeDescendantsControl.setValue(state.includeDescendants !== false);
    const status = state.status || (state.ativo === false ? 'inativo' : 'ativo');
    this.statusControl.setValue(status === 'all' ? 'all' : status === 'inativo' ? 'inativo' : 'ativo');

    this.selectedGroupId = this.normalizePositive(state.groupId);
    this.selectedGroupPath = state.groupPath || null;
    this.selectedGroupBreadcrumb = state.groupBreadcrumb || null;
    this.selectedGroupId$.next(this.selectedGroupId);

    for (const selected of state.selectedItems || []) {
      const movementItemTypeId = this.normalizePositive(selected.movementItemTypeId);
      const catalogItemId = this.normalizePositive(selected.catalogItemId);
      if (!movementItemTypeId || !catalogItemId) {
        continue;
      }
      this.selectedItems.set(this.selectionKey(movementItemTypeId, catalogItemId), {
        movementItemTypeId,
        catalogItemId,
        tenantUnitId: selected.tenantUnitId ?? null,
        unidade: selected.unidade ?? null,
        allowedUnits: [],
        unitsLoading: true,
        quantidade: Number(selected.quantidade || 0),
        valorUnitario: selected.valorUnitario == null ? null : Number(selected.valorUnitario),
        observacao: selected.observacao || null,
        catalogType: selected.catalogType,
        codigo: Number(selected.codigo || 0),
        nome: selected.nome || '',
        descricao: selected.descricao || null
      });
      const restored = this.selectedItems.get(this.selectionKey(movementItemTypeId, catalogItemId));
      if (restored) {
        this.loadAllowedUnitsForSelected(restored, selected.unidade || null);
      }
    }
  }

  private captureState(): MovimentoCatalogSelectorDialogState {
    return {
      movementItemTypeId: this.normalizePositive(this.movementItemTypeControl.value),
      q: (this.searchControl.value || '').trim(),
      searchFields: [...this.searchFields],
      status: this.statusControl.value,
      groupId: this.selectedGroupId,
      groupPath: this.selectedGroupPath,
      groupBreadcrumb: this.selectedGroupBreadcrumb,
      includeDescendants: this.includeDescendantsControl.value,
      ativo: this.resolveAtivoFromStatus(this.statusControl.value) !== false,
      selectedItems: [...this.selectedItems.values()].map(item => ({
        movementItemTypeId: item.movementItemTypeId,
        catalogItemId: item.catalogItemId,
        tenantUnitId: item.tenantUnitId ?? null,
        unidade: item.unidade ?? null,
        quantidade: item.quantidade,
        valorUnitario: item.valorUnitario,
        observacao: item.observacao,
        catalogType: item.catalogType,
        codigo: item.codigo,
        nome: item.nome,
        descricao: item.descricao ?? null
      }))
    };
  }

  private selectionKey(movementItemTypeId: number, catalogItemId: number): string {
    return `${movementItemTypeId}:${catalogItemId}`;
  }

  private normalizePositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return parsed > 0 ? parsed : null;
  }

  private toScaledNumber(value: unknown, scale: number, fallback: number | null): number | null {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return fallback;
    }
    const factor = Math.pow(10, scale);
    return Math.round(parsed * factor) / factor;
  }

  private resolveAtivoFromStatus(status: string | null | undefined): boolean | null {
    if (status === 'inativo') {
      return false;
    }
    if (status === 'all') {
      return null;
    }
    return true;
  }

  private resolveData(): MovimentoCatalogSelectorDialogData {
    const source = this.panelData || this.dialogData;
    if (!source) {
      throw new Error('movimento_catalog_selector_data_required');
    }
    return source;
  }

  private currentData(): MovimentoCatalogSelectorDialogData {
    return this.panelData || this.data;
  }

  private loadAllowedUnitsForSelected(
    selected: MovimentoCatalogSelectorSelectedItemState,
    preferredSigla: string | null = null
  ): void {
    if (!selected.catalogType || !selected.catalogItemId) {
      selected.allowedUnits = [];
      selected.unitsLoading = false;
      selected.tenantUnitId = null;
      selected.unidade = null;
      return;
    }
    selected.unitsLoading = true;
    this.movementOperationService.listAllowedUnits(selected.catalogType, selected.catalogItemId)
      .pipe(finalize(() => (selected.unitsLoading = false)))
      .subscribe({
        next: units => {
          const allowed = [...(units || [])];
          selected.allowedUnits = allowed;
          if (!allowed.length) {
            selected.tenantUnitId = null;
            selected.unidade = null;
            return;
          }

          const currentId = `${selected.tenantUnitId || ''}`.trim();
          const preferredById = currentId
            ? allowed.find(item => item.tenantUnitId === currentId)
            : null;
          const preferredBySigla = (preferredSigla || '').trim()
            ? allowed.find(item => (item.sigla || '').trim().toUpperCase() === (preferredSigla || '').trim().toUpperCase())
            : null;
          const fallback = preferredById || preferredBySigla || allowed[0] || null;
          selected.tenantUnitId = fallback?.tenantUnitId || null;
          selected.unidade = fallback?.sigla || null;
        },
        error: err => {
          selected.allowedUnits = [];
          selected.tenantUnitId = null;
          selected.unidade = null;
          this.notificationService.error(err?.error?.detail || 'Nao foi possivel carregar as unidades permitidas do item.');
        }
      });
  }

  private updateViewportState(): void {
    this.isMobileView = typeof window !== 'undefined' ? window.innerWidth < 1024 : false;
    if (!this.isMobileView) {
      this.mobileSidebarOpen = true;
    }
  }
}
