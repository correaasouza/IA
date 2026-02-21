import { Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { WorkflowService } from '../workflows/workflow.service';
import { MovementOperationService, MovimentoEstoqueItemResponse, MovimentoEstoqueResponse } from './movement-operation.service';
import { MovimentoItensListComponent } from './components/movimento-itens-list.component';

@Component({
  selector: 'app-movimento-estoque-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule,
    MatDialogModule,
    FieldSearchComponent,
    AccessControlDirective,
    MovimentoItensListComponent
  ],
  templateUrl: './movimento-estoque-list.component.html',
  styleUrls: ['./movimento-estoque-list.component.css']
})
export class MovimentoEstoqueListComponent implements OnInit {
  displayedColumns = ['nome', 'movimentoConfig', 'tipoEntidadePadrao', 'status', 'acoes'];
  rows: MovimentoEstoqueResponse[] = [];
  showTipoEntidadePadrao = false;
  selectedMovimentoId: number | null = null;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  hasMoreRows = true;
  loadingMoreRows = false;
  itemChunkSize = 30;
  visibleItemCount = 30;
  itemSaving = false;
  loading = false;
  isMobile = false;
  mobileFiltersOpen = false;
  workflowEnabled = true;
  itemTransitionsByItemId: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
  itemStateNamesByItemId: Record<number, string> = {};
  itemStateKeysByItemId: Record<number, string> = {};
  itemStateColorsByStateKey: Record<string, string> = {};
  movimentoStateNamesById: Record<number, string> = {};
  movimentoStateKeysById: Record<number, string> = {};
  movimentoStateColorsByStateKey: Record<string, string> = {};
  movimentoTransitionsById: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
  private loadedItemColorConfigIds = new Set<number>();
  private loadedMovementColorConfigIds = new Set<number>();

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['nome'];
  @ViewChild('movimentosPane') movimentosPane?: ElementRef<HTMLElement>;
  @ViewChild('itensPane') itensPane?: ElementRef<HTMLElement>;

