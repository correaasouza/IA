import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DateMaskDirective } from '../../shared/date-mask.directive';

import { TenantService, LocatarioResponse } from './tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    DateMaskDirective,
    MatIconModule,
    MatDialogModule
  ],
  template: `
    <mat-card class="card">
      <mat-card-title>{{ title }}</mat-card-title>
      <mat-card-content>
        <form [formGroup]="form" class="form">
          <mat-form-field appearance="outline">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Data limite</mat-label>
            <input matInput formControlName="dataLimiteAcesso" placeholder="DD-MM-YYYY" appDateMask />
          </mat-form-field>
          <mat-slide-toggle formControlName="ativo">Ativo</mat-slide-toggle>
          <div class="status" *ngIf="locatario">
            Status atual: <strong>{{ locatario.ativo ? (locatario.bloqueado ? 'Bloqueado' : 'Ativo') : 'Inativo' }}</strong>
          </div>
          <div class="actions">
            <button mat-stroked-button type="button" (click)="back()">
              <mat-icon>arrow_back</mat-icon> Voltar
            </button>
            <button mat-button color="warn" *ngIf="mode !== 'new'" type="button" (click)="remove()">
              <mat-icon>delete</mat-icon> Excluir
            </button>
            <button mat-flat-button color="primary" *ngIf="mode !== 'view'" type="button" (click)="save()">
              <mat-icon>save</mat-icon> Salvar
            </button>
            <button mat-stroked-button *ngIf="mode === 'view'" type="button" (click)="toEdit()">
              <mat-icon>edit</mat-icon> Editar
            </button>
          </div>
          <div class="actions" *ngIf="mode !== 'new'">
            <button mat-stroked-button type="button" (click)="renew()">
              <mat-icon>event_repeat</mat-icon> Renovar +30d
            </button>
            <button mat-stroked-button type="button" (click)="toggleStatus()">
              {{ form.value.ativo ? 'Desativar' : 'Ativar' }}
            </button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .form { display: grid; gap: 8px; max-width: 520px; }
      .actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
      .mat-mdc-button .mat-icon,
      .mat-mdc-outlined-button .mat-icon { margin-right: 4px; }
      .status { font-size: 12px; color: var(--muted); }
    `
  ]
})
export class TenantFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  locatario: LocatarioResponse | null = null;
  title = 'Novo locatário';

  form = this.fb.group({
    nome: ['', Validators.required],
    dataLimiteAcesso: ['', Validators.required],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: TenantService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (id) {
      this.mode = isEdit ? 'edit' : 'view';
      this.load(Number(id));
    } else {
      this.mode = 'new';
      this.updateTitle();
    }
  }

  private updateTitle() {
    this.title = this.mode === 'new' ? 'Novo locatário' : this.mode === 'edit' ? 'Editar locatário' : 'Consultar locatário';
  }

  private load(id: number) {
    this.service.get(id).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({
          nome: data.nome,
          dataLimiteAcesso: data.dataLimiteAcesso,
          ativo: data.ativo
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
        }
        this.updateTitle();
      }
    });
  }

  save() {
    if (this.form.invalid) return;
    const payload = {
      nome: this.form.value.nome!,
      dataLimiteAcesso: this.form.value.dataLimiteAcesso!,
      ativo: !!this.form.value.ativo
    };
    if (this.mode === 'new') {
      this.service.create(payload).subscribe({
        next: () => this.router.navigateByUrl('/tenants')
      });
      return;
    }
    if (!this.locatario) return;
    this.service.update(this.locatario.id, payload).subscribe({
      next: () => this.router.navigate(['/tenants', this.locatario!.id])
    });
  }

  renew() {
    if (!this.locatario) return;
    const date = new Date(this.locatario.dataLimiteAcesso);
    date.setDate(date.getDate() + 30);
    const iso = date.toISOString().slice(0, 10);
    this.service.updateAccessLimit(this.locatario.id, iso).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ dataLimiteAcesso: data.dataLimiteAcesso });
      }
    });
  }

  toggleStatus() {
    if (!this.locatario) return;
    const ativo = !this.locatario.ativo;
    this.service.updateStatus(this.locatario.id, ativo).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ ativo: data.ativo });
      }
    });
  }

  remove() {
    if (!this.locatario) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir locatário', message: `Deseja excluir o locatário "${this.locatario.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(this.locatario!.id).subscribe({
        next: () => this.router.navigateByUrl('/tenants')
      });
    });
  }

  toEdit() {
    if (!this.locatario) return;
    this.router.navigate(['/tenants', this.locatario.id, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/tenants');
  }
}
