import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import {
  FieldSearchComponent,
  FieldSearchOption,
  FieldSearchValue
} from '../../shared/field-search/field-search.component';
import { NotificationService } from '../../core/notifications/notification.service';
import {
  CatalogCrudType,
  CatalogItem,
  CatalogItemContext,
  CatalogItemPayload,
  CatalogItemService
} from './catalog-item.service';
import { CatalogGroupNode, CatalogGroupService } from './catalog-group.service';
import { CatalogGroupsTreeComponent } from './catalog-groups-tree.component';
import { CatalogItemHistoryDialogComponent } from './catalog-item-history-dialog.component';

@Component({
  selector: 'app-catalog-items-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
    FieldSearchComponent,
    CatalogGroupsTreeComponent
  ],
  templateUrl: './catalog-items-list.component.html',
  styleUrls: ['./catalog-items-list.component.css']
})
export class CatalogItemsListComponent implements OnInit {
  type: CatalogCrudType = 'PRODUCTS';
  title = 'Produtos';
  singular = 'produto';

  loadingContext = false;
  loading = false;
  contextWarning = '';
  context: CatalogItemContext | null = null;

  items: CatalogItem[] = [];
  displayedColumns = ['codigo', 'nome', 'unidade', 'descricao', 'grupo', 'ativo', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 30;
  hasMoreRows = true;
  loadingMoreRows = false;

  selectedGroupId: number | null = null;
  includeChildrenFilter = true;
  groupOptions: Array<{ id: number; nome: string }> = [];

  isMobile = false;
  groupTreeOpen = false;
  mobileFiltersOpen = false;
  movementAreaOpen = false;
  dropTargetGroupId: number | null = null;
  dropZoneActive = false;
  dropZoneBusy = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Codigo' },
    { key: 'nome', label: 'Nome' },
    { key: 'descricao', label: 'Descricao' }
  ];
  searchTerm = '';
  searchFields = ['codigo', 'nome', 'descricao'];

  filters = this.fb.group({
    status: ['']
  });
  @ViewChild('itemsPane') itemsPane?: ElementRef<HTMLElement>;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private itemService: CatalogItemService,
    private groupService: CatalogGroupService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.route.data.subscribe(data => {
      this.type = (data['type'] || 'PRODUCTS') as CatalogCrudType;
      this.title = data['title'] || (this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos');
      this.singular = data['singular'] || (this.type === 'PRODUCTS' ? 'produto' : 'servico');
      this.pageIndex = 0;
      this.selectedGroupId = null;
      this.includeChildrenFilter = true;
      this.refreshContextAndData();
    });
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  @HostListener('window:empresa-context-updated')
  onEmpresaContextUpdated(): void {
    this.selectedGroupId = null;
    this.refreshContextAndData();
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
      this.loadItems(false);
    }
  }

  refreshContextAndData(): void {
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      this.items = [];
      this.totalElements = 0;
      this.pageIndex = 0;
      this.hasMoreRows = false;
      this.loadingMoreRows = false;
      this.groupOptions = [];
      return;
    }

