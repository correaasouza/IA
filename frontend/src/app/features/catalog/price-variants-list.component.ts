import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { CatalogPricingService, PriceVariant } from './catalog-pricing.service';

@Component({
  selector: 'app-price-variants-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    AccessControlDirective,
    FieldSearchComponent
  ],
  templateUrl: './price-variants-list.component.html'
})
export class PriceVariantsListComponent implements OnInit {
  rows: PriceVariant[] = [];
  filteredRows: PriceVariant[] = [];
  loading = false;
  isMobile = false;
  mobileFiltersOpen = false;
  displayedColumns = ['name', 'status', 'acoes'];

  searchOptions: FieldSearchOption[] = [
    { key: 'name', label: 'Nome' }
  ];
  searchTerm = '';
  searchFields = ['name'];

  filters = this.fb.group({
    active: ['' as '' | 'true' | 'false']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly pricingService: CatalogPricingService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
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
    if ((this.searchTerm || '').trim()) count += 1;
    if ((this.filters.value.active || '').trim()) count += 1;
    return count;
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['name'];
    this.filters.patchValue({ active: '' }, { emitEvent: false });
    this.applyFilters();
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(item => item.key);
    this.applyFilters();
  }

  applyFilters(): void {
    const text = (this.searchTerm || '').trim().toLowerCase();
    const active = this.filters.value.active || '';
    this.filteredRows = this.rows.filter(row => {
      const byText = !text || (this.searchFields.includes('name') && (row.name || '').toLowerCase().includes(text));
      const byStatus = !active || (active === 'true' && row.active) || (active === 'false' && !row.active);
      return byText && byStatus;
    });
  }

  load(): void {
    this.loading = true;
    this.pricingService.listVariants()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.applyFilters();
        },
        error: err => {
          this.rows = [];
          this.filteredRows = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar variacoes de preco.');
        }
      });
  }

  view(row: PriceVariant): void {
    this.router.navigate(['/catalog/pricing/variants', row.id]);
  }

  edit(row: PriceVariant): void {
    this.router.navigate(['/catalog/pricing/variants', row.id, 'edit']);
  }

  remove(row: PriceVariant): void {
    if (!confirm(`Excluir variacao de preco "${row.name}"?`)) {
      return;
    }
    this.pricingService.deleteVariant(row.id).subscribe({
      next: () => {
        this.notify.success('Variacao de preco excluida.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir variacao de preco.')
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}
