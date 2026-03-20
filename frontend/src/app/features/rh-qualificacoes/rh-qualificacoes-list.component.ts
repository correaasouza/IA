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
import { RhQualificacao, RhQualificacoesService } from './rh-qualificacoes.service';

@Component({
  selector: 'app-rh-qualificacoes-list',
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
  templateUrl: './rh-qualificacoes-list.component.html'
})
export class RhQualificacoesListComponent implements OnInit {
  rows: RhQualificacao[] = [];
  filteredRows: RhQualificacao[] = [];
  loading = false;
  mobileFiltersOpen = false;
  isMobile = false;
  displayedColumns = ['nome', 'tipo', 'completo', 'status', 'acoes'];

  filters = this.fb.group({
    text: [''],
    ativo: ['' as '' | 'true' | 'false']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly service: RhQualificacoesService,
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
        || row.nome.toLowerCase().includes(text)
        || (row.tipo || '').toLowerCase().includes(text);
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
    this.service.list({ text, ativo: ativoParam })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.rows = rows || [];
          this.applyFilters();
        },
        error: err => {
          this.rows = [];
          this.filteredRows = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar qualificacoes RH.');
        }
      });
  }

  statusLabel(row: RhQualificacao): string {
    return row.ativo ? 'Ativa' : 'Inativa';
  }

  view(row: RhQualificacao): void {
    this.router.navigate(['/configs/rh-qualificacoes', row.id]);
  }

  edit(row: RhQualificacao): void {
    this.router.navigate(['/configs/rh-qualificacoes', row.id, 'edit']);
  }

  toggleStatus(row: RhQualificacao): void {
    const payload = {
      nome: row.nome,
      completo: !!row.completo,
      tipo: row.tipo || null,
      ativo: !row.ativo
    };
    this.service.update(row.id, payload).subscribe({
      next: () => {
        this.notify.success(!row.ativo ? 'Qualificacao ativada.' : 'Qualificacao desativada.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar status da qualificacao.')
    });
  }

  remove(row: RhQualificacao): void {
    if (!confirm(`Inativar a qualificacao "${row.nome}"?`)) return;
    this.service.delete(row.id).subscribe({
      next: () => {
        this.notify.success('Qualificacao inativada.');
        this.load();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel inativar qualificacao.')
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}

