import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { EntityTypeService, TipoEntidade } from './entity-type.service';

@Component({
  selector: 'app-entity-types-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatPaginatorModule,
    MatTableModule,
    MatTooltipModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './entity-types-list.component.html',
  styleUrls: ['./entity-types-list.component.css']
})
export class EntityTypesListComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  displayedColumns = ['nome', 'codigoSeed', 'tipoPadrao', 'ativo', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  loading = false;
  mobileFiltersOpen = false;
  isMobile = false;

  searchOptions: FieldSearchOption[] = [{ key: 'nome', label: 'Nome' }];
  searchTerm = '';
  searchFields = ['nome'];

  filters = this.fb.group({
    status: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: EntityTypeService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  load(): void {
    const params: { page: number; size: number; nome?: string; ativo?: boolean | '' } = {
      page: this.pageIndex,
      size: this.pageSize
    };
    if (this.searchFields.includes('nome') && this.searchTerm.trim()) {
      params.nome = this.searchTerm.trim();
    }
    const status = this.filters.value.status || '';
    if (status === 'ativo') params.ativo = true;
    if (status === 'inativo') params.ativo = false;

    this.loading = true;
    this.service.list(params).pipe(finalize(() => (this.loading = false))).subscribe({
      next: data => {
        this.tipos = data.content || [];
        this.totalElements = data.totalElements || 0;
      },
      error: () => {
        this.tipos = [];
        this.totalElements = 0;
        this.notify.error('Nao foi possivel carregar os tipos de entidade.');
      }
    });
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.pageIndex = 0;
    this.load();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.load();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count++;
    if ((this.filters.value.status || '').trim()) count++;
    return count;
  }

  pageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  remove(row: TipoEntidade): void {
    if (row.tipoPadrao) {
      this.notify.error('Tipos padrao do sistema nao podem ser excluidos.');
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir tipo de entidade',
        message: `Deseja excluir o tipo "${row.nome}"?`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({
        next: () => {
          this.notify.success('Tipo de entidade excluido.');
          this.load();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o tipo de entidade.')
      });
    });
  }

  statusLabel(row: TipoEntidade): string {
    return row.ativo ? 'Ativo' : 'Inativo';
  }

  origemLabel(row: TipoEntidade): string {
    return row.tipoPadrao ? 'Padrao do Sistema' : 'Customizado';
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }
}