  constructor(
    private dialog: MatDialog,
    private router: Router,
    private notify: NotificationService,
    private service: MovementOperationService,
    private workflowService: WorkflowService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load(true);
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  @HostListener('window:empresa-context-updated')
  onEmpresaContextUpdated(): void {
    this.load(true);
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (!this.isMobile) {
      return;
    }
    const scrollTop = window.scrollY || document.documentElement.scrollTop || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
    const fullHeight = document.documentElement.scrollHeight || 0;
    if (scrollTop + viewportHeight >= fullHeight - 180) {
      this.load(false);
    }
  }

  load(reset = false, keepSelection = false): void {
    if (this.loadingMoreRows) {
      return;
    }
    if (!reset && !this.hasMoreRows) {
      return;
    }
    if (reset) {
      this.pageIndex = 0;
      this.hasMoreRows = true;
      this.rows = [];
      if (!keepSelection) {
        this.selectedMovimentoId = null;
        this.visibleItemCount = this.itemChunkSize;
      }
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.itemStateColorsByStateKey = {};
      this.movimentoStateNamesById = {};
      this.movimentoStateKeysById = {};
      this.movimentoStateColorsByStateKey = {};
      this.movimentoTransitionsById = {};
      this.loadedItemColorConfigIds.clear();
      this.loadedMovementColorConfigIds.clear();
    }
    const nome = this.searchFields.includes('nome') ? this.searchTerm : '';
    const targetPage = reset ? 0 : this.pageIndex;
    this.loading = reset;
    this.loadingMoreRows = true;
    this.service.listEstoque({
      page: targetPage,
      size: this.pageSize,
      nome
    }).pipe(finalize(() => {
      this.loading = false;
      this.loadingMoreRows = false;
    })).subscribe({
      next: page => {
        const incoming = page.content || [];
        this.rows = reset ? incoming : [...this.rows, ...incoming];
        const serverTotal = Number(page.totalElements || 0);
        const receivedFullPage = incoming.length >= this.pageSize;
        if (serverTotal > 0) {
          this.totalElements = serverTotal;
          this.hasMoreRows = this.rows.length < this.totalElements;
        } else {
          const inferredTotal = this.rows.length + (receivedFullPage ? 1 : 0);
          this.totalElements = Math.max(this.totalElements, inferredTotal);
          this.hasMoreRows = receivedFullPage;
        }
        this.pageIndex = targetPage + 1;
        this.syncSelectedRow();
        this.loadWorkflowDefinitionColorsForRows();
        this.loadMovementStateNames();
        this.loadTransitionsForSelectedItems();
        this.showTipoEntidadePadrao = this.rows.some(item => item.tipoEntidadePadraoId != null);
        this.displayedColumns = this.showTipoEntidadePadrao
          ? ['nome', 'movimentoConfig', 'tipoEntidadePadrao', 'status', 'acoes']
          : ['nome', 'movimentoConfig', 'status', 'acoes'];
        this.ensureMovimentosFillViewport();
      },
      error: err => {
        this.rows = [];
        this.totalElements = 0;
        this.pageIndex = 0;
        this.hasMoreRows = false;
        this.showTipoEntidadePadrao = false;
        this.displayedColumns = ['nome', 'movimentoConfig', 'status', 'acoes'];
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar movimentos de estoque.');
      }
    });
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applyFilters();
  }

  applyFilters(): void {
    this.load(true);
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['nome'];
    this.applyFilters();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    return (this.searchTerm || '').trim() ? 1 : 0;
  }

  onMovimentosScroll(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target) {
      return;
    }
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 120) {
      this.load(false);
    }
  }

  onItensScroll(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target) {
      return;
    }
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 120) {
      this.visibleItemCount += this.itemChunkSize;
      this.ensureItensFillViewport();
    }
  }

  newMovimento(): void {
    this.router.navigate(['/movimentos/estoque/new'], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  view(row: MovimentoEstoqueResponse): void {
    this.router.navigate(['/movimentos/estoque', row.id], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  selectRow(row: MovimentoEstoqueResponse): void {
    this.selectedMovimentoId = row.id;
    this.visibleItemCount = this.itemChunkSize;
    this.itemTransitionsByItemId = {};
    this.itemStateNamesByItemId = {};
    this.itemStateKeysByItemId = {};
    this.loadWorkflowDefinitionColorsForConfig(Number(row.movimentoConfigId || 0));
    this.loadTransitionsForSelectedItems();
    this.ensureItensFillViewport();
  }

  isSelected(row: MovimentoEstoqueResponse): boolean {
    return row.id === this.selectedMovimentoId;
  }

  selectedRow(): MovimentoEstoqueResponse | null {
    if (!this.rows.length || this.selectedMovimentoId == null) {
      return null;
    }
    return this.rows.find(item => item.id === this.selectedMovimentoId) || null;
  }

  selectedItems(): MovimentoEstoqueItemResponse[] {
    return this.selectedRow()?.itens || [];
  }

  visibleSelectedItems(): MovimentoEstoqueItemResponse[] {
    const all = this.selectedItems();
    return all.slice(0, this.visibleItemCount);
  }

  consultItem(item: MovimentoEstoqueItemResponse): void {
    const selected = this.selectedRow();
    if (!selected) return;
    this.notify.info(`Item ${item.catalogCodigoSnapshot} - ${item.catalogNomeSnapshot}`);
    this.view(selected);
  }

  removeItem(item: MovimentoEstoqueItemResponse): void {
    const selected = this.selectedRow();
    if (!selected || this.itemSaving) return;
    const remaining = (selected.itens || []).filter(current => current.id !== item.id);
    const payload = {
      empresaId: selected.empresaId,
      nome: selected.nome,
      tipoEntidadeId: selected.tipoEntidadePadraoId,
      stockAdjustmentId: selected.stockAdjustmentId,
      version: selected.version,
      itens: remaining.map((current, idx) => ({
        movimentoItemTipoId: current.movimentoItemTipoId,
        catalogItemId: current.catalogItemId,
        quantidade: current.quantidade,
        valorUnitario: current.valorUnitario,
        ordem: idx,
        observacao: current.observacao || null
      }))
    };
    this.itemSaving = true;
    this.service.updateEstoque(selected.id, payload)
      .pipe(finalize(() => (this.itemSaving = false)))
      .subscribe({
        next: updated => {
          this.rows = this.rows.map(row => row.id === updated.id ? updated : row);
          this.notify.success('Item excluido do movimento.');
          this.syncSelectedRow();
          this.loadTransitionsForSelectedItems();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o item do movimento.')
      });
  }

  edit(row: MovimentoEstoqueResponse): void {
    this.router.navigate(['/movimentos/estoque', row.id, 'edit'], { queryParams: { returnTo: '/movimentos/estoque' } });
  }

  remove(row: MovimentoEstoqueResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir movimento',
        message: `Deseja excluir o movimento "${row.nome}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.service.deleteEstoque(row.id).subscribe({
        next: () => {
          this.notify.success('Movimento excluido.');
          this.load(true);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o movimento.')
      });
    });
  }

  openItemOnMovimentoForm(item: MovimentoEstoqueItemResponse): void {
    const selected = this.selectedRow();
    if (!selected) {
      return;
    }
    this.router.navigate(['/movimentos/estoque', selected.id, 'edit'], {
      queryParams: {
        returnTo: '/movimentos/estoque',
        editItemUid: item.id,
        editCatalogItemId: item.catalogItemId
      }
    });
  }

  onTransitionItem(event: {
    item: MovimentoEstoqueItemResponse;
    transitionKey: string;
    expectedCurrentStateKey?: string | null;
  }): void {
    if (!this.workflowEnabled) {
      return;
    }
    const itemId = Number(event?.item?.id || 0);
    if (!itemId || !event.transitionKey) {
      return;
    }
    this.itemSaving = true;
    this.workflowService.transition('ITEM_MOVIMENTO_ESTOQUE', itemId, {
      transitionKey: event.transitionKey,
      expectedCurrentStateKey: event.expectedCurrentStateKey || null,
      notes: 'Transicao manual pela lista de movimentos'
    }).pipe(finalize(() => (this.itemSaving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Transicao executada com sucesso.');
          this.load(true, true);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel transicionar o item.')
      });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }

  private syncSelectedRow(): void {
    const previousId = this.selectedMovimentoId;
    if (!this.rows.length) {
      this.selectedMovimentoId = null;
      this.visibleItemCount = this.itemChunkSize;
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.itemStateColorsByStateKey = {};
      this.movimentoTransitionsById = {};
      this.movimentoStateKeysById = {};
      this.movimentoStateColorsByStateKey = {};
      return;
    }
    if (this.selectedMovimentoId != null && this.rows.some(item => item.id === this.selectedMovimentoId)) {
      return;
    }
    this.selectedMovimentoId = this.rows[0]?.id ?? null;
    if (previousId !== this.selectedMovimentoId) {
      this.visibleItemCount = this.itemChunkSize;
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.movimentoTransitionsById = {};
      this.movimentoStateKeysById = {};
      this.loadTransitionsForSelectedItems();
      this.ensureItensFillViewport();
    }
  }

  private ensureMovimentosFillViewport(): void {
    setTimeout(() => {
      if (this.loadingMoreRows || !this.hasMoreRows) {
        return;
      }
      const pane = this.movimentosPane?.nativeElement;
      if (pane && pane.scrollHeight <= pane.clientHeight + 4) {
        this.load(false);
        return;
      }
      if (this.isMobile) {
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
        const fullHeight = document.documentElement.scrollHeight || 0;
        if (fullHeight <= viewportHeight + 120) {
          this.load(false);
        }
      }
    }, 0);
  }

  private ensureItensFillViewport(): void {
    setTimeout(() => {
      const total = this.selectedItems().length;
      if (this.visibleItemCount >= total) {
        return;
      }
      const pane = this.itensPane?.nativeElement;
      if (!pane) {
        return;
      }
      if (pane.scrollHeight <= pane.clientHeight + 4) {
        this.visibleItemCount = Math.min(this.visibleItemCount + this.itemChunkSize, total);
        this.ensureItensFillViewport();
      }
    }, 0);
  }

  private loadTransitionsForSelectedItems(): void {
    if (!this.workflowEnabled) {
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.itemStateColorsByStateKey = {};
      return;
    }
    const selected = this.selectedRow();
    if (!selected) {
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.itemStateColorsByStateKey = {};
      return;
    }
    const items = selected.itens || [];
    if (!items.length) {
      this.itemTransitionsByItemId = {};
      this.itemStateNamesByItemId = {};
      this.itemStateKeysByItemId = {};
      this.itemStateColorsByStateKey = {};
      return;
    }
    this.itemTransitionsByItemId = {};
    this.itemStateNamesByItemId = {};
    this.itemStateKeysByItemId = {};
    const nextMap: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
    const nextStateNames: Record<number, string> = {};
    const nextStateKeys: Record<number, string> = {};
    for (const item of items) {
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

  movementStatus(row: MovimentoEstoqueResponse): string {
    const rowId = Number(row?.id || 0);
    const mapped = rowId > 0 ? (this.movimentoStateNamesById[rowId] || '').trim() : '';
    if (mapped) {
      return mapped;
    }
    const raw = (row?.status || '').trim();
    if (!raw) {
      return '-';
    }
    return this.looksLikeUuid(raw) ? '-' : raw;
  }

  movementStatusColor(row: MovimentoEstoqueResponse): string | null {
    const rowId = Number(row?.id || 0);
    const runtimeStateKey = rowId > 0 ? (this.movimentoStateKeysById[rowId] || '').trim().toUpperCase() : '';
    if (runtimeStateKey) {
      const runtimeColor = (this.movimentoStateColorsByStateKey[runtimeStateKey] || '').trim();
      if (this.isValidHexColor(runtimeColor)) {
        return runtimeColor;
      }
    }
    const raw = (row?.status || '').trim().toUpperCase();
    if (!raw || this.looksLikeUuid(raw)) {
      return null;
    }
    const fallbackColor = (this.movimentoStateColorsByStateKey[raw] || '').trim();
    return this.isValidHexColor(fallbackColor) ? fallbackColor : null;
  }

  movementTransitions(row: MovimentoEstoqueResponse): Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }> {
    const rowId = Number(row?.id || 0);
    if (!rowId) {
      return [];
    }
    return this.movimentoTransitionsById[rowId] || [];
  }

  onTransitionMovement(row: MovimentoEstoqueResponse, transitionKey: string): void {
    const rowId = Number(row?.id || 0);
    if (!rowId || !transitionKey) {
      return;
    }
    const expectedCurrentStateKey = (this.movimentoStateKeysById[rowId] || '').trim();
    this.itemSaving = true;
    this.workflowService.transition('MOVIMENTO_ESTOQUE', rowId, {
      transitionKey,
      expectedCurrentStateKey: expectedCurrentStateKey || null,
      notes: 'Transicao manual pela lista de movimentos'
    }).pipe(finalize(() => (this.itemSaving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Transicao do movimento executada com sucesso.');
          this.load(true, true);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel transicionar o movimento.')
      });
  }

  private loadMovementStateNames(): void {
    if (!this.workflowEnabled) {
      this.movimentoStateNamesById = {};
      this.movimentoStateKeysById = {};
      this.movimentoStateColorsByStateKey = {};
      this.movimentoTransitionsById = {};
      return;
    }
    if (!this.rows.length) {
      this.movimentoStateNamesById = {};
      this.movimentoStateKeysById = {};
      this.movimentoStateColorsByStateKey = {};
      this.movimentoTransitionsById = {};
      return;
    }
    this.movimentoStateNamesById = {};
    this.movimentoStateKeysById = {};
    this.movimentoTransitionsById = {};
    const nextStateNames: Record<number, string> = {};
    const nextStateKeys: Record<number, string> = {};
    const nextTransitions: Record<number, Array<{ key: string; name: string; toStateKey: string; toStateName?: string | null }>> = {};
    for (const row of this.rows) {
      const rowId = Number(row?.id || 0);
      if (!rowId) {
        continue;
      }
      this.workflowService.getRuntimeState('MOVIMENTO_ESTOQUE', rowId).subscribe({
        next: runtime => {
          const stateName = (runtime.currentStateName || '').trim();
          const stateKey = (runtime.currentStateKey || '').trim();
          const stateColor = (runtime.currentStateColor || '').trim();
          if (stateName) {
            nextStateNames[rowId] = stateName;
          }
          if (stateKey) {
            nextStateKeys[rowId] = stateKey;
            if (this.isValidHexColor(stateColor)) {
              this.movimentoStateColorsByStateKey = {
                ...this.movimentoStateColorsByStateKey,
                [stateKey.toUpperCase()]: stateColor
              };
            }
          }
          nextTransitions[rowId] = runtime.transitions || [];
          this.movimentoStateNamesById = { ...this.movimentoStateNamesById, ...nextStateNames };
          this.movimentoStateKeysById = { ...this.movimentoStateKeysById, ...nextStateKeys };
          this.movimentoTransitionsById = { ...this.movimentoTransitionsById, ...nextTransitions };
        },
        error: () => {
          nextTransitions[rowId] = [];
          this.movimentoTransitionsById = { ...this.movimentoTransitionsById, ...nextTransitions };
        }
      });
    }
  }

  private loadWorkflowDefinitionColorsForRows(): void {
    if (!this.workflowEnabled || !this.rows.length) {
      return;
    }
    const configIds = [...new Set(
      this.rows
        .map(row => Number(row?.movimentoConfigId || 0))
        .filter(id => Number.isFinite(id) && id > 0)
    )];
    for (const configId of configIds) {
      this.loadWorkflowDefinitionColorsForConfig(configId);
    }
  }

  private loadWorkflowDefinitionColorsForConfig(configId: number): void {
    if (!this.workflowEnabled || !Number.isFinite(configId) || configId <= 0) {
      return;
    }
    if (!this.loadedItemColorConfigIds.has(configId)) {
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
    if (!this.loadedMovementColorConfigIds.has(configId)) {
      this.loadedMovementColorConfigIds.add(configId);
      this.workflowService.getDefinitionByOrigin('MOVIMENTO_ESTOQUE', {
        type: 'MOVIMENTO_CONFIG',
        id: configId
      }).subscribe({
        next: definition => {
          this.movimentoStateColorsByStateKey = {
            ...this.movimentoStateColorsByStateKey,
            ...this.buildStateColorMap(definition?.states || [])
          };
        },
        error: () => {
          this.loadedMovementColorConfigIds.delete(configId);
        }
      });
    }
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

  private looksLikeUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  }
}
