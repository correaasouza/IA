import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { FeatureFlagService } from '../../core/features/feature-flag.service';
import { EntityTypeService } from '../entity-types/entity-type.service';
import { WorkflowService } from '../workflows/workflow.service';
import { MovimentoItensListComponent } from './components/movimento-itens-list.component';
import {
  MovimentoCatalogSelectorDialogComponent,
  MovimentoCatalogSelectorDialogData,
  MovimentoCatalogSelectorDialogResult,
  MovimentoCatalogSelectorDialogState,
  MovimentoCatalogSelectorResultItem
} from './components/movimento-catalog-selector-dialog.component';
import {
  MovimentoEstoqueCreateRequest,
  MovimentoEstoqueItemResponse,
  MovimentoEstoqueItemRequest,
  MovimentoEstoqueResponse,
  MovimentoStockAdjustmentOption,
  MovimentoEstoqueTemplateResponse,
  MovimentoItemCatalogOption,
  MovimentoTipoItemTemplate,
  MovementOperationService
} from './movement-operation.service';

interface MovimentoEstoqueItemDraft {
  uid: number;
  codigo: number | null;
  movimentoItemTipoId: number | null;
  catalogType: 'PRODUCTS' | 'SERVICES' | null;
  catalogItemId: number | null;
  tenantUnitId: string | null;
  tenantUnitSigla: string | null;
  quantidade: number;
  valorUnitario: number;
  cobrar: boolean;
  estoqueMovimentado: boolean;
  finalizado: boolean;
  ordem: number;
  observacao: string;
  status?: string | null;
  catalogSearchText: string;
  catalogOptions: MovimentoItemCatalogOption[];
  editing: boolean;
  saved: boolean;
}

