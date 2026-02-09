import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { UsuarioPapeisDialogComponent } from './usuario-papeis-dialog.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule
  ],
  template: `
    <mat-card class="card">
      <mat-card-title>Usuários</mat-card-title>
      <mat-card-content>
        <div class="toolbar">
          <button mat-flat-button color="primary" routerLink="/users/new">
            <mat-icon>add</mat-icon> Novo usuário
          </button>
        </div>

        <table mat-table [dataSource]="usuarios" class="table-dense">
          <ng-container matColumnDef="username">
            <th mat-header-cell *matHeaderCellDef>Username</th>
            <td mat-cell *matCellDef="let row">{{ row.username }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let row">{{ row.email }}</td>
          </ng-container>
          <ng-container matColumnDef="ativo">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let row">
              <mat-chip>{{ row.ativo ? 'Ativo' : 'Inativo' }}</mat-chip>
            </td>
          </ng-container>
          <ng-container matColumnDef="papeis">
            <th mat-header-cell *matHeaderCellDef>Papéis</th>
            <td mat-cell *matCellDef="let row">
              <mat-chip-listbox>
                <mat-chip *ngFor="let p of (row.papeis || [])">{{ p }}</mat-chip>
              </mat-chip-listbox>
            </td>
          </ng-container>
          <ng-container matColumnDef="acoes">
            <th mat-header-cell *matHeaderCellDef>Ações</th>
            <td mat-cell *matCellDef="let row">
              <button mat-stroked-button (click)="view(row)">
                <mat-icon>visibility</mat-icon> Consultar
              </button>
              <button mat-stroked-button (click)="edit(row)">
                <mat-icon>edit</mat-icon> Editar
              </button>
              <button mat-stroked-button (click)="editPapeis(row)">
                <mat-icon>security</mat-icon> Papéis
              </button>
              <button mat-button color="warn" (click)="remove(row)">
                <mat-icon>delete</mat-icon> Excluir
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
      .mat-mdc-button .mat-icon,
      .mat-mdc-outlined-button .mat-icon { margin-right: 4px; }
    `
  ]
})
export class UsersListComponent implements OnInit {
  usuarios: UsuarioResponse[] = [];
  displayedColumns = ['username', 'email', 'ativo', 'papeis', 'acoes'];

  constructor(private service: UsuarioService, private dialog: MatDialog, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.service.list(0, 50).subscribe({
      next: data => this.usuarios = data.content || [],
      error: () => this.usuarios = []
    });
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
      this.service.delete(row.id).subscribe({ next: () => this.load() });
    });
  }

  editPapeis(row: UsuarioResponse) {
    const ref = this.dialog.open(UsuarioPapeisDialogComponent, {
      data: { userId: row.id, username: row.username }
    });
    ref.afterClosed().subscribe();
  }
}
