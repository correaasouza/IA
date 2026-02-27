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
import { TenantUnit, UnitsService } from './units.service';

@Component({
  selector: 'app-tenant-units-list',
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
  templateUrl: './tenant-units-list.component.html'
})
export class TenantUnitsListComponent implements OnInit {
  rows: TenantUnit[] = [];
  filteredRows: TenantUnit[] = [];
  loading = false;
  reconciling = false;
  mobileFiltersOpen = false;
  isMobile = false;
  displayedColumns = ['sigla', 'nome', 'oficial', 'fator', 'padrao', 'mirror', 'acoes'];

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

  clearFilters(): void {
    this.filters.patchValue({ text: '' }, { emitEvent: false });
    this.applyFilters();
  }

  activeFiltersCount(): number {
    return (this.filters.value.text || '').trim() ? 1 : 0;
  }

  applyFilters(): void {
    const text = (this.filters.value.text || '').trim().toLowerCase();
    this.filteredRows = this.rows.filter(row => {
      if (!text) return true;
      return row.sigla.toLowerCase().includes(text)
        || row.nome.toLowerCase().includes(text)
        || (row.unidadeOficialCodigo || '').toLowerCase().includes(text)
        || (row.unidadeOficialDescricao || '').toLowerCase().includes(text);
    });
  }

  load(): void {
    const text = (this.filters.value.text || '').trim();
    this.loading = true;
    this.unitsService.listTenantUnits(text)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.applyFilters();
        },
        error: err => {
          this.rows = [];
          this.filteredRows = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar as unidades do locatario.');
        }
      });
  }

  reconcile(): void {
    this.reconciling = true;
    this.unitsService.reconcileTenantUnits()
      .pipe(finalize(() => (this.reconciling = false)))
      .subscribe({
        next: result => {
          this.notify.success(`Reconciliação concluida. ${result.createdMirrors} unidade(s) espelhada(s) criada(s).`);
          this.load();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel executar a reconciliacao.')
      });
  }

  view(row: TenantUnit): void {
    this.router.navigate(['/tenant-units', row.id]);
  }

  edit(row: TenantUnit): void {
    this.router.navigate(['/tenant-units', row.id, 'edit']);
  }

  remove(row: TenantUnit): void {
    if (!confirm(`Excluir a unidade "${row.sigla}"?`)) {
      return;
    }
    this.unitsService.deleteTenantUnit(row.id).subscribe({
      next: () => {
        this.notify.success('Unidade excluida.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a unidade.')
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}
