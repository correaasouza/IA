import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { UsuarioPapeisDialogComponent } from './usuario-papeis-dialog.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { AccessControlDirective } from '../../shared/access-control.directive';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule,
    MatPaginatorModule,
    MatTooltipModule,
    MatMenuModule,
    InlineLoaderComponent,
    FieldSearchComponent,
    AccessControlDirective
  ],
  templateUrl: './users-list.component.html',
})
export class UsersListComponent implements OnInit {
  usuarios: UsuarioResponse[] = [];
  filteredUsuarios: UsuarioResponse[] = [];
  displayedColumns = ['username', 'email', 'ativo', 'papeis', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  loading = false;
  togglingUserId: number | null = null;

  searchOptions: FieldSearchOption[] = [
    { key: 'username', label: 'Username' },
    { key: 'email', label: 'Email' },
    { key: 'papeis', label: 'Papéis' }
  ];
  searchTerm = '';
  searchFields = ['username', 'email'];
  mobileFiltersOpen = false;
  isMobile = false;

  constructor(
    private service: UsuarioService,
    private dialog: MatDialog,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.load();
  }

  load() {
    this.loading = true;
    this.service.list(this.pageIndex, this.pageSize).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.usuarios = data.content || [];
        this.totalElements = data.totalElements || 0;
        this.applySearch();
      },
      error: () => {
        this.usuarios = [];
        this.filteredUsuarios = [];
        this.totalElements = 0;
        this.notify.error('Não foi possível carregar os usuários.');
      }
    });
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  clearFilters() {
    this.searchTerm = '';
    this.searchFields = ['username', 'email'];
    this.applySearch();
  }

  
  @HostListener('window:resize')
  onWindowResize() {
    this.updateViewportMode();
  }

  toggleMobileFilters() {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    return (this.searchTerm || '').trim() ? 1 : 0;
  }

  private updateViewportMode() {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredUsuarios = [...this.usuarios];
      return;
    }
    const has = (value?: string) => (value || '').toLowerCase().includes(term);
    this.filteredUsuarios = this.usuarios.filter(u => {
      const matchUsername = this.searchFields.includes('username') && has(u.username);
      const matchEmail = this.searchFields.includes('email') && has(u.email || '');
      const matchPapeis = this.searchFields.includes('papeis') && has((u.papeis || []).join(' '));
      return matchUsername || matchEmail || matchPapeis;
    });
  }

  statusLabel(row: UsuarioResponse) {
    return row.ativo ? 'Ativo' : 'Inativo';
  }

  view(row: UsuarioResponse) {
    this.router.navigate(['/users', row.id]);
  }

  edit(row: UsuarioResponse) {
    this.router.navigate(['/users', row.id, 'edit']);
  }

  remove(row: UsuarioResponse) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir usuário', message: `Deseja excluir o usuário "${row.username}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({
        next: () => {
          this.notify.success('Usuário removido.');
          this.load();
        },
        error: () => this.notify.error('Não foi possível remover o usuário.')
      });
    });
  }

  editPapeis(row: UsuarioResponse) {
    const ref = this.dialog.open(UsuarioPapeisDialogComponent, {
      data: { userId: row.id, username: row.username },
      width: '420px',
      maxWidth: '92vw'
    });
    ref.afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }

  toggleStatus(row: UsuarioResponse) {
    const nextStatus = !row.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: nextStatus ? 'Ativar usuário' : 'Desativar usuário',
        message: nextStatus
          ? `Deseja ativar o usuário "${row.username}"?`
          : `Deseja desativar o usuário "${row.username}"?`,
        confirmText: nextStatus ? 'Ativar' : 'Desativar',
        confirmColor: nextStatus ? 'primary' : 'warn',
        confirmAriaLabel: `${nextStatus ? 'Ativar' : 'Desativar'} usuário`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.togglingUserId = row.id;
      this.service.update(row.id, {
        username: row.username,
        email: row.email || undefined,
        ativo: nextStatus
      }).pipe(finalize(() => (this.togglingUserId = null))).subscribe({
        next: () => {
          row.ativo = nextStatus;
          this.notify.success(nextStatus ? 'Usuário ativado.' : 'Usuário desativado.');
        },
        error: () => this.notify.error('Não foi possível atualizar o status do usuário.')
      });
    });
  }
}