    this.loadingContext = true;
    this.contextWarning = '';
    this.itemService.contextoEmpresa(this.type)
      .pipe(finalize(() => (this.loadingContext = false)))
      .subscribe({
        next: context => {
          this.context = context;
          if (!context.vinculado) {
            this.contextWarning = context.mensagem || 'Empresa sem grupo configurado para este catalogo.';
            this.items = [];
            this.totalElements = 0;
            this.pageIndex = 0;
            this.hasMoreRows = false;
            this.loadingMoreRows = false;
            this.groupOptions = [];
            return;
          }
          this.contextWarning = '';
          this.loadGroups();
          this.loadItems(true);
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
          this.items = [];
          this.totalElements = 0;
          this.pageIndex = 0;
          this.hasMoreRows = false;
          this.loadingMoreRows = false;
          this.groupOptions = [];
        }
      });
  }

  loadGroups(): void {
    this.groupService.tree(this.type).subscribe({
      next: tree => {
        this.groupOptions = this.flatten(tree || []);
        if (this.selectedGroupId && !this.groupOptions.some(group => group.id === this.selectedGroupId)) {
          this.selectedGroupId = null;
        }
      },
      error: () => {
        this.groupOptions = [];
      }
    });
  }

  loadItems(reset = false): void {
    if (!this.context?.vinculado) {
      this.items = [];
      this.totalElements = 0;
      this.pageIndex = 0;
      this.hasMoreRows = false;
      this.loadingMoreRows = false;
      return;
    }
    if (this.loadingMoreRows) {
      return;
    }
    if (!reset && !this.hasMoreRows) {
      return;
    }
    if (reset) {
      this.pageIndex = 0;
      this.hasMoreRows = true;
      this.items = [];
    }

    const term = this.searchTerm.trim();
    const codigo = this.searchFields.includes('codigo') ? this.parseNumber(term) : null;
    const textEnabled = this.searchFields.includes('nome') || this.searchFields.includes('descricao');
    const text = textEnabled ? term : '';
    const status = (this.filters.value.status || '').trim();
    const targetPage = reset ? 0 : this.pageIndex;

    this.loading = reset;
    this.loadingMoreRows = true;
    this.itemService.list(this.type, {
      page: targetPage,
      size: this.pageSize,
      codigo,
      text,
      grupoId: this.selectedGroupId,
      includeChildren: this.includeChildrenFilter,
      ativo: status === '' ? '' : status === 'ativo'
    }).pipe(finalize(() => {
      this.loading = false;
      this.loadingMoreRows = false;
    })).subscribe({
      next: data => {
        const incoming = data?.content || [];
        this.items = reset ? incoming : [...this.items, ...incoming];
        const serverTotal = Number(this.extractTotalElements(data) || 0);
        if (serverTotal > 0) {
          this.totalElements = serverTotal;
          this.hasMoreRows = this.items.length < this.totalElements;
        } else {
          this.totalElements = this.items.length + (incoming.length >= this.pageSize ? 1 : 0);
          this.hasMoreRows = incoming.length >= this.pageSize;
        }
        this.pageIndex = targetPage + 1;
        this.ensureItemsFillViewport();
      },
      error: err => {
        this.items = [];
        this.totalElements = 0;
        this.pageIndex = 0;
        this.hasMoreRows = false;
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar itens do catalogo.');
      }
    });
  }

  applyFilters(): void {
    this.loadItems(true);
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['codigo', 'nome', 'descricao'];
    this.filters.patchValue({ status: '' }, { emitEvent: false });
    this.includeChildrenFilter = true;
    this.loadItems(true);
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applyFilters();
  }

  onGroupSelected(groupId: number | null): void {
    this.selectedGroupId = groupId;
    this.loadItems(true);
    if (this.isMobile) {
      this.groupTreeOpen = false;
      this.mobileFiltersOpen = false;
    }
  }

  onGroupsChanged(): void {
    this.loadGroups();
    this.loadItems(true);
  }

  onIncludeChildrenFilterChange(checked: boolean): void {
    this.includeChildrenFilter = !!checked;
    this.loadItems(true);
  }

  showAllGroups(): void {
    this.onGroupSelected(null);
  }

  toggleMovementArea(): void {
    this.movementAreaOpen = !this.movementAreaOpen;
  }

  onDropTargetGroupChange(rawGroupId: string): void {
    this.dropTargetGroupId = this.parseNumber(rawGroupId);
  }

  dropTargetGroupLabel(): string {
    if (!this.dropTargetGroupId) return 'Nenhum';
    const target = this.groupOptions.find(item => item.id === this.dropTargetGroupId);
    return target?.nome || 'Nao encontrado';
  }

  onDestinationDragOver(event: DragEvent): void {
    if (this.isMobile || this.dropZoneBusy) return;
    event.preventDefault();
    this.dropZoneActive = true;
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDestinationDragLeave(): void {
    this.dropZoneActive = false;
  }

  onDestinationDrop(event: DragEvent): void {
    if (this.isMobile || this.dropZoneBusy) return;
    event.preventDefault();
    this.dropZoneActive = false;
    if (!this.context?.vinculado) return;

    const targetGroupId = this.dropTargetGroupId;
    if (!targetGroupId) {
      this.notify.info('Selecione um grupo de destino antes de soltar.');
      return;
    }

    const itemId = this.readItemIdFromDrag(event);
    if (itemId) {
      this.moveItemToGroup(itemId, targetGroupId);
      return;
    }

    const sourceGroupId = this.readGroupIdFromDrag(event);
    if (sourceGroupId) {
      if (sourceGroupId === targetGroupId) {
        this.notify.info('O grupo de origem ja e o grupo de destino.');
        return;
      }
      this.moveAllFromGroupToGroup(sourceGroupId, targetGroupId);
      return;
    }

    this.notify.info('Arraste um item da lista ou um grupo da arvore.');
  }

  toggleGroupTree(): void {
    this.groupTreeOpen = !this.groupTreeOpen;
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
    if (this.isMobile && !this.mobileFiltersOpen) {
      this.groupTreeOpen = false;
    }
  }

  onItemsScroll(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target) {
      return;
    }
    if (target.scrollTop + target.clientHeight >= target.scrollHeight - 120) {
      this.loadItems(false);
    }
  }

  goNew(): void {
    if (!this.context?.vinculado) return;
    this.router.navigate([`/catalog/${this.routeSegment()}/new`]);
  }

  onItemRowDragStart(event: DragEvent, row: CatalogItem): void {
    const payload = JSON.stringify({ itemId: row.id });
    event.dataTransfer?.setData('application/x-catalog-item', payload);
    event.dataTransfer?.setData('text/plain', String(row.id));
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  view(row: CatalogItem): void {
    this.router.navigate([`/catalog/${this.routeSegment()}/${row.id}`]);
  }

  edit(row: CatalogItem): void {
    this.router.navigate([`/catalog/${this.routeSegment()}/${row.id}/edit`]);
  }

  openHistory(row: CatalogItem): void {
    this.dialog.open(CatalogItemHistoryDialogComponent, {
      width: '1200px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        type: this.type,
        itemId: row.id,
        itemCodigo: row.codigo,
        itemNome: row.nome
      }
    });
  }

  onStatusToggle(row: CatalogItem, nextAtivo: boolean): void {
    const previous = !!row.ativo;
    if (previous === nextAtivo) return;

    const payload = this.toPayload(row, nextAtivo);
    this.itemService.update(this.type, row.id, payload).subscribe({
      next: updated => {
        row.ativo = !!updated.ativo;
        this.notify.success(`${this.titleLabelSingle()} ${updated.codigo} ${row.ativo ? 'ativado' : 'inativado'}.`);
        this.loadItems(true);
      },
      error: err => {
        row.ativo = previous;
        this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar status do item.');
      }
    });
  }

  remove(row: CatalogItem): void {
    if (!confirm(`Excluir ${this.titleLabelSingle()} codigo ${row.codigo}?`)) return;

    this.itemService.delete(this.type, row.id).subscribe({
      next: () => {
        this.notify.success(`${this.titleLabelSingle()} excluido.`);
        this.loadItems(true);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir item do catalogo.')
    });
  }

  titleLabelSingle(): string {
    return this.type === 'PRODUCTS' ? 'Produto' : 'Servico';
  }

  pageTitle(): string {
    return `Cadastro de ${this.title}`;
  }

  empresaHeaderLabel(): string {
    const contextName = (this.context?.empresaNome || '').trim();
    if (contextName) {
      return contextName;
    }
    return (localStorage.getItem('empresaContextNome') || '').trim();
  }

  newButtonLabel(): string {
    return `Novo ${this.singular}`;
  }

  selectedGroupLabel(): string {
    if (!this.selectedGroupId) return 'Todos';
    const selected = this.groupOptions.find(group => group.id === this.selectedGroupId);
    return selected?.nome || 'Todos';
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count++;
    if ((this.filters.value.status || '').trim()) count++;
    return count;
  }

  private moveItemToGroup(itemId: number, groupId: number | null): void {
    if (!this.context?.vinculado) return;
    const row = this.items.find(item => item.id === itemId);
    if (!row) return;
    const currentGroupId = row.catalogGroupId || null;
    if ((groupId || null) === currentGroupId) return;

    const payload = this.toPayload(row, row.ativo);
    payload.catalogGroupId = groupId;

    this.itemService.update(this.type, row.id, payload).subscribe({
      next: updated => {
        row.catalogGroupId = updated.catalogGroupId || null;
        row.catalogGroupNome = updated.catalogGroupNome || null;
        this.loadItems(true);
        this.notify.success(`${this.titleLabelSingle()} ${row.codigo} movido de grupo.`);
      },
      error: err => {
        this.notify.error(err?.error?.detail || 'Nao foi possivel mover item de grupo.');
      }
    });
  }

  private async moveAllFromGroupToGroup(sourceGroupId: number, targetGroupId: number): Promise<void> {
    if (!this.context?.vinculado) return;
    this.dropZoneBusy = true;
    try {
      const items = await this.fetchAllItemsByGroup(sourceGroupId);
      if (items.length === 0) {
        this.notify.info('O grupo de origem nao possui itens para mover.');
        return;
      }

      let moved = 0;
      for (const item of items) {
        const currentGroupId = item.catalogGroupId || null;
        if (currentGroupId === targetGroupId) continue;
        const payload = this.toPayload(item, item.ativo);
        payload.catalogGroupId = targetGroupId;
        await firstValueFrom(this.itemService.update(this.type, item.id, payload));
        moved++;
      }

      if (moved === 0) {
        this.notify.info('Nenhum item precisou ser movido.');
        return;
      }
      this.notify.success(`${moved} item(ns) movido(s) para ${this.dropTargetGroupLabel()}.`);
      this.loadItems(true);
    } catch (err: any) {
      this.notify.error(err?.error?.detail || 'Nao foi possivel mover itens do grupo selecionado.');
    } finally {
      this.dropZoneBusy = false;
    }
  }

  private async fetchAllItemsByGroup(groupId: number): Promise<CatalogItem[]> {
    const size = 200;
    let page = 0;
    const all: CatalogItem[] = [];
    while (true) {
      const response = await firstValueFrom(this.itemService.list(this.type, {
        page,
        size,
        grupoId: groupId,
        ativo: '',
        codigo: null,
        text: ''
      }));
      const entries = response?.content || [];
      all.push(...entries);
      if (entries.length < size) break;
      page++;
    }
    return all;
  }

  private readItemIdFromDrag(event: DragEvent): number | null {
    const rawItem = event.dataTransfer?.getData('application/x-catalog-item') || '';
    if (rawItem) {
      try {
        const parsed = JSON.parse(rawItem);
        const id = Number(parsed?.itemId || 0);
        return Number.isFinite(id) && id > 0 ? id : null;
      } catch {
        return null;
      }
    }
    const rawText = event.dataTransfer?.getData('text/plain') || '';
    if (rawText.startsWith('group:')) return null;
    try {
      const parsed = JSON.parse(rawText);
      const id = Number(parsed?.itemId || 0);
      if (Number.isFinite(id) && id > 0) return id;
    } catch {
      const id = Number(rawText);
      if (Number.isFinite(id) && id > 0) return id;
    }
    return null;
  }

  private readGroupIdFromDrag(event: DragEvent): number | null {
    const rawGroup = event.dataTransfer?.getData('application/x-catalog-group') || '';
    if (rawGroup) {
      try {
        const parsed = JSON.parse(rawGroup);
        const id = Number(parsed?.groupId || 0);
        return Number.isFinite(id) && id > 0 ? id : null;
      } catch {
        return null;
      }
    }
    const rawText = event.dataTransfer?.getData('text/plain') || '';
    if (!rawText.startsWith('group:')) return null;
    const id = Number(rawText.replace('group:', '').trim());
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  private toPayload(row: CatalogItem, ativo: boolean): CatalogItemPayload {
    return {
      codigo: this.context?.numberingMode === 'MANUAL' ? row.codigo : null,
      nome: row.nome,
      descricao: row.descricao || null,
      catalogGroupId: row.catalogGroupId || null,
      tenantUnitId: row.tenantUnitId,
      unidadeAlternativaTenantUnitId: row.unidadeAlternativaTenantUnitId || null,
      fatorConversaoAlternativa: row.fatorConversaoAlternativa ?? null,
      ativo
    };
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

  private extractTotalElements(payload: any): number {
    if (typeof payload?.totalElements === 'number') return payload.totalElements;
    if (typeof payload?.page?.totalElements === 'number') return payload.page.totalElements;
    return (payload?.content || []).length;
  }

  private routeSegment(): string {
    return this.type === 'PRODUCTS' ? 'products' : 'services';
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
    if (this.isMobile) {
      this.movementAreaOpen = false;
      this.dropZoneActive = false;
      this.dropZoneBusy = false;
    }
  }

  private ensureItemsFillViewport(): void {
    setTimeout(() => {
      if (this.loadingMoreRows || !this.hasMoreRows) {
        return;
      }
      const pane = this.itemsPane?.nativeElement;
      if (pane && pane.scrollHeight <= pane.clientHeight + 4) {
        this.loadItems(false);
        return;
      }
      if (this.isMobile) {
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
        const fullHeight = document.documentElement.scrollHeight || 0;
        if (fullHeight <= viewportHeight + 120) {
          this.loadItems(false);
        }
      }
    }, 0);
  }

  private parseNumber(value: unknown): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }
}
