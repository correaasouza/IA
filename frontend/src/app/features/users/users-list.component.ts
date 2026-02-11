import { Component, OnInit } from '@angular/core';
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
    FieldSearchComponent
  ],
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {
  usuarios: UsuarioResponse[] = [];
  filteredUsuarios: UsuarioResponse[] = [];
  displayedColumns = ['username', 'email', 'ativo', 'papeis', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  loading = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'username', label: 'Username' },
    { key: 'email', label: 'Email' },
    { key: 'papeis', label: 'Papéis' }
  ];
  searchTerm = '';
  searchFields = ['username', 'email'];

  constructor(
    private service: UsuarioService,
    private dialog: MatDialog,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
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

  statusClass(row: UsuarioResponse) {
    return row.ativo ? '' : 'off';
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
      data: { userId: row.id, username: row.username }
    });
    ref.afterClosed().subscribe();
  }
}


