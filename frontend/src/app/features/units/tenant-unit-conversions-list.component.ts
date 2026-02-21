import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { TenantUnitConversion, UnitsService } from './units.service';

@Component({
  selector: 'app-tenant-unit-conversions-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    MatTooltipModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './tenant-unit-conversions-list.component.html'
})
export class TenantUnitConversionsListComponent implements OnInit {
  rows: TenantUnitConversion[] = [];
  filteredRows: TenantUnitConversion[] = [];
  loading = false;
  mobileFiltersOpen = false;
  isMobile = false;
  displayedColumns = ['origem', 'destino', 'fator', 'acoes'];

  filters = this.fb.group({
    text: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly unitsService: UnitsService,
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
    return (this.filters.value.text || '').trim() ? 1 : 0;
  }

  clearFilters(): void {
    this.filters.patchValue({ text: '' }, { emitEvent: false });
    this.applyFilters();
  }

  applyFilters(): void {
    const text = (this.filters.value.text || '').trim().toLowerCase();
    this.filteredRows = this.rows.filter(row => {
      if (!text) return true;
      return (row.unidadeOrigemSigla || '').toLowerCase().includes(text)
        || (row.unidadeDestinoSigla || '').toLowerCase().includes(text);
    });
  }

  load(): void {
    this.loading = true;
    this.unitsService.listTenantUnitConversions()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.applyFilters();
        },
        error: err => {
          this.rows = [];
          this.filteredRows = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar as conversoes.');
        }
      });
  }

  view(row: TenantUnitConversion): void {
    this.router.navigate(['/tenant-unit-conversions', row.id]);
  }

  edit(row: TenantUnitConversion): void {
    this.router.navigate(['/tenant-unit-conversions', row.id, 'edit']);
  }

  remove(row: TenantUnitConversion): void {
    if (!confirm(`Excluir a conversao ${row.unidadeOrigemSigla} -> ${row.unidadeDestinoSigla}?`)) {
      return;
    }
    this.unitsService.deleteTenantUnitConversion(row.id).subscribe({
      next: () => {
        this.notify.success('Conversao excluida.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a conversao.')
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}

