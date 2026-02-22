import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import {
  CatalogPricingService,
  PriceBook,
  PriceVariant,
  SalePriceApplyByGroupResponse,
  SalePriceBulkItem,
  SalePriceGroupOption,
  SalePriceGridRow
} from './catalog-pricing.service';
import { CatalogConfigurationType } from './catalog-configuration.service';

@Component({
  selector: 'app-sale-price-grid',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    AccessControlDirective
  ],
  templateUrl: './sale-price-grid.component.html',
  styleUrls: ['./sale-price-grid.component.css']
})
export class SalePriceGridComponent implements OnInit, OnDestroy {
  books: PriceBook[] = [];
  variants: PriceVariant[] = [];
  groupOptions: SalePriceGroupOption[] = [];
  rows: SalePriceGridRow[] = [];
  displayedColumns = ['catalogType', 'catalogItemId', 'tenantUnitId', 'priceFinal', 'acoes'];

  loading = false;
  saving = false;
  applyingGroup = false;
  groupOptionsLoading = false;
  isMobile = false;
  mobileFiltersOpen = false;

  filters = this.fb.group({
    priceBookId: [0, Validators.required],
    variantId: [''],
    catalogType: ['' as '' | CatalogConfigurationType]
  });

  createForm = this.fb.group({
    catalogType: ['PRODUCTS' as CatalogConfigurationType, Validators.required],
    catalogItemId: [null as number | null, [Validators.required, Validators.min(1)]],
    priceFinal: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  groupApplyForm = this.fb.group({
    catalogType: ['PRODUCTS' as CatalogConfigurationType, Validators.required],
    catalogGroupId: [0, [Validators.required, Validators.min(1)]],
    percentage: [0, Validators.required],
    includeChildren: [true],
    overwriteExisting: [false]
  });

  private edits = new Map<number, number | null>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly pricingService: CatalogPricingService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.loadReferences();
    this.loadGroupOptions('PRODUCTS');
    this.groupApplyForm.controls.catalogType.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(rawValue => {
        const type = rawValue === 'SERVICES' ? 'SERVICES' : 'PRODUCTS';
        this.groupApplyForm.patchValue({ catalogGroupId: 0 }, { emitEvent: false });
        this.loadGroupOptions(type);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    let count = 0;
    if (Number(this.filters.value.priceBookId || 0) > 0) count += 1;
    if (`${this.filters.value.variantId || ''}`.trim()) count += 1;
    if (`${this.filters.value.catalogType || ''}`.trim()) count += 1;
    return count;
  }

  clearFilters(): void {
    this.filters.patchValue({ variantId: '', catalogType: '' }, { emitEvent: false });
    this.rows = [];
    this.edits.clear();
  }

  loadGrid(): void {
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.notify.error('Selecione a tabela de preco.');
      return;
    }

    const variantId = this.selectedVariantId();
    const catalogType = (this.filters.value.catalogType || '') as '' | CatalogConfigurationType;

    this.loading = true;
    this.pricingService.gridSalePrices(bookId, variantId, catalogType || null, 0, 200)
      .subscribe({
        next: page => {
          this.rows = page?.content || [];
          this.edits.clear();
          this.loading = false;
        },
        error: err => {
          this.rows = [];
          this.edits.clear();
          this.loading = false;
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a grade de precos.');
        }
      });
  }

  setEditedPrice(row: SalePriceGridRow, rawValue: string): void {
    const value = Number(rawValue);
    if (!Number.isFinite(value) || value < 0) {
      this.edits.set(row.id, row.priceFinal);
      return;
    }
    this.edits.set(row.id, value);
  }

  hasPendingChanges(): boolean {
    return this.edits.size > 0 || this.createForm.valid;
  }

  saveChanges(): void {
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.notify.error('Selecione a tabela de preco.');
      return;
    }

    const variantId = this.selectedVariantId();

    const items: SalePriceBulkItem[] = [];
    for (const row of this.rows) {
      if (!this.edits.has(row.id)) continue;
      const edited = this.edits.get(row.id);
      items.push({
        catalogType: row.catalogType,
        catalogItemId: row.catalogItemId,
        tenantUnitId: row.tenantUnitId || null,
        priceFinal: edited == null ? null : Number(edited)
      });
    }

    if (this.createForm.valid) {
      items.push({
        catalogType: this.createForm.value.catalogType as CatalogConfigurationType,
        catalogItemId: Number(this.createForm.value.catalogItemId),
        priceFinal: Number(this.createForm.value.priceFinal),
        tenantUnitId: null
      });
    }

    if (!items.length) {
      return;
    }

    this.saving = true;
    this.pricingService.bulkUpsertSalePrices(bookId, variantId, items)
      .subscribe({
        next: () => {
          this.notify.success('Precos salvos.');
          this.saving = false;
          this.edits.clear();
          this.createForm.reset({
            catalogType: 'PRODUCTS',
            catalogItemId: null,
            priceFinal: null
          }, { emitEvent: false });
          this.loadGrid();
        },
        error: err => {
          this.saving = false;
          this.notify.error(err?.error?.detail || 'Nao foi possivel salvar precos.');
        }
      });
  }

  remove(row: SalePriceGridRow): void {
    if (!row.id) return;
    if (!confirm('Excluir este preco de venda?')) return;
    this.pricingService.deleteSalePrice(row.id).subscribe({
      next: () => {
        this.notify.success('Preco removido.');
        this.loadGrid();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover preco.')
    });
  }

  priceInputValue(row: SalePriceGridRow): number {
    const edited = this.edits.get(row.id);
    return edited == null ? row.priceFinal : edited;
  }

  groupOptionLabel(option: SalePriceGroupOption): string {
    const level = Math.max(0, Number(option.nivel || 0));
    return `${level > 0 ? '› '.repeat(level) : ''}${option.nome}`;
  }

  applyByGroup(): void {
    const bookId = Number(this.filters.value.priceBookId || 0);
    if (!bookId) {
      this.notify.error('Selecione a tabela de preco.');
      return;
    }
    if (this.groupApplyForm.invalid) {
      this.groupApplyForm.markAllAsTouched();
      return;
    }

    const variantId = this.selectedVariantId();
    const payload = {
      priceBookId: bookId,
      variantId,
      catalogType: (this.groupApplyForm.value.catalogType || 'PRODUCTS') as CatalogConfigurationType,
      catalogGroupId: Number(this.groupApplyForm.value.catalogGroupId || 0),
      percentage: Number(this.groupApplyForm.value.percentage || 0),
      includeChildren: !!this.groupApplyForm.value.includeChildren,
      overwriteExisting: !!this.groupApplyForm.value.overwriteExisting
    };

    this.applyingGroup = true;
    this.pricingService.applySalePricesByGroup(payload)
      .pipe(finalize(() => (this.applyingGroup = false)))
      .subscribe({
        next: response => {
          this.notify.success(this.applySummary(response));
          this.loadGrid();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel aplicar percentual por grupo.')
      });
  }

  private loadReferences(): void {
    this.pricingService.listBooks().subscribe({
      next: rows => {
        this.books = rows || [];
        const defaultBook = this.books.find(item => item.defaultBook) || this.books[0];
        this.filters.patchValue({ priceBookId: defaultBook?.id || 0 }, { emitEvent: false });
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel carregar tabelas de preco.')
    });

    this.pricingService.listVariants().subscribe({
      next: rows => {
        this.variants = rows || [];
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel carregar variacoes de preco.')
    });
  }

  private loadGroupOptions(catalogType: CatalogConfigurationType): void {
    this.groupOptionsLoading = true;
    this.pricingService.listSalePriceGroupOptions(catalogType)
      .pipe(finalize(() => (this.groupOptionsLoading = false)))
      .subscribe({
        next: rows => {
          this.groupOptions = rows || [];
          const current = Number(this.groupApplyForm.value.catalogGroupId || 0);
          const exists = this.groupOptions.some(item => item.id === current);
          if (!exists) {
            this.groupApplyForm.patchValue(
              { catalogGroupId: this.groupOptions[0]?.id || 0 },
              { emitEvent: false });
          }
        },
        error: err => {
          this.groupOptions = [];
          this.groupApplyForm.patchValue({ catalogGroupId: 0 }, { emitEvent: false });
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar grupos do catalogo.');
        }
      });
  }

  private selectedVariantId(): number | null {
    const variantRaw = `${this.filters.value.variantId || ''}`.trim();
    return variantRaw ? Number(variantRaw) : null;
  }

  private applySummary(response: SalePriceApplyByGroupResponse): string {
    return `Aplicado no grupo: ${response.processedItems} item(ns) processado(s), ${response.createdItems} criado(s), `
      + `${response.updatedItems} atualizado(s), ${response.skippedWithoutBasePrice} sem Venda Base, `
      + `${response.skippedExisting} existente(s) ignorado(s).`;
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}