@Component({
  selector: 'app-movimento-estoque-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatDialogModule,
    MatSelectModule,
    InlineLoaderComponent,
    AccessControlDirective,
    MovimentoCatalogSelectorDialogComponent,
    MovimentoItensListComponent
  ],
  templateUrl: './movimento-estoque-form.component.html',
  styleUrls: ['./movimento-estoque-form.component.css']
})
export class MovimentoEstoqueFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo movimento de estoque';
  returnTo = '/movimentos/estoque';
  loading = false;
  saving = false;
  deleting = false;
  errorMessage = '';
  movimento: MovimentoEstoqueResponse | null = null;
  templateData: MovimentoEstoqueTemplateResponse | null = null;
  empresaNomeHeader = '-';
  isMobileView = false;
  selectedTabIndex = 0;
  itemRows: MovimentoEstoqueItemDraft[] = [];
  savedItemsView: MovimentoEstoqueItemResponse[] = [];
  stockAdjustmentsView: MovimentoStockAdjustmentOption[] = [];
  tiposEntidadePermitidosView: number[] = [];
  itemTypesPermitidosView: MovimentoTipoItemTemplate[] = [];
  tipoEntidadeNomeById = new Map<number, string>();
  itemTypeNameById = new Map<number, string>();
  workflowEnabled = true;
  itemTransitionsByItemId: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null; controlKey?: string | null }>> = {};
  itemStateNamesByItemId: Record<number, string> = {};
  itemStateKeysByItemId: Record<number, string> = {};
  itemStateColorsByStateKey: Record<string, string> = {};

  private nextRowUid = 1;
  private pendingEditItemUid: number | null = null;
  private pendingEditCatalogItemId: number | null = null;
  private editingItemRowUid: number | null = null;
  private openSearchOnSelectorOpen = false;
  private catalogSelectorState: MovimentoCatalogSelectorDialogState | null = null;
  private catalogSelectorOpen = false;
  private loadedItemColorConfigIds = new Set<number>();

  form = this.fb.group({
    empresaId: [null as number | null, Validators.required],
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    tipoEntidadeId: [null as number | null],
    stockAdjustmentId: [null as number | null],
    version: [null as number | null]
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService,
    private service: MovementOperationService,
    private entityTypeService: EntityTypeService,
    private workflowService: WorkflowService,
    private featureFlagService: FeatureFlagService
  ) {}

  ngOnInit(): void {
    this.updateViewportState();
    this.workflowEnabled = this.featureFlagService.isEnabled('workflowEnabled', true);
    const id = Number(this.route.snapshot.paramMap.get('id') || 0);
    const isEdit = this.route.snapshot.url.some(item => item.path === 'edit');
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    const editItemUid = Number(this.route.snapshot.queryParamMap.get('editItemUid') || 0);
    const editCatalogItemId = Number(this.route.snapshot.queryParamMap.get('editCatalogItemId') || 0);
    if (returnTo) {
      this.returnTo = returnTo;
    }
    this.pendingEditItemUid = editItemUid > 0 ? editItemUid : null;
    this.pendingEditCatalogItemId = editCatalogItemId > 0 ? editCatalogItemId : null;
    this.mode = id > 0 ? (isEdit ? 'edit' : 'view') : 'new';
    this.title = this.mode === 'new'
      ? 'Novo movimento de estoque'
      : this.mode === 'edit'
        ? 'Editar movimento de estoque'
        : 'Consultar movimento de estoque';

    if (this.mode === 'new') {
      this.loadTemplate();
      return;
    }
    this.loadMovimento(id);
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportState();
  }

  sectionLabel(index: number): string {
    if (index === 1) {
      return `Itens do movimento (${this.savedItemsView.length})`;
    }
    return 'Dados do movimento';
  }

  save(options?: { keepEditing?: boolean }): void {
    const keepEditing = options?.keepEditing === true;
    if (this.mode === 'view') {
      return;
    }
    if (this.mode !== 'new' && this.isMovimentoFinalizado()) {
      this.notify.error('Movimento finalizado. Nao e permitido alterar ou excluir.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const itens = this.buildItemRequests();
    if (itens == null) {
      return;
    }

    const empresaId = Number(this.form.value.empresaId || 0);
    const nome = (this.form.value.nome || '').trim();
    const tipoEntidadeId = this.normalizeTipoEntidadeId(this.form.value.tipoEntidadeId);
    const stockAdjustmentId = this.normalizeStockAdjustmentId(this.form.value.stockAdjustmentId);
    this.saving = true;

    if (this.mode === 'new') {
      const payload: MovimentoEstoqueCreateRequest = { empresaId, nome, tipoEntidadeId, stockAdjustmentId, itens };
      this.service.createEstoque(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: created => {
            this.notify.success('Movimento de estoque criado.');
            if (keepEditing) {
              this.router.navigate(['/movimentos/estoque', created.id, 'edit'], { queryParams: { returnTo: this.returnTo } });
              return;
            }
            this.router.navigateByUrl(this.returnTo);
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar o movimento de estoque.')
        });
      return;
    }

    if (!this.movimento?.id) {
      this.saving = false;
      return;
    }
    const version = Number(this.form.value.version ?? this.movimento.version);
    this.service.updateEstoque(this.movimento.id, { empresaId, nome, tipoEntidadeId, stockAdjustmentId, version, itens })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: updated => {
          this.notify.success('Movimento de estoque atualizado.');
          if (keepEditing) {
            this.loadMovimento(updated.id);
            return;
          }
          this.router.navigateByUrl(this.returnTo);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o movimento de estoque.')
      });
  }

  remove(): void {
    if (!this.movimento?.id) return;
    if (this.isMovimentoFinalizado()) {
      this.notify.error('Movimento com situacao finalizada nao pode ser alterado nem excluido.');
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir movimento',
        message: `Deseja excluir o movimento "${this.movimento.nome}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.deleting = true;
      this.service.deleteEstoque(this.movimento!.id)
        .pipe(finalize(() => (this.deleting = false)))
        .subscribe({
          next: () => {
            this.notify.success('Movimento excluido.');
            this.router.navigateByUrl(this.returnTo);
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o movimento.')
        });
    });
  }

  toEdit(): void {
    if (!this.movimento?.id) return;
    if (this.isMovimentoFinalizado()) {
      this.notify.error('Movimento com situacao finalizada nao pode ser alterado nem excluido.');
      return;
    }
    this.router.navigate(['/movimentos/estoque', this.movimento.id, 'edit'], { queryParams: { returnTo: this.returnTo } });
  }

  back(): void {
    this.router.navigateByUrl(this.returnTo);
  }

  nomeAtual(): string {
    return (this.form.get('nome')?.value || this.movimento?.nome || '-').trim() || '-';
  }

  codigoAtual(): string {
    const codigo = Number(this.movimento?.codigo || 0);
    return codigo > 0 ? String(codigo) : '-';
  }

  empresaAtual(): string {
    return (this.empresaNomeHeader || '').trim() || '-';
  }

  tipoEntidadeAtual(): string {
    const tipoEntidadeId = this.normalizeTipoEntidadeId(this.form.get('tipoEntidadeId')?.value);
    if (tipoEntidadeId == null) {
      return '-';
    }
    return this.tipoEntidadeNomeById.get(tipoEntidadeId) || `Tipo #${tipoEntidadeId}`;
  }

  stockAdjustmentAtual(): string {
    const id = this.normalizeStockAdjustmentId(this.form.get('stockAdjustmentId')?.value);
    if (id == null) {
      return '-';
    }
    const option = this.stockAdjustmentsView.find(item => item.id === id);
    if (!option) {
      return `Ajuste #${id}`;
    }
    return `${option.codigo} - ${option.nome} (${option.tipo})`;
  }

  stockAdjustmentOptionLabel(option: MovimentoStockAdjustmentOption): string {
    return `${option.codigo} - ${option.nome} (${option.tipo})`;
  }

  tiposEntidadePermitidos(): number[] {
    return this.tiposEntidadePermitidosView;
  }

  hasTiposEntidadeConfigurados(): boolean {
    return this.tiposEntidadePermitidos().length > 0;
  }

  tipoEntidadeOptionLabel(tipoEntidadeId: number): string {
    return this.tipoEntidadeNomeById.get(tipoEntidadeId) || `Tipo #${tipoEntidadeId}`;
  }

  itemTypesPermitidos(): MovimentoTipoItemTemplate[] {
    return this.itemTypesPermitidosView;
  }

  itemTypesPermitidosLabel(): string {
    const tipos = this.itemTypesPermitidos();
    if (!tipos.length) {
      return '-';
    }
    return tipos.map(item => item.nome).join(', ');
  }

  canRenderCatalogSelector(): boolean {
    if (this.mode === 'view') {
      return false;
    }
    if (this.isMovimentoFinalizado()) {
      return false;
    }
    if (!this.itemTypesPermitidosView.length) {
      return false;
    }
    return this.resolveMovementConfigId() > 0;
  }

  isCatalogSelectorOpen(): boolean {
    return this.catalogSelectorOpen;
  }

  openCatalogSelector(): void {
    if (!this.canRenderCatalogSelector()) {
      return;
    }
    this.catalogSelectorState = {
      movementItemTypeId: this.catalogSelectorState?.movementItemTypeId ?? null,
      q: this.catalogSelectorState?.q || '',
      searchFields: this.catalogSelectorState?.searchFields || ['codigo', 'nome', 'descricao'],
      status: this.catalogSelectorState?.status || (this.catalogSelectorState?.ativo === false ? 'inativo' : 'ativo'),
      groupId: this.catalogSelectorState?.groupId ?? null,
      groupPath: this.catalogSelectorState?.groupPath ?? null,
      groupBreadcrumb: this.catalogSelectorState?.groupBreadcrumb ?? null,
      includeDescendants: this.catalogSelectorState?.includeDescendants !== false,
      ativo: this.catalogSelectorState?.ativo !== false,
      selectedItems: []
    };
    this.editingItemRowUid = null;
    this.openSearchOnSelectorOpen = true;
    this.selectedTabIndex = 1;
    this.reopenCatalogSelector();
  }

  closeCatalogSelector(): void {
    this.catalogSelectorOpen = false;
    this.editingItemRowUid = null;
    this.openSearchOnSelectorOpen = false;
  }

  catalogSelectorData(): MovimentoCatalogSelectorDialogData | null {
    if (!this.canRenderCatalogSelector()) {
      return null;
    }
    return {
      movementType: 'MOVIMENTO_ESTOQUE',
      movementConfigId: this.resolveMovementConfigId(),
      itemTypes: this.itemTypesPermitidosView,
      mode: this.editingItemRowUid ? 'edit' : 'add',
      openSearchOnInit: this.openSearchOnSelectorOpen,
      state: this.catalogSelectorState
    };
  }

  onCatalogSelectorAdd(result: MovimentoCatalogSelectorDialogResult): void {
    if (!result) {
      return;
    }
    this.catalogSelectorState = result.state
      ? { ...result.state, selectedItems: [] }
      : this.catalogSelectorState;
    const selectedItems = result.items || [];
    if (!selectedItems.length) {
      return;
    }
    const selectedItem = selectedItems[0];
    if (!selectedItem) {
      return;
    }
    if (this.applyCatalogSelectionToEditingRow(selectedItem)) {
      this.closeCatalogSelector();
      this.persistMovementAfterItemSave();
      return;
    }
    const addedCount = this.appendSelectedCatalogItemsLocally([selectedItem], false);
    if (addedCount <= 0) {
      return;
    }
    this.closeCatalogSelector();
    this.persistMovementAfterItemSave();
  }

  onCatalogSelectorStateChange(state: MovimentoCatalogSelectorDialogState): void {
    this.catalogSelectorState = state;
  }

  private resolveMovementConfigId(): number {
    const fromTemplate = Number(this.templateData?.movimentoConfigId || 0);
    if (fromTemplate > 0) {
      return fromTemplate;
    }
    const fromMovimento = Number(this.movimento?.movimentoConfigId || 0);
    return fromMovimento > 0 ? fromMovimento : 0;
  }

  private appendSelectedCatalogItemsLocally(items: MovimentoCatalogSelectorResultItem[], showSuccessMessage = true): number {
    const rowsToAppend: MovimentoEstoqueItemDraft[] = [];
    for (const item of items) {
      const movementItemTypeId = Number(item.movementItemTypeId || 0);
      const catalogItemId = Number(item.catalogItemId || 0);
      const quantidade = this.toScaledNumber(item.quantidade, 3, 0) ?? 0;
      if (movementItemTypeId <= 0 || catalogItemId <= 0 || quantidade <= 0) {
        continue;
      }
      const itemType = this.itemTypesPermitidosView.find(current => current.tipoItemId === movementItemTypeId);
      const cobrar = itemType?.cobrar ?? true;
      const valorUnitario = cobrar ? (this.toScaledNumber(item.valorUnitario, 2, 0) ?? 0) : 0;
      rowsToAppend.push({
        uid: this.nextRowUid++,
        codigo: null,
        movimentoItemTipoId: movementItemTypeId,
        catalogType: item.catalogType,
        catalogItemId,
        tenantUnitId: item.tenantUnitId || null,
        tenantUnitSigla: item.unidade || null,
        quantidade,
        valorUnitario,
        cobrar,
        estoqueMovimentado: false,
        finalizado: false,
        ordem: this.itemRows.length + rowsToAppend.length,
        observacao: item.observacao || '',
        status: null,
        catalogSearchText: `${item.codigo} - ${item.nome}`,
        catalogOptions: [{
          id: catalogItemId,
          catalogType: item.catalogType,
          codigo: item.codigo,
          nome: item.nome,
          descricao: null
        }],
        editing: false,
        saved: true
      });
    }

    if (!rowsToAppend.length) {
      this.notify.info('Nenhum item valido foi selecionado para adicionar.');
      return 0;
    }
    this.itemRows = [...this.itemRows, ...rowsToAppend];
    this.reindexRows();
    if (showSuccessMessage) {
      this.notify.success(`${rowsToAppend.length} item(ns) adicionado(s).`);
    }
    return rowsToAppend.length;
  }

  private applyCatalogSelectionToEditingRow(selectedItem: MovimentoCatalogSelectorResultItem): boolean {
    const targetUid = Number(this.editingItemRowUid || 0);
    if (targetUid <= 0) {
      return false;
    }
    const index = this.findRowIndexByUid(targetUid);
    this.editingItemRowUid = null;
    if (index < 0) {
      this.notify.error('Nao foi possivel localizar o item em edicao.');
      return false;
    }
    const row = this.itemRows[index];
    if (!row) {
      return false;
    }

    const movementItemTypeId = Number(selectedItem.movementItemTypeId || 0);
    const itemType = this.itemTypesPermitidosView.find(current => current.tipoItemId === movementItemTypeId);
    row.movimentoItemTipoId = movementItemTypeId > 0 ? movementItemTypeId : null;
    row.catalogType = itemType?.catalogType || selectedItem.catalogType || row.catalogType || 'PRODUCTS';
    row.catalogItemId = Number(selectedItem.catalogItemId || 0) || null;
    row.tenantUnitId = selectedItem.tenantUnitId || null;
    row.tenantUnitSigla = selectedItem.unidade || null;
    row.codigo = row.codigo && row.codigo > 0 ? row.codigo : null;
    row.quantidade = this.toScaledNumber(selectedItem.quantidade, 3, 0) ?? 0;
    row.cobrar = itemType?.cobrar ?? row.cobrar;
    row.valorUnitario = row.cobrar ? (this.toScaledNumber(selectedItem.valorUnitario, 2, 0) ?? 0) : 0;
    row.estoqueMovimentado = false;
    row.finalizado = false;
    row.observacao = (selectedItem.observacao || '').trim();
    row.catalogSearchText = `${selectedItem.codigo} - ${selectedItem.nome}`;
    row.catalogOptions = [{
      id: Number(selectedItem.catalogItemId || 0),
      catalogType: selectedItem.catalogType,
      codigo: Number(selectedItem.codigo || 0),
      nome: selectedItem.nome || '-',
      descricao: null
    }];
    row.editing = false;
    row.saved = true;
    this.reindexRows();
    return true;
  }

  removeItemRow(index: number): void {
    const removed = this.itemRows[index];
    this.itemRows.splice(index, 1);
    if (removed && Number(this.editingItemRowUid || 0) === Number(removed.uid || 0)) {
      this.editingItemRowUid = null;
    }
    this.reindexRows();
  }

  lineTotal(row: MovimentoEstoqueItemDraft): number {
    if (!row.cobrar) {
      return 0;
    }
    const quantidade = this.toScaledNumber(row.quantidade, 3, 0) ?? 0;
    const valorUnitario = this.toScaledNumber(row.valorUnitario, 2, 0) ?? 0;
    return this.toScaledNumber(quantidade * valorUnitario, 2, 0) ?? 0;
  }

  itensTotalCobrado(): number {
    return this.itemRows.reduce((acc, row) => acc + this.lineTotal(row), 0);
  }

  tipoItemLabel(tipoItemId: number | null): string {
    if (!tipoItemId) {
      return '-';
    }
    return this.itemTypeNameById.get(tipoItemId) || `Tipo #${tipoItemId}`;
  }

  itemCatalogoResumo(row: MovimentoEstoqueItemDraft): string {
    return (row.catalogSearchText || '').trim() || '-';
  }

  onSavedItemConsult(item: MovimentoEstoqueItemResponse): void {
    const index = this.findRowIndexForItem(item);
    if (index < 0) {
      return;
    }
    const row = this.itemRows[index];
    if (!row) {
      return;
    }
    this.notify.info(`Item ${this.itemCatalogoResumo(row)}`);
  }

  onSavedItemEdit(item: MovimentoEstoqueItemResponse): void {
    if (this.isItemLockedForEdit(item)) {
      this.notify.error(this.lockReason(item));
      return;
    }
    if (this.mode === 'view' && this.movimento?.id) {
      this.router.navigate(['/movimentos/estoque', this.movimento.id, 'edit'], {
        queryParams: {
          returnTo: this.returnTo,
          editItemUid: item.id,
          editCatalogItemId: item.catalogItemId
        }
      });
      return;
    }
    const index = this.findRowIndexForItem(item);
    if (index >= 0) {
      this.openCatalogSelectorForEdit(index);
      return;
    }
    const fallbackIndex = this.itemRows.findIndex(row => row.saved);
    if (fallbackIndex >= 0) {
      this.openCatalogSelectorForEdit(fallbackIndex);
      this.notify.info('Item selecionado nao encontrado; abrindo primeiro item salvo para edicao.');
      return;
    }
    this.notify.error('Nao foi possivel localizar item salvo para edicao.');
  }

  onSavedItemDelete(item: MovimentoEstoqueItemResponse): void {
    if (this.isItemLockedForEdit(item)) {
      this.notify.error(this.lockReason(item));
      return;
    }
    if (this.mode === 'view') {
      this.notify.info('Abra a ficha em modo de edicao para excluir itens.');
      return;
    }
    const index = this.findRowIndexForItem(item);
    if (index >= 0) {
      this.removeItemRow(index);
      this.persistMovementAfterItemSave();
    }
  }

  onSavedItemTransition(event: {
    item: MovimentoEstoqueItemResponse;
    transitionKey: string;
    expectedCurrentStateKey?: string | null;
  }): void {
    if (!this.workflowEnabled || this.mode === 'new') {
      return;
    }
    const itemId = Number(event?.item?.id || 0);
    if (!itemId || !event.transitionKey) {
      return;
    }
    this.saving = true;
    this.workflowService.transition('ITEM_MOVIMENTO_ESTOQUE', itemId, {
      transitionKey: event.transitionKey,
      expectedCurrentStateKey: event.expectedCurrentStateKey || null,
      notes: 'Transicao manual na ficha do movimento'
    }).pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Transicao executada com sucesso.');
          if (this.movimento?.id) {
            this.loadMovimento(this.movimento.id);
          }
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel transicionar o item.')
      });
  }

  private buildItemRequests(): MovimentoEstoqueItemRequest[] | null {
    const items = (this.itemRows || []).map((row, idx) => ({ row, idx }));
    const requests: MovimentoEstoqueItemRequest[] = [];
    for (const current of items) {
      const row = current.row;
      if (!row.saved) {
        continue;
      }
      if (!row.movimentoItemTipoId && !row.catalogItemId && !row.observacao.trim()) {
        continue;
      }
      const error = this.validateItemRow(row, current.idx);
      if (error) {
        this.notify.error(error);
        return null;
      }
      const quantidade = this.toScaledNumber(row.quantidade, 3, 0) ?? 0;
      const valorUnitario = row.cobrar ? (this.toScaledNumber(row.valorUnitario, 2, 0) ?? 0) : 0;
      requests.push({
        movimentoItemTipoId: row.movimentoItemTipoId!,
        catalogItemId: row.catalogItemId!,
        tenantUnitId: row.tenantUnitId || null,
        quantidade,
        valorUnitario,
        ordem: current.idx,
        observacao: row.observacao?.trim() || null
      });
    }
    return requests;
  }

  private loadTemplate(): void {
    const empresaId = Number(localStorage.getItem('empresaContextId') || 0);
    if (!empresaId) {
      this.errorMessage = 'Selecione uma empresa no topo do sistema para criar um movimento.';
      this.form.disable();
      return;
    }
    this.loadTemplateByEmpresa(empresaId);
  }

  private loadTemplateByEmpresa(empresaId: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.buildTemplate('MOVIMENTO_ESTOQUE', empresaId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: template => {
          this.templateData = template;
          this.refreshAllowedViews();
          this.loadItemWorkflowStateColorsForCurrentConfig();
          this.ensureTipoEntidadeLabels(template.tiposEntidadePermitidos || []);
          const tipoEntidadeId = this.resolveTipoEntidadeInicial(template, this.movimento?.tipoEntidadePadraoId || null);
          this.empresaNomeHeader = this.resolveEmpresaNome(empresaId);
          if (this.mode === 'new') {
          this.form.patchValue({
            empresaId: template.empresaId,
            nome: template.nome || '',
            tipoEntidadeId,
            stockAdjustmentId: this.normalizeStockAdjustmentId(template.stockAdjustmentId),
            version: null
          });
        } else if (this.mode === 'edit') {
          this.form.patchValue({
            tipoEntidadeId,
            stockAdjustmentId: this.normalizeStockAdjustmentId(this.movimento?.stockAdjustmentId)
          });
        } else {
          this.form.patchValue({
            tipoEntidadeId: this.movimento?.tipoEntidadePadraoId || tipoEntidadeId || null,
            stockAdjustmentId: this.normalizeStockAdjustmentId(this.movimento?.stockAdjustmentId)
          }, { emitEvent: false });
        }
          this.form.enable();
          if (this.mode === 'view') {
            this.form.disable();
          }
        },
        error: err => {
          this.errorMessage = err?.error?.detail || 'Nao foi possivel resolver a configuracao para criar o movimento.';
          this.form.disable();
        }
      });
  }

  private loadMovimento(id: number): void {
    if (!id) {
      this.errorMessage = 'Movimento invalido.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.service.getEstoque(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: movimento => {
          this.movimento = movimento;
          if (this.mode === 'edit' && !!movimento.finalizado) {
            this.mode = 'view';
            this.title = 'Consultar movimento de estoque';
          }
          this.refreshAllowedViews();
          this.loadItemWorkflowStateColorsForCurrentConfig();
          this.ensureTipoEntidadeLabels([movimento.tipoEntidadePadraoId || 0]);
          this.form.patchValue({
            empresaId: movimento.empresaId,
            nome: movimento.nome || '',
            tipoEntidadeId: movimento.tipoEntidadePadraoId,
            stockAdjustmentId: this.normalizeStockAdjustmentId(movimento.stockAdjustmentId),
            version: movimento.version
          });
          this.empresaNomeHeader = this.resolveEmpresaNome(movimento.empresaId);

          const loadedRows = (movimento.itens || []).map((item, idx) => ({
            // Preserve backend id so list-driven edit can target the exact row.
            uid: Number(item.id || 0) > 0 ? Number(item.id) : this.nextRowUid++,
            codigo: Number(item.codigo || 0) > 0 ? Number(item.codigo) : (idx + 1),
            movimentoItemTipoId: item.movimentoItemTipoId,
            catalogType: item.catalogType,
            catalogItemId: item.catalogItemId,
            tenantUnitId: item.tenantUnitId || null,
            tenantUnitSigla: item.tenantUnitSigla || item.unidadeBaseCatalogoSigla || null,
            quantidade: this.toScaledNumber(item.quantidade, 3, 0) ?? 0,
            valorUnitario: this.toScaledNumber(item.valorUnitario, 2, 0) ?? 0,
            cobrar: item.cobrar,
            estoqueMovimentado: !!item.estoqueMovimentado,
            finalizado: !!item.finalizado,
            status: item.status || null,
            ordem: idx,
            observacao: item.observacao || '',
            catalogSearchText: `${item.catalogCodigoSnapshot} - ${item.catalogNomeSnapshot}`,
            catalogOptions: [{
              id: item.catalogItemId,
              catalogType: item.catalogType,
              codigo: item.catalogCodigoSnapshot,
              nome: item.catalogNomeSnapshot,
              descricao: null
            }],
            editing: false,
            saved: true
          }));
          const maxLoadedUid = loadedRows.reduce((maxUid, row) => Math.max(maxUid, Number(row.uid || 0)), 0);
          this.nextRowUid = Math.max(this.nextRowUid, maxLoadedUid + 1);
          this.itemRows = loadedRows;
          this.reindexRows();
          this.activatePendingItemEdit();
          this.loadTransitionsForSavedItems();

          this.loadTemplateByEmpresa(movimento.empresaId);

          if (this.mode === 'view') {
            this.form.disable();
          } else {
            this.form.enable();
          }
        },
        error: err => {
          this.errorMessage = err?.error?.detail || 'Nao foi possivel carregar o movimento.';
          this.form.disable();
        }
      });
  }

  private resolveEmpresaNome(empresaId: number): string {
    const contextId = Number(localStorage.getItem('empresaContextId') || 0);
    const contextName = (localStorage.getItem('empresaContextNome') || '').trim();
    if (contextId > 0 && contextId === empresaId && contextName) {
      return contextName;
    }
    return empresaId > 0 ? `Empresa #${empresaId}` : '-';
  }

  private toScaledNumber(value: unknown, scale: number, fallback: number | null): number | null {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
      return fallback;
    }
    const normalized = Math.abs(parsed);
    const factor = Math.pow(10, scale);
    return Math.round(normalized * factor) / factor;
  }

  private normalizeTipoEntidadeId(value: unknown): number | null {
    const parsed = Number(value || 0);
    return parsed > 0 ? parsed : null;
  }

  private normalizeStockAdjustmentId(value: unknown): number | null {
    const parsed = Number(value || 0);
    return parsed > 0 ? parsed : null;
  }

  private resolveTipoEntidadeInicial(
    template: MovimentoEstoqueTemplateResponse,
    fallback: number | null
  ): number | null {
    const allowed = (template.tiposEntidadePermitidos || []).filter(id => Number(id) > 0);
    const defaultId = this.normalizeTipoEntidadeId(template.tipoEntidadePadraoId);
    if (defaultId && allowed.includes(defaultId)) {
      return defaultId;
    }
    const fallbackId = this.normalizeTipoEntidadeId(fallback);
    if (fallbackId && allowed.includes(fallbackId)) {
      return fallbackId;
    }
    if (allowed.length === 1) {
      return allowed[0] ?? null;
    }
    return null;
  }

  private ensureTipoEntidadeLabels(ids: number[]): void {
    const requestedIds = (ids || []).filter(id => Number(id) > 0);
    if (!requestedIds.length) {
      return;
    }
    const missing = requestedIds.filter(id => !this.tipoEntidadeNomeById.has(id));
    if (!missing.length) {
      return;
    }
    this.entityTypeService.list({ page: 0, size: 200, ativo: true }).subscribe({
      next: response => {
        for (const item of response.content || []) {
          if (item?.id > 0) {
            this.tipoEntidadeNomeById.set(item.id, item.nome || `Tipo #${item.id}`);
          }
        }
      }
    });
  }

  private validateItemRow(row: MovimentoEstoqueItemDraft, idx: number): string | null {
    if (!row.movimentoItemTipoId) {
      return `Informe o tipo de item na linha ${idx + 1}.`;
    }
    if (!row.catalogItemId) {
      return `Informe o item de catalogo na linha ${idx + 1}.`;
    }
    const quantidade = this.toScaledNumber(row.quantidade, 3, 0) ?? 0;
    if (quantidade <= 0) {
      return `Quantidade deve ser maior que zero na linha ${idx + 1}.`;
    }
    return null;
  }

  private reindexRows(): void {
    this.itemRows = this.itemRows.map((item, idx) => ({ ...item, ordem: idx }));
    this.refreshItemViews();
  }

  private refreshAllowedViews(): void {
    const tiposEntidade = (this.templateData?.tiposEntidadePermitidos || [])
      .filter((id): id is number => Number(id) > 0);
    this.tiposEntidadePermitidosView = tiposEntidade;
    this.stockAdjustmentsView = (this.templateData?.stockAdjustments || [])
      .filter(item => Number(item?.id || 0) > 0);

    let itemTypes: MovimentoTipoItemTemplate[] = [];
    if (this.templateData?.tiposItensPermitidos?.length) {
      itemTypes = this.templateData.tiposItensPermitidos.slice();
    } else {
      const byId = new Map<number, MovimentoTipoItemTemplate>();
      for (const item of this.movimento?.itens || []) {
        byId.set(item.movimentoItemTipoId, {
          tipoItemId: item.movimentoItemTipoId,
          nome: item.movimentoItemTipoNome,
          catalogType: item.catalogType,
          cobrar: item.cobrar
        });
      }
      itemTypes = [...byId.values()];
    }
    this.itemTypesPermitidosView = itemTypes;
    this.itemTypeNameById = new Map<number, string>(itemTypes.map(item => [item.tipoItemId, item.nome]));
  }

  private refreshItemViews(): void {
    const tipoNomeById = this.itemTypeNameById;
    const saved: MovimentoEstoqueItemResponse[] = [];
    for (let idx = 0; idx < this.itemRows.length; idx += 1) {
      const row = this.itemRows[idx];
      if (!row) {
        continue;
      }
      if (row.saved) {
          const catalogSummary = this.parseCatalogSummary(row.catalogSearchText);
          saved.push({
            id: row.uid,
            codigo: Number(row.codigo || 0) > 0 ? Number(row.codigo) : (row.ordem + 1),
            movimentoItemTipoId: row.movimentoItemTipoId || 0,
          movimentoItemTipoNome: row.movimentoItemTipoId
            ? (tipoNomeById.get(row.movimentoItemTipoId) || `Tipo #${row.movimentoItemTipoId}`)
            : '-',
          catalogType: row.catalogType || 'PRODUCTS',
          catalogItemId: row.catalogItemId || 0,
          catalogCodigoSnapshot: catalogSummary.codigo,
          catalogNomeSnapshot: catalogSummary.nome,
          tenantUnitId: row.tenantUnitId || null,
          tenantUnitSigla: row.tenantUnitSigla || null,
          quantidade: this.toScaledNumber(row.quantidade, 3, 0) ?? 0,
          valorUnitario: this.toScaledNumber(row.cobrar ? row.valorUnitario : 0, 2, 0) ?? 0,
          valorTotal: this.lineTotal(row),
          cobrar: row.cobrar,
          estoqueMovimentado: !!row.estoqueMovimentado,
          estoqueMovimentacaoId: null,
          finalizado: !!row.finalizado,
          status: row.status || null,
          ordem: row.ordem,
          observacao: row.observacao || null
        });
      }
    }
    this.savedItemsView = saved;
    this.loadTransitionsForSavedItems();
  }

  private findRowIndexByUid(uid: number): number {
    const target = Number(uid || 0);
    return this.itemRows.findIndex(row => Number(row.uid || 0) === target);
  }

  private findRowIndexForItem(item: MovimentoEstoqueItemResponse): number {
    const byUid = this.findRowIndexByUid(item.id);
    if (byUid >= 0) {
      return byUid;
    }
    const catalogItemId = Number(item.catalogItemId || 0);
    const movimentoItemTipoId = Number(item.movimentoItemTipoId || 0);
    const ordem = Number(item.ordem ?? -1);
    const byComposite = this.itemRows.findIndex(row =>
      Number(row.catalogItemId || 0) === catalogItemId
      && Number(row.movimentoItemTipoId || 0) === movimentoItemTipoId
      && Number(row.ordem ?? -1) === ordem);
    if (byComposite >= 0) {
      return byComposite;
    }
    return this.itemRows.findIndex(row =>
      Number(row.catalogItemId || 0) === catalogItemId
      && Number(row.movimentoItemTipoId || 0) === movimentoItemTipoId);
  }

  private parseCatalogSummary(value: string): { codigo: number; nome: string } {
    const raw = (value || '').trim();
    if (!raw) {
      return { codigo: 0, nome: '-' };
    }
    const separatorIndex = raw.indexOf(' - ');
    if (separatorIndex <= 0) {
      return { codigo: 0, nome: raw };
    }
    const codigoPart = raw.slice(0, separatorIndex).trim();
    const nomePart = raw.slice(separatorIndex + 3).trim();
    const parsedCodigo = Number(codigoPart);
    return {
      codigo: Number.isFinite(parsedCodigo) ? parsedCodigo : 0,
      nome: nomePart || raw
    };
  }

  private activatePendingItemEdit(): void {
    if (this.mode !== 'edit') {
      return;
    }
    let index = -1;
    if (this.pendingEditItemUid) {
      index = this.findRowIndexByUid(this.pendingEditItemUid);
    }
    if (index < 0 && this.pendingEditCatalogItemId) {
      index = this.itemRows.findIndex(row => Number(row.catalogItemId || 0) === this.pendingEditCatalogItemId);
    }
    if (index >= 0) {
      this.openCatalogSelectorForEdit(index);
    }
    this.pendingEditItemUid = null;
    this.pendingEditCatalogItemId = null;
  }

  private openCatalogSelectorForEdit(index: number): void {
    const row = this.itemRows[index];
    if (!row) {
      return;
    }
    const movementItemTypeId = Number(row.movimentoItemTipoId || 0);
    const catalogItemId = Number(row.catalogItemId || 0);
    if (movementItemTypeId <= 0 || catalogItemId <= 0) {
      this.notify.error('Item invalido para edicao.');
      return;
    }
    const summary = this.parseCatalogSummary(row.catalogSearchText);
    const nome = summary.nome === '-' ? (row.catalogSearchText || '').trim() || `Item #${catalogItemId}` : summary.nome;
    const itemType = this.itemTypesPermitidosView.find(current => current.tipoItemId === movementItemTypeId);
    const catalogType = itemType?.catalogType || row.catalogType || 'PRODUCTS';

    this.editingItemRowUid = Number(row.uid || 0);
    this.openSearchOnSelectorOpen = false;
    this.catalogSelectorState = {
      movementItemTypeId: movementItemTypeId > 0 ? movementItemTypeId : null,
      q: '',
      searchFields: ['codigo', 'nome', 'descricao'],
      status: 'ativo',
      groupId: null,
      groupPath: null,
      groupBreadcrumb: null,
      includeDescendants: true,
      ativo: true,
      selectedItems: [{
        movementItemTypeId,
        catalogItemId,
        tenantUnitId: row.tenantUnitId || null,
        unidade: row.tenantUnitSigla || null,
        quantidade: this.toScaledNumber(row.quantidade, 3, 0) ?? 0,
        valorUnitario: row.cobrar ? (this.toScaledNumber(row.valorUnitario, 2, 0) ?? 0) : 0,
        observacao: (row.observacao || '').trim() || null,
        catalogType,
        codigo: Number(summary.codigo || 0),
        nome
      }]
    };
    this.selectedTabIndex = 1;
    this.reopenCatalogSelector();
  }

  private updateViewportState(): void {
    this.isMobileView = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }

  private persistMovementAfterItemSave(): void {
    if (this.mode === 'view') {
      return;
    }
    this.save({ keepEditing: true });
  }

  private reopenCatalogSelector(): void {
    if (this.catalogSelectorOpen) {
      this.catalogSelectorOpen = false;
      setTimeout(() => (this.catalogSelectorOpen = true), 0);
      return;
    }
    this.catalogSelectorOpen = true;
  }

  private loadTransitionsForSavedItems(): void {
    if (!this.workflowEnabled || this.mode === 'new') {
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      return;
    }
    const savedItems = this.savedItemsView || [];
    if (!savedItems.length) {
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      return;
    }
    this.itemTransitionsByItemId = {};
    this.itemStateNamesByItemId = {};
    this.itemStateKeysByItemId = {};
    const nextMap: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null; controlKey?: string | null }>> = {};
    const nextStateNames: Record<number, string> = {};
    const nextStateKeys: Record<number, string> = {};
    for (const item of savedItems) {
      const itemId = Number(item?.id || 0);
      if (!itemId) {
        continue;
      }
      this.workflowService.getRuntimeState('ITEM_MOVIMENTO_ESTOQUE', itemId).subscribe({
        next: runtime => {
          nextMap[itemId] = runtime.transitions || [];
          const stateName = (runtime.currentStateName || '').trim();
          const stateKey = (runtime.currentStateKey || '').trim();
          const stateColor = (runtime.currentStateColor || '').trim();
          if (stateName) {
            nextStateNames[itemId] = stateName;
          }
          if (stateKey) {
            nextStateKeys[itemId] = stateKey;
            if (this.isValidHexColor(stateColor)) {
              this.itemStateColorsByStateKey = {
                ...this.itemStateColorsByStateKey,
                [stateKey.toUpperCase()]: stateColor
              };
            }
          }
          this.itemTransitionsByItemId = { ...this.itemTransitionsByItemId, ...nextMap };
          this.itemStateNamesByItemId = { ...this.itemStateNamesByItemId, ...nextStateNames };
          this.itemStateKeysByItemId = { ...this.itemStateKeysByItemId, ...nextStateKeys };
        },
        error: () => {
          nextMap[itemId] = [];
          this.itemTransitionsByItemId = { ...this.itemTransitionsByItemId, ...nextMap };
        }
      });
    }
  }

  private loadItemWorkflowStateColorsForCurrentConfig(): void {
    if (!this.workflowEnabled) {
      return;
    }
    const configId = Number(this.resolveMovementConfigId() || 0);
    if (!Number.isFinite(configId) || configId <= 0) {
      return;
    }
    if (this.loadedItemColorConfigIds.has(configId)) {
      return;
    }
    this.loadedItemColorConfigIds.add(configId);
    this.workflowService.getDefinitionByOrigin('ITEM_MOVIMENTO_ESTOQUE', {
      type: 'MOVIMENTO_CONFIG',
      id: configId
    }).subscribe({
      next: definition => {
        this.itemStateColorsByStateKey = {
          ...this.itemStateColorsByStateKey,
          ...this.buildStateColorMap(definition?.states || [])
        };
      },
      error: () => {
        this.loadedItemColorConfigIds.delete(configId);
      }
    });
  }

  private buildStateColorMap(states: Array<{ key?: string | null; color?: string | null }>): Record<string, string> {
    const map: Record<string, string> = {};
    for (const state of states || []) {
      const key = (state?.key || '').trim().toUpperCase();
      const color = (state?.color || '').trim();
      if (!key || !this.isValidHexColor(color)) {
        continue;
      }
      map[key] = color;
    }
    return map;
  }

  private isValidHexColor(value: string): boolean {
    return /^#[\da-fA-F]{6}$/.test((value || '').trim());
  }

  private isMovimentoFinalizado(): boolean {
    return this.mode !== 'new' && !!this.movimento?.finalizado;
  }

  private isItemLockedForEdit(item: MovimentoEstoqueItemResponse): boolean {
    if (this.isMovimentoFinalizado()) {
      return true;
    }
    return !!item?.estoqueMovimentado || !!item?.finalizado;
  }

  private lockReason(item: MovimentoEstoqueItemResponse): string {
    if (this.isMovimentoFinalizado()) {
      return 'Movimento finalizado. Nao e permitido alterar ou excluir itens.';
    }
    if (item?.finalizado) {
      return 'Item finalizado. Nao e permitido alterar ou excluir.';
    }
    if (item?.estoqueMovimentado) {
      return 'Item com movimentacao de estoque. Nao e permitido alterar ou excluir sem desfazer.';
    }
    return 'Item bloqueado para alteracao.';
  }
}

