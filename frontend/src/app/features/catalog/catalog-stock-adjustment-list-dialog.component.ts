import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import {
  CatalogConfigurationService,
  CatalogConfigurationType,
  CatalogStockAdjustment
} from './catalog-configuration.service';
import {
  CatalogStockAdjustmentFormDialogComponent,
  CatalogStockAdjustmentFormDialogData
} from './catalog-stock-adjustment-form-dialog.component';

export interface CatalogStockAdjustmentListDialogData {
  type: CatalogConfigurationType;
}

@Component({
  selector: 'app-catalog-stock-adjustment-list-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './catalog-stock-adjustment-list-dialog.component.html',
  styleUrls: ['./catalog-stock-adjustment-list-dialog.component.css']
})
export class CatalogStockAdjustmentListDialogComponent {
  loading = false;
  deleting = false;
  error = '';
  searchTerm = '';
  statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';
  rows: CatalogStockAdjustment[] = [];
  pageIndex = 0;
  pageSize = 10;
  pageSizeOptions = [10, 20, 50];
  private hasChanges = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: CatalogStockAdjustmentListDialogData,
    private dialogRef: MatDialogRef<CatalogStockAdjustmentListDialogComponent>,
    private dialog: MatDialog,
    private service: CatalogConfigurationService,
    private notify: NotificationService
  ) {
    this.load();
  }

  close(changed = false): void {
    this.dialogRef.close(changed || this.hasChanges);
  }

  title(): string {
    return `Tipos de ajuste de estoque - ${this.data.type === 'PRODUCTS' ? 'Produtos' : 'Servicos'}`;
  }

  filteredRows(): CatalogStockAdjustment[] {
    const term = (this.searchTerm || '').trim().toLowerCase();
    let filtered = [...(this.rows || [])];
    if (this.statusFilter === 'ACTIVE') filtered = filtered.filter(item => item.active);
    if (this.statusFilter === 'INACTIVE') filtered = filtered.filter(item => !item.active);
    if (term) {
      filtered = filtered.filter(item =>
        (item.codigo || '').toLowerCase().includes(term)
        || (item.nome || '').toLowerCase().includes(term));
    }
    filtered.sort((a, b) => {
      const ao = Number(a.ordem || 0);
      const bo = Number(b.ordem || 0);
      if (ao !== bo) return ao - bo;
      return (a.nome || '').localeCompare(b.nome || '', 'pt-BR');
    });
    return filtered;
  }

  pagedRows(rows: CatalogStockAdjustment[]): CatalogStockAdjustment[] {
    const start = this.pageIndex * this.pageSize;
    const end = start + this.pageSize;
    return rows.slice(start, end);
  }

  onFilterChanged(): void {
    this.pageIndex = 0;
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.ensurePageBounds(this.filteredRows().length);
  }

  openCreate(): void {
    this.openFormDialog({ type: this.data.type, adjustment: null });
  }

  openEdit(row: CatalogStockAdjustment): void {
    this.openFormDialog({ type: this.data.type, adjustment: row });
  }

  remove(row: CatalogStockAdjustment): void {
    if (!row?.id || this.deleting) return;
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
      this.performRemove(row.id);
    });
  }

  typeLabel(tipo: string): string {
    if (tipo === 'ENTRADA') return 'Entrada';
    if (tipo === 'SAIDA') return 'Saida';
    if (tipo === 'TRANSFERENCIA') return 'Transferencia';
    return tipo || '-';
  }

  private load(): void {
    this.loading = true;
    this.error = '';
    this.service.listStockAdjustmentsByType(this.data.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.ensurePageBounds(this.filteredRows().length);
        },
        error: err => {
          this.rows = [];
          this.error = err?.error?.detail || 'Nao foi possivel carregar tipos de ajuste.';
        }
      });
  }

  private openFormDialog(data: CatalogStockAdjustmentFormDialogData): void {
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
      this.hasChanges = true;
      this.load();
    });
  }

  private performRemove(id: number): void {
    this.deleting = true;
    this.service.deleteStockAdjustmentByType(this.data.type, id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Tipo de ajuste removido.');
          this.hasChanges = true;
          this.load();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel remover tipo de ajuste.');
        }
      });
  }

  private ensurePageBounds(totalRows: number): void {
    if (totalRows <= 0) {
      this.pageIndex = 0;
      return;
    }
    const maxPage = Math.floor((totalRows - 1) / this.pageSize);
    if (this.pageIndex > maxPage) {
      this.pageIndex = maxPage;
    }
  }
}
