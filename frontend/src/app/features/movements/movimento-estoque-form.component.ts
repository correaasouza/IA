import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { FeatureFlagService } from '../../core/features/feature-flag.service';
import { EntityTypeService } from '../entity-types/entity-type.service';
import { WorkflowService } from '../workflows/workflow.service';
import { MovimentoItensListComponent } from './components/movimento-itens-list.component';
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
  movimentoItemTipoId: number | null;
  catalogType: 'PRODUCTS' | 'SERVICES' | null;
  catalogItemId: number | null;
  quantidade: number;
  valorUnitario: number;
  cobrar: boolean;
  ordem: number;
  observacao: string;
  status?: string | null;
  catalogSearchText: string;
  catalogOptions: MovimentoItemCatalogOption[];
  editing: boolean;
  saved: boolean;
}

interface MovimentoEstoqueEditingRowView {
  row: MovimentoEstoqueItemDraft;
  index: number;
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
    MatSlideToggleModule,
    InlineLoaderComponent,
    AccessControlDirective,
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
  itemRows: MovimentoEstoqueItemDraft[] = [];
  editingRowsView: MovimentoEstoqueEditingRowView[] = [];
  savedItemsView: MovimentoEstoqueItemResponse[] = [];
  stockAdjustmentsView: MovimentoStockAdjustmentOption[] = [];
  tiposEntidadePermitidosView: number[] = [];
  itemTypesPermitidosView: MovimentoTipoItemTemplate[] = [];
  loadingCatalogByRow = new Map<number, boolean>();
  tipoEntidadeNomeById = new Map<number, string>();
  itemTypeNameById = new Map<number, string>();
  workflowEnabled = true;
  itemTransitionsByItemId: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
  itemStateNamesByItemId: Record<number, string> = {};
  itemStateKeysByItemId: Record<number, string> = {};
  itemStateColorsByStateKey: Record<string, string> = {};
  @ViewChild('itemEditorAnchor') itemEditorAnchor?: ElementRef<HTMLElement>;

  private nextRowUid = 1;
  private pendingEditItemUid: number | null = null;
  private pendingEditCatalogItemId: number | null = null;

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
    this.workflowEnabled = this.featureFlagService.isEnabled('workflowEnabled', true);
    this.loadItemWorkflowStateColors();
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

