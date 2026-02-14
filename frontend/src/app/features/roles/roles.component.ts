import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { RolesService, Papel } from './roles.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
    MatTooltipModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './roles.component.html',
  styleUrls: ['./roles.component.css']
})
export class RolesComponent implements OnInit {
  papeis: Papel[] = [];
  filteredPapeis: Papel[] = [];
  displayedColumns = ['nome', 'descricao', 'ativo', 'acoes'];
  rolesLoading = false;
  togglingRoleId: number | null = null;

  roleSearchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' },
    { key: 'descricao', label: 'Descrição' }
  ];
  roleSearchTerm = '';
  roleSearchFields = ['nome', 'descricao'];
  mobileRoleFiltersOpen = false;
  isMobile = false;

  constructor(
    private service: RolesService,
    private notify: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
  }

  @HostListener('window:resize')
  onWindowResize() {
    this.updateViewportMode();
  }

  load() {
    this.rolesLoading = true;
    this.service.list().pipe(finalize(() => (this.rolesLoading = false))).subscribe({
      next: data => {
        this.papeis = data || [];
        this.applyRoleSearch();
      },
      error: () => {
        this.papeis = [];
        this.filteredPapeis = [];
        this.notify.error('Não foi possível carregar os papéis.');
      }
    });
  }

  onRoleSearchChange(value: FieldSearchValue) {
    this.roleSearchTerm = value.term;
    this.roleSearchFields = value.fields.length ? value.fields : this.roleSearchOptions.map(o => o.key);
    this.applyRoleSearch();
  }

  toggleMobileRoleFilters() {
    this.mobileRoleFiltersOpen = !this.mobileRoleFiltersOpen;
  }

  activeRoleFiltersCount(): number {
    return (this.roleSearchTerm || '').trim() ? 1 : 0;
  }

  statusLabel(row: Papel) {
    return row.ativo ? 'Ativo' : 'Inativo';
  }

  viewRole(row: Papel) {
    this.router.navigate(['/roles', row.id]);
  }

  openEditRole(row: Papel) {
    this.router.navigate(['/roles', row.id, 'edit']);
  }

  removeRole(row: Papel) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir papel', message: `Deseja excluir o papel "${row.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({
        next: () => {
          this.notify.success('Papel excluído.');
          this.load();
        },
        error: () => this.notify.error('Não foi possível excluir o papel.')
      });
    });
  }

  toggleStatus(row: Papel) {
    const nextStatus = !row.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: nextStatus ? 'Ativar papel' : 'Desativar papel',
        message: nextStatus
          ? `Deseja ativar o papel "${row.nome}"?`
          : `Deseja desativar o papel "${row.nome}"?`,
        confirmText: nextStatus ? 'Ativar' : 'Desativar',
        confirmColor: nextStatus ? 'primary' : 'warn',
        confirmAriaLabel: `${nextStatus ? 'Ativar' : 'Desativar'} papel`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.togglingRoleId = row.id;
      this.service.update(row.id, {
        nome: row.nome,
        descricao: row.descricao || undefined,
        ativo: nextStatus
      }).pipe(finalize(() => (this.togglingRoleId = null))).subscribe({
        next: () => {
          row.ativo = nextStatus;
          this.notify.success(nextStatus ? 'Papel ativado.' : 'Papel desativado.');
        },
        error: () => this.notify.error('Não foi possível atualizar o status do papel.')
      });
    });
  }

  private applyRoleSearch() {
    const term = this.roleSearchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredPapeis = [...this.papeis];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredPapeis = this.papeis.filter(p => {
      const matchNome = this.roleSearchFields.includes('nome') && match(p.nome);
      const matchDescricao = this.roleSearchFields.includes('descricao') && match(p.descricao || '');
      return matchNome || matchDescricao;
    });
  }

  private updateViewportMode() {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }
}

