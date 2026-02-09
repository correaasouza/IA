import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';

import { MetadataService, TipoEntidade, CampoDefinicao } from './metadata.service';

@Component({
  selector: 'app-metadata',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule
  ],
  template: `
    <div class="grid">
      <mat-card class="card">
        <mat-card-title>Tipos de entidade</mat-card-title>
        <mat-card-content>
          <form [formGroup]="tipoForm" (ngSubmit)="createTipo()" class="form">
            <mat-form-field appearance="outline">
              <mat-label>Nome</mat-label>
              <input matInput formControlName="nome" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit">Criar</button>
          </form>
          <table mat-table [dataSource]="tipos" class="table-dense">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>ID</th>
              <td mat-cell *matCellDef="let row">{{ row.id }}</td>
            </ng-container>
            <ng-container matColumnDef="nome">
              <th mat-header-cell *matHeaderCellDef>Nome</th>
              <td mat-cell *matCellDef="let row">
                <button mat-button (click)="selectTipo(row)">{{ row.nome }}</button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="tipoColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: tipoColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Campos do tipo</mat-card-title>
        <mat-card-content>
          <div class="subtitle">Selecionado: {{ tipoSelecionado?.nome || '-' }}</div>
          <form [formGroup]="campoForm" (ngSubmit)="createCampo()" class="form">
            <mat-form-field appearance="outline">
              <mat-label>Nome</mat-label>
              <input matInput formControlName="nome" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Label</mat-label>
              <input matInput formControlName="label" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Tipo</mat-label>
              <mat-select formControlName="tipo">
                <mat-option value="TEXTO">TEXTO</mat-option>
                <mat-option value="NUMERO">NUMERO</mat-option>
                <mat-option value="DATA">DATA</mat-option>
                <mat-option value="BOOLEANO">BOOLEANO</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Tamanho</mat-label>
              <input matInput formControlName="tamanho" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit">Adicionar</button>
          </form>
          <table mat-table [dataSource]="campos" class="table-dense">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>ID</th>
              <td mat-cell *matCellDef="let row">{{ row.id }}</td>
            </ng-container>
            <ng-container matColumnDef="nome">
              <th mat-header-cell *matHeaderCellDef>Nome</th>
              <td mat-cell *matCellDef="let row">{{ row.nome }}</td>
            </ng-container>
            <ng-container matColumnDef="tipo">
              <th mat-header-cell *matHeaderCellDef>Tipo</th>
              <td mat-cell *matCellDef="let row">{{ row.tipo }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="campoColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: campoColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .grid { display: grid; gap: 12px; grid-template-columns: 1fr 1fr; }
      .form { display: grid; gap: 8px; margin-bottom: 8px; }
      .subtitle { color: #4d5660; margin-bottom: 6px; }
      @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
    `
  ]
})
export class MetadataComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  campos: CampoDefinicao[] = [];
  tipoSelecionado: TipoEntidade | null = null;
  tipoColumns = ['id', 'nome'];
  campoColumns = ['id', 'nome', 'tipo'];

  tipoForm = this.fb.group({
    nome: ['', Validators.required]
  });

  campoForm = this.fb.group({
    nome: ['', Validators.required],
    label: [''],
    tipo: ['TEXTO', Validators.required],
    tamanho: ['']
  });

  constructor(private fb: FormBuilder, private service: MetadataService) {}

  ngOnInit(): void {
    this.loadTipos();
  }

  loadTipos() {
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.service.listTipos(tenantId).subscribe({
      next: data => this.tipos = data.content || [],
      error: () => this.tipos = []
    });
  }

  selectTipo(tipo: TipoEntidade) {
    this.tipoSelecionado = tipo;
    this.loadCampos();
  }

  loadCampos() {
    if (!this.tipoSelecionado) {
      this.campos = [];
      return;
    }
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.service.listCampos(tenantId, this.tipoSelecionado.id).subscribe({
      next: data => this.campos = data.content || [],
      error: () => this.campos = []
    });
  }

  createTipo() {
    if (this.tipoForm.invalid) {
      return;
    }
    this.service.createTipo(this.tipoForm.value.nome!).subscribe({
      next: () => {
        this.tipoForm.reset();
        this.loadTipos();
      }
    });
  }

  createCampo() {
    if (this.campoForm.invalid || !this.tipoSelecionado) {
      return;
    }
    this.service.createCampo({
      tipoEntidadeId: this.tipoSelecionado.id,
      nome: this.campoForm.value.nome,
      label: this.campoForm.value.label,
      tipo: this.campoForm.value.tipo,
      obrigatorio: false,
      tamanho: this.campoForm.value.tamanho ? Number(this.campoForm.value.tamanho) : null
    }).subscribe({
      next: () => {
        this.campoForm.reset({ tipo: 'TEXTO' });
        this.loadCampos();
      }
    });
  }
}
