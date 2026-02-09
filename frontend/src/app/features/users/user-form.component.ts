import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    FormsModule,
    MatIconModule,
    MatDialogModule
  ],
  template: `
    <mat-card class="card">
      <mat-card-title>{{ title }}</mat-card-title>
      <mat-card-content>
        <form [formGroup]="form" class="form">
          <mat-form-field appearance="outline">
            <mat-label>Username</mat-label>
            <input matInput formControlName="username" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" />
          </mat-form-field>
          <mat-form-field appearance="outline" *ngIf="mode === 'new'">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" />
          </mat-form-field>
          <mat-form-field appearance="outline" *ngIf="mode === 'new'">
            <mat-label>Roles (vírgula)</mat-label>
            <input matInput formControlName="roles" placeholder="USER, TENANT_ADMIN" />
          </mat-form-field>
          <mat-slide-toggle formControlName="ativo">Ativo</mat-slide-toggle>

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

          <div class="reset" *ngIf="mode !== 'new'">
            <div class="reset-title">Resetar senha</div>
            <mat-form-field appearance="outline">
              <mat-label>Nova senha</mat-label>
              <input matInput type="password" [(ngModel)]="resetPasswordValue" name="resetPasswordValue" />
            </mat-form-field>
            <button mat-stroked-button type="button" (click)="resetPassword()">
              <mat-icon>lock_reset</mat-icon> Aplicar nova senha
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
      .reset { margin-top: 12px; display: grid; gap: 6px; }
      .reset-title { font-size: 12px; color: var(--muted); }
    `
  ]
})
export class UserFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  user: UsuarioResponse | null = null;
  title = 'Novo usuário';
  resetPasswordValue = '';

  form = this.fb.group({
    username: ['', Validators.required],
    email: [''],
    password: ['', Validators.required],
    roles: ['USER'],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: UsuarioService,
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
    this.title = this.mode === 'new' ? 'Novo usuário' : this.mode === 'edit' ? 'Editar usuário' : 'Consultar usuário';
  }

  private load(id: number) {
    this.service.get(id).subscribe({
      next: data => {
        this.user = data;
        this.form.patchValue({
          username: data.username,
          email: data.email || '',
          ativo: data.ativo
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
          this.form.get('password')?.disable();
          this.form.get('roles')?.disable();
        }
        this.updateTitle();
      }
    });
  }

  save() {
    if (this.form.invalid) return;
    if (this.mode === 'new') {
      const roles = (this.form.value.roles || '')
        .split(',')
        .map(r => r.trim())
        .filter(r => r.length > 0);
      this.service.create({
        username: this.form.value.username!,
        email: this.form.value.email || undefined,
        password: this.form.value.password!,
        ativo: !!this.form.value.ativo,
        roles
      }).subscribe({ next: () => this.router.navigateByUrl('/users') });
      return;
    }
    if (!this.user) return;
    this.service.update(this.user.id, {
      username: this.form.value.username!,
      email: this.form.value.email || undefined,
      ativo: !!this.form.value.ativo
    }).subscribe({
      next: () => this.router.navigate(['/users', this.user!.id])
    });
  }

  resetPassword() {
    if (!this.user || !this.resetPasswordValue) return;
    this.service.resetPassword(this.user.id, this.resetPasswordValue).subscribe({
      next: () => {
        this.resetPasswordValue = '';
        alert('Senha atualizada.');
      }
    });
  }

  remove() {
    if (!this.user) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir usuário', message: `Deseja excluir o usuário "${this.user.username}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(this.user!.id).subscribe({
        next: () => this.router.navigateByUrl('/users')
      });
    });
  }

  toEdit() {
    if (!this.user) return;
    this.router.navigate(['/users', this.user.id, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/users');
  }
}
