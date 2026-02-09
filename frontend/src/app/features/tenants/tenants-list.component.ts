import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router, RouterLink } from '@angular/router';

import { TenantService, LocatarioResponse } from './tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-tenants-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatDialogModule
  ],
  template: `
    <mat-card class="card">
      <mat-card-title>Locatários</mat-card-title>
      <mat-card-content>
        <div class="toolbar">
          <button mat-flat-button color="primary" routerLink="/tenants/new">
            <mat-icon>add</mat-icon> Novo locatário
          </button>
        </div>

        <form [formGroup]="filters" class="filters">
          <mat-form-field appearance="outline">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Todos</mat-option>
              <mat-option value="ativo">Ativo</mat-option>
              <mat-option value="bloqueado">Bloqueado</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-stroked-button type="button" (click)="applyFilters()">Filtrar</button>
        </form>

        <table mat-table [dataSource]="locatarios" matSort (matSortChange)="sortChange($event)" class="table-dense">
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>ID</th>
            <td mat-cell *matCellDef="let row">{{ row.id }}</td>
          </ng-container>
          <ng-container matColumnDef="nome">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Nome</th>
            <td mat-cell *matCellDef="let row">{{ row.nome }}</td>
          </ng-container>
          <ng-container matColumnDef="dataLimite">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Vencimento</th>
            <td mat-cell *matCellDef="let row">{{ row.dataLimiteAcesso | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let row">
              <mat-chip>{{ row.ativo ? (row.bloqueado ? 'Bloqueado' : 'Ativo') : 'Inativo' }}</mat-chip>
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
              <button mat-button color="warn" (click)="remove(row)">
                <mat-icon>delete</mat-icon> Excluir
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <mat-paginator [length]="totalElements" [pageSize]="pageSize" [pageIndex]="pageIndex"
          [pageSizeOptions]="[10, 25, 50]" (page)="pageChange($event)">
        </mat-paginator>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .filters { display: grid; gap: 8px; grid-template-columns: 1fr 160px 120px; align-items: end; margin-bottom: 8px; }
      .toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
      .mat-mdc-button .mat-icon,
      .mat-mdc-outlined-button .mat-icon { margin-right: 4px; }
      @media (max-width: 900px) { .filters { grid-template-columns: 1fr; } }
    `
  ]
})
export class TenantsListComponent implements OnInit {
  locatarios: LocatarioResponse[] = [];
  displayedColumns = ['id', 'nome', 'dataLimite', 'status', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  sort = 'id,asc';

  filters = this.fb.group({
    nome: [''],
    status: ['']
  });

  constructor(private fb: FormBuilder, private service: TenantService, private router: Router, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    const status = this.filters.value.status || '';
    const bloqueado = status === 'bloqueado' ? 'true' : '';
    const ativo = status === 'ativo' ? 'true' : '';

    this.service.list({
      page: this.pageIndex,
      size: this.pageSize,
      sort: this.sort,
      nome: this.filters.value.nome || '',
      bloqueado,
      ativo
    }).subscribe({
      next: data => {
        this.locatarios = data.content || [];
        this.totalElements = data.totalElements || 0;
      },
      error: () => {
        this.locatarios = [];
        this.totalElements = 0;
      }
    });
  }

  applyFilters() {
    this.pageIndex = 0;
    this.load();
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  sortChange(sort: Sort) {
    this.sort = sort.direction ? `${sort.active},${sort.direction}` : 'id,asc';
    this.load();
  }

  view(row: LocatarioResponse) {
    this.router.navigate(['/tenants', row.id]);
  }

  edit(row: LocatarioResponse) {
    this.router.navigate(['/tenants', row.id, 'edit']);
  }

  remove(row: LocatarioResponse) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir locatário', message: `Deseja excluir o locatário "${row.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({ next: () => this.load() });
    });
  }
}