  save(): void {
    if (this.mode === 'view') {
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
            this.router.navigate(['/movimentos/estoque', created.id], { queryParams: { returnTo: this.returnTo } });
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
          this.router.navigate(['/movimentos/estoque', updated.id], { queryParams: { returnTo: this.returnTo } });
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o movimento de estoque.')
      });
  }

  remove(): void {
    if (!this.movimento?.id) return;
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
    this.router.navigate(['/movimentos/estoque', this.movimento.id, 'edit'], { queryParams: { returnTo: this.returnTo } });
  }

  back(): void {
    this.router.navigateByUrl(this.returnTo);
  }

  nomeAtual(): string {
    return (this.form.get('nome')?.value || this.movimento?.nome || '-').trim() || '-';
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

  addItemRow(): void {
    const row: MovimentoEstoqueItemDraft = {
      uid: this.nextRowUid++,
      movimentoItemTipoId: null,
      catalogType: null,
      catalogItemId: null,
      quantidade: 1,
      valorUnitario: 0,
      cobrar: true,
      ordem: this.itemRows.length,
      observacao: '',
      status: null,
      catalogSearchText: '',
      catalogOptions: [],
      editing: true,
      saved: false
    };
    this.itemRows = [row, ...this.itemRows];
    this.reindexRows();
  }

  removeItemRow(index: number): void {
    this.itemRows.splice(index, 1);
    this.reindexRows();
  }

  saveItemRow(index: number): void {
    const row = this.itemRows[index];
    if (!row) return;
    const error = this.validateItemRow(row, index);
    if (error) {
      this.notify.error(error);
      return;
    }
    row.editing = false;
    row.saved = true;
    this.reindexRows();
  }

  editItemRow(index: number): void {
    const row = this.itemRows[index];
    if (!row) return;
    row.editing = true;
    this.itemRows = [...this.itemRows];
    this.refreshItemViews();
  }

  cancelItemRow(index: number): void {
    const row = this.itemRows[index];
    if (!row) return;
    if (!row.saved) {
      this.removeItemRow(index);
      return;
    }
    row.editing = false;
    this.reindexRows();
  }

  onTipoItemChange(row: MovimentoEstoqueItemDraft): void {
    const tipo = this.itemTypesPermitidos().find(item => item.tipoItemId === row.movimentoItemTipoId);
    row.catalogType = tipo?.catalogType || null;
    row.cobrar = tipo?.cobrar ?? true;
    row.catalogItemId = null;
    row.catalogOptions = [];
    if (!row.cobrar) {
      row.valorUnitario = 0;
    }
    if (row.movimentoItemTipoId) {
      this.searchCatalog(row);
    }
  }

  isRowEditable(row: MovimentoEstoqueItemDraft): boolean {
    return this.mode !== 'view' && row.editing;
  }

  searchCatalog(row: MovimentoEstoqueItemDraft): void {
    if (!row.movimentoItemTipoId) {
      row.catalogOptions = [];
      return;
    }
    this.loadingCatalogByRow.set(row.uid, true);
    this.service.searchCatalogItemsByTipoItem(row.movimentoItemTipoId, row.catalogSearchText || '', 0, 20)
      .pipe(finalize(() => this.loadingCatalogByRow.set(row.uid, false)))
      .subscribe({
        next: page => {
          row.catalogOptions = (page?.content || []).slice();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel buscar itens de catalogo.')
      });
  }

  onCatalogItemChange(row: MovimentoEstoqueItemDraft): void {
    if (!row.catalogItemId) {
      return;
    }
    const option = row.catalogOptions.find(item => item.id === row.catalogItemId);
    if (option) {
      row.catalogSearchText = `${option.codigo} - ${option.nome}`;
    }
  }

  onValorUnitarioChange(row: MovimentoEstoqueItemDraft): void {
    if (!row.cobrar) {
      row.valorUnitario = 0;
    }
  }

  lineTotal(row: MovimentoEstoqueItemDraft): number {
    if (!row.cobrar) {
      return 0;
    }
    const quantidade = Number(row.quantidade || 0);
    const valorUnitario = Number(row.valorUnitario || 0);
    return quantidade * valorUnitario;
  }

  itensTotalCobrado(): number {
    return this.itemRows.reduce((acc, row) => acc + this.lineTotal(row), 0);
  }

  isRowCatalogLoading(row: MovimentoEstoqueItemDraft): boolean {
    return !!this.loadingCatalogByRow.get(row.uid);
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
      this.editItemRow(index);
      this.scrollToItemEditor();
      return;
    }
    const fallbackIndex = this.itemRows.findIndex(row => row.saved && !row.editing);
    if (fallbackIndex >= 0) {
      this.editItemRow(fallbackIndex);
      this.scrollToItemEditor();
      this.notify.info('Item selecionado nao encontrado; abrindo primeiro item salvo para edicao.');
      return;
    }
    this.notify.error('Nao foi possivel localizar item salvo para edicao.');
  }

  onSavedItemDelete(item: MovimentoEstoqueItemResponse): void {
    if (this.mode === 'view') {
      this.notify.info('Abra a ficha em modo de edicao para excluir itens.');
      return;
    }
    const index = this.findRowIndexForItem(item);
    if (index >= 0) {
      this.removeItemRow(index);
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

  catalogTypeLabel(value: 'PRODUCTS' | 'SERVICES' | null): string {
    if (!value) {
      return '-';
    }
    return value === 'SERVICES' ? 'Servicos' : 'Produtos';
  }

  private buildItemRequests(): MovimentoEstoqueItemRequest[] | null {
    const items = (this.itemRows || []).map((row, idx) => ({ row, idx }));
    const requests: MovimentoEstoqueItemRequest[] = [];
    for (const current of items) {
      const row = current.row;
      if (row.editing) {
        if (this.rowHasContent(row)) {
          this.notify.error(`Salve o item da linha ${current.idx + 1} antes de salvar o movimento.`);
          return null;
        }
        continue;
      }
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
      const quantidade = Number(row.quantidade || 0);
      const valorUnitario = row.cobrar ? Number(row.valorUnitario || 0) : 0;
      requests.push({
        movimentoItemTipoId: row.movimentoItemTipoId!,
        catalogItemId: row.catalogItemId!,
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
    this.loadTemplateByEmpresa(empresaId, true);
  }

  private loadTemplateByEmpresa(empresaId: number, initializeRows = false): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.buildTemplate('MOVIMENTO_ESTOQUE', empresaId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: template => {
          this.templateData = template;
          this.refreshAllowedViews();
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
          if (initializeRows && this.mode !== 'view' && this.itemRows.length === 0) {
            this.addItemRow();
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
          this.refreshAllowedViews();
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
            movimentoItemTipoId: item.movimentoItemTipoId,
            catalogType: item.catalogType,
            catalogItemId: item.catalogItemId,
            quantidade: Number(item.quantidade || 0),
            valorUnitario: Number(item.valorUnitario || 0),
            cobrar: item.cobrar,
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

          this.loadTemplateByEmpresa(movimento.empresaId, false);

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

  private rowHasContent(row: MovimentoEstoqueItemDraft): boolean {
    return !!row.movimentoItemTipoId
      || !!row.catalogItemId
      || Number(row.quantidade || 0) > 0
      || Number(row.valorUnitario || 0) > 0
      || !!(row.observacao || '').trim();
  }

  private validateItemRow(row: MovimentoEstoqueItemDraft, idx: number): string | null {
    if (!row.movimentoItemTipoId) {
      return `Informe o tipo de item na linha ${idx + 1}.`;
    }
    if (!row.catalogItemId) {
      return `Informe o item de catalogo na linha ${idx + 1}.`;
    }
    const quantidade = Number(row.quantidade || 0);
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
    const editing: MovimentoEstoqueEditingRowView[] = [];
    const saved: MovimentoEstoqueItemResponse[] = [];
    for (let idx = 0; idx < this.itemRows.length; idx += 1) {
      const row = this.itemRows[idx];
      if (!row) {
        continue;
      }
      if (row.editing) {
        editing.push({ row, index: idx });
      }
      if (row.saved && !row.editing) {
        const catalogSummary = this.parseCatalogSummary(row.catalogSearchText);
        saved.push({
          id: row.uid,
          movimentoItemTipoId: row.movimentoItemTipoId || 0,
          movimentoItemTipoNome: row.movimentoItemTipoId
            ? (tipoNomeById.get(row.movimentoItemTipoId) || `Tipo #${row.movimentoItemTipoId}`)
            : '-',
          catalogType: row.catalogType || 'PRODUCTS',
          catalogItemId: row.catalogItemId || 0,
          catalogCodigoSnapshot: catalogSummary.codigo,
          catalogNomeSnapshot: catalogSummary.nome,
          quantidade: Number(row.quantidade || 0),
          valorUnitario: Number(row.cobrar ? row.valorUnitario : 0),
          valorTotal: this.lineTotal(row),
          cobrar: row.cobrar,
          status: row.status || null,
          ordem: row.ordem,
          observacao: row.observacao || null
        });
      }
    }
    this.editingRowsView = editing;
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

  private scrollToItemEditor(): void {
    setTimeout(() => {
      this.itemEditorAnchor?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 50);
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
      this.editItemRow(index);
      this.scrollToItemEditor();
    }
    this.pendingEditItemUid = null;
    this.pendingEditCatalogItemId = null;
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
    const nextMap: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
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
          if (stateName) {
            nextStateNames[itemId] = stateName;
          }
          if (stateKey) {
            nextStateKeys[itemId] = stateKey;
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

  private loadItemWorkflowStateColors(): void {
    if (!this.workflowEnabled) {
      this.itemStateColorsByStateKey = {};
      return;
    }
    this.workflowService.getDefinitionByOrigin('ITEM_MOVIMENTO_ESTOQUE').subscribe({
      next: definition => {
        this.itemStateColorsByStateKey = this.buildStateColorMap(definition?.states || []);
      },
      error: () => {
        this.itemStateColorsByStateKey = {};
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
}
