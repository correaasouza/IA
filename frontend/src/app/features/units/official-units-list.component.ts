import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { OfficialUnit, UnitsService } from './units.service';

@Component({
  selector: 'app-official-units-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './official-units-list.component.html'
})
export class OfficialUnitsListComponent implements OnInit {
  rows: OfficialUnit[] = [];
  filteredRows: OfficialUnit[] = [];
  loading = false;
  mobileFiltersOpen = false;
  isMobile = false;
  displayedColumns = ['codigo', 'descricao', 'origem', 'status', 'acoes'];

  filters = this.fb.group({
    text: [''],
    ativo: ['' as '' | 'true' | 'false']
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
    let count = 0;
    if ((this.filters.value.text || '').trim()) count += 1;
    if ((this.filters.value.ativo || '').trim()) count += 1;
    return count;
  }

  clearFilters(): void {
    this.filters.patchValue({ text: '', ativo: '' }, { emitEvent: false });
    this.applyFilters();
  }

  applyFilters(): void {
    const text = (this.filters.value.text || '').trim().toLowerCase();
    const ativo = this.filters.value.ativo || '';
    this.filteredRows = this.rows.filter(row => {
      const matchesText = !text
        || row.codigoOficial.toLowerCase().includes(text)
        || row.descricao.toLowerCase().includes(text);
      const matchesStatus = !ativo
        || (ativo === 'true' && row.ativo)
        || (ativo === 'false' && !row.ativo);
      return matchesText && matchesStatus;
    });
  }

  load(): void {
    const text = (this.filters.value.text || '').trim() || undefined;
    const ativoParam = this.filters.value.ativo === '' ? '' : this.filters.value.ativo === 'true';
    this.loading = true;
    this.unitsService.listOfficial({ text, ativo: ativoParam })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.applyFilters();
        },
        error: err => {
          this.rows = [];
          this.filteredRows = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar as unidades oficiais.');
        }
      });
  }

  statusLabel(row: OfficialUnit): string {
    return row.ativo ? 'Ativa' : 'Inativa';
  }

  view(row: OfficialUnit): void {
    this.router.navigate(['/global-settings/official-units', row.id]);
  }

  edit(row: OfficialUnit): void {
    this.router.navigate(['/global-settings/official-units', row.id, 'edit']);
  }

  toggleStatus(row: OfficialUnit): void {
    const payload = {
      codigoOficial: row.codigoOficial,
      descricao: row.descricao,
      ativo: !row.ativo,
      origem: row.origem
    };
    this.unitsService.updateOfficial(row.id, payload).subscribe({
      next: () => {
        this.notify.success(!row.ativo ? 'Unidade oficial ativada.' : 'Unidade oficial desativada.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o status da unidade oficial.')
    });
  }

  remove(row: OfficialUnit): void {
    if (!confirm(`Excluir a unidade oficial "${row.codigoOficial}"?`)) {
      return;
    }
    this.unitsService.deleteOfficial(row.id).subscribe({
      next: () => {
        this.notify.success('Unidade oficial excluida.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a unidade oficial.')
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}

