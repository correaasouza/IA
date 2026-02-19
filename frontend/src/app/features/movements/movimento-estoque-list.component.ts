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
  displayedColumns = ['nome', 'movimentoConfig', 'tipoEntidadePadrao', 'acoes'];
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

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['nome'];
  readonly handleListItemConsult = (item: MovimentoEstoqueItemResponse) => this.consultItem(item);
  readonly handleListItemEdit = (item: MovimentoEstoqueItemResponse) => this.openItemOnMovimentoForm(item);
  readonly handleListItemDelete = (item: MovimentoEstoqueItemResponse) => this.removeItem(item);
  @ViewChild('movimentosPane') movimentosPane?: ElementRef<HTMLElement>;
  @ViewChild('itensPane') itensPane?: ElementRef<HTMLElement>;

  constructor(
    private dialog: MatDialog,
    private router: Router,
    private notify: NotificationService,
    private service: MovementOperationService
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

  load(reset = false): void {
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
      this.selectedMovimentoId = null;
      this.visibleItemCount = this.itemChunkSize;
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
        this.showTipoEntidadePadrao = this.rows.some(item => item.tipoEntidadePadraoId != null);
        this.displayedColumns = this.showTipoEntidadePadrao
          ? ['nome', 'movimentoConfig', 'tipoEntidadePadrao', 'acoes']
          : ['nome', 'movimentoConfig', 'acoes'];
        this.ensureMovimentosFillViewport();
      },
      error: err => {
        this.rows = [];
        this.totalElements = 0;
        this.pageIndex = 0;
        this.hasMoreRows = false;
        this.showTipoEntidadePadrao = false;
        this.displayedColumns = ['nome', 'movimentoConfig', 'acoes'];
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

  private openItemOnMovimentoForm(item: MovimentoEstoqueItemResponse): void {
    const selected = this.selectedRow();
    if (!selected) {
      return;
    }
    this.router.navigate(['/movimentos/estoque', selected.id, 'edit'], {
      queryParams: {
        returnTo: '/movimentos/estoque',
        editItemUid: item.id
      }
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
      return;
    }
    if (this.selectedMovimentoId != null && this.rows.some(item => item.id === this.selectedMovimentoId)) {
      return;
    }
    this.selectedMovimentoId = this.rows[0]?.id ?? null;
    if (previousId !== this.selectedMovimentoId) {
      this.visibleItemCount = this.itemChunkSize;
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
}
