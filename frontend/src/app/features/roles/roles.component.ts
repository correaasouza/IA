import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatListModule, MatListOption } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';

import { RolesService, Papel, PermissaoCatalog } from './roles.service';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatListModule,
    MatChipsModule
  ],
  template: `
    <div class="grid">
      <mat-card class="card">
        <mat-card-title>Novo papel</mat-card-title>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="create()" class="form">
            <mat-form-field appearance="outline">
              <mat-label>Nome</mat-label>
              <input matInput formControlName="nome" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Descrição</mat-label>
              <input matInput formControlName="descricao" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit">Criar</button>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Papéis</mat-card-title>
        <mat-card-content>
          <table mat-table [dataSource]="papeis" class="table-dense">
            <ng-container matColumnDef="nome">
              <th mat-header-cell *matHeaderCellDef>Nome</th>
              <td mat-cell *matCellDef="let row">
                <button mat-button (click)="select(row)">{{ row.nome }}</button>
              </td>
            </ng-container>
            <ng-container matColumnDef="descricao">
              <th mat-header-cell *matHeaderCellDef>Descrição</th>
              <td mat-cell *matCellDef="let row">{{ row.descricao }}</td>
            </ng-container>
            <ng-container matColumnDef="ativo">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let row">
                <mat-chip>{{ row.ativo ? 'Ativo' : 'Inativo' }}</mat-chip>
              </td>
            </ng-container>
            <ng-container matColumnDef="acoes">
              <th mat-header-cell *matHeaderCellDef>Ações</th>
              <td mat-cell *matCellDef="let row">
                <button mat-stroked-button (click)="edit(row)">Editar</button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Permissões (catálogo)</mat-card-title>
        <mat-card-content>
          <form [formGroup]="permForm" (ngSubmit)="createPerm()" class="form">
            <mat-form-field appearance="outline">
              <mat-label>Código</mat-label>
              <input matInput formControlName="codigo" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Label</mat-label>
              <input matInput formControlName="label" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit">Adicionar</button>
          </form>
          <table mat-table [dataSource]="catalog" class="table-dense">
            <ng-container matColumnDef="codigo">
              <th mat-header-cell *matHeaderCellDef>Código</th>
              <td mat-cell *matCellDef="let row">{{ row.codigo }}</td>
            </ng-container>
            <ng-container matColumnDef="label">
              <th mat-header-cell *matHeaderCellDef>Label</th>
              <td mat-cell *matCellDef="let row">{{ row.label }}</td>
            </ng-container>
            <ng-container matColumnDef="acoes">
              <th mat-header-cell *matHeaderCellDef>Ações</th>
              <td mat-cell *matCellDef="let row">
                <button mat-stroked-button (click)="editPerm(row)">Editar</button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="permColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: permColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Permissões do papel</mat-card-title>
        <mat-card-content>
          <div *ngIf="!selected">Selecione um papel para editar permissões.</div>
          <div *ngIf="selected">
            <div class="selected">
              <div class="name">{{ selected.nome }}</div>
              <div class="desc">{{ selected.descricao }}</div>
            </div>
            <mat-selection-list #permList (selectionChange)="onPermChange(permList.selectedOptions.selected)">
              <mat-list-option *ngFor="let p of catalog" [value]="p.codigo" [selected]="selectedPerms.has(p.codigo)">
                {{ p.codigo }} — {{ p.label }}
              </mat-list-option>
            </mat-selection-list>
            <div class="actions">
              <button mat-flat-button color="primary" (click)="savePerms()">Salvar permissões</button>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .grid { display: grid; gap: 12px; grid-template-columns: 320px 1fr; }
      .form { display: grid; gap: 8px; }
      .selected { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
      .selected .name { font-weight: 600; }
      .selected .desc { color: var(--muted); font-size: 12px; }
      .actions { margin-top: 8px; }
      @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
    `
  ]
})
export class RolesComponent implements OnInit {
  papeis: Papel[] = [];
  catalog: PermissaoCatalog[] = [];
  selected: Papel | null = null;
  selectedPerms = new Set<string>();
  displayedColumns = ['nome', 'descricao', 'ativo', 'acoes'];
  permColumns = ['codigo', 'label', 'acoes'];

  form = this.fb.group({
    nome: ['', Validators.required],
    descricao: ['']
  });

  permForm = this.fb.group({
    codigo: ['', Validators.required],
    label: ['', Validators.required]
  });

  constructor(private fb: FormBuilder, private service: RolesService) {}

  ngOnInit(): void {
    this.load();
    this.service.listCatalog().subscribe({ next: data => this.catalog = data || [] });
  }

  load() {
    this.service.list().subscribe({
      next: data => this.papeis = data || []
    });
  }

  create() {
    if (this.form.invalid) return;
    this.service.create({
      nome: this.form.value.nome!,
      descricao: this.form.value.descricao || undefined,
      ativo: true
    }).subscribe({
      next: () => {
        this.form.reset();
        this.load();
      }
    });
  }

  edit(row: Papel) {
    const nome = prompt('Nome do papel', row.nome);
    if (!nome) return;
    const descricao = prompt('Descrição', row.descricao || '') || '';
    this.service.update(row.id, { nome, descricao, ativo: row.ativo }).subscribe({
      next: () => this.load()
    });
  }

  select(row: Papel) {
    this.selected = row;
    this.selectedPerms = new Set<string>();
    this.service.listPermissoes(row.id).subscribe({
      next: data => this.selectedPerms = new Set(data || [])
    });
  }

  onPermChange(options: MatListOption[]) {
    this.selectedPerms = new Set(options.map(o => o.value as string));
  }

  savePerms() {
    if (!this.selected) return;
    this.service.setPermissoes(this.selected.id, Array.from(this.selectedPerms)).subscribe();
  }

  createPerm() {
    if (this.permForm.invalid) return;
    this.service.createPerm({
      codigo: this.permForm.value.codigo!,
      label: this.permForm.value.label!,
      ativo: true
    }).subscribe({
      next: () => {
        this.permForm.reset();
        this.service.listCatalog().subscribe({ next: data => this.catalog = data || [] });
      }
    });
  }

  editPerm(row: PermissaoCatalog) {
    const label = prompt('Label', row.label);
    if (!label) return;
    this.service.updatePerm(row.id, row.codigo, label).subscribe({
      next: () => this.service.listCatalog().subscribe({ next: data => this.catalog = data || [] })
    });
  }
}
