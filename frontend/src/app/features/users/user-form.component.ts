import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    FormsModule,
    MatIconModule,
    MatDialogModule,
    InlineLoaderComponent
  ],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  user: UsuarioResponse | null = null;
  title = 'Novo usuário';
  resetPasswordValue = '';
  loading = false;
  saving = false;
  deleting = false;
  resetting = false;

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
    private dialog: MatDialog,
    private notify: NotificationService
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
    this.loading = true;
    this.service.get(id).pipe(finalize(() => this.loading = false)).subscribe({
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
      },
      error: () => this.notify.error('Não foi possível carregar o usuário.')
    });
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
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
      }).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.notify.success('Usuário criado.');
          this.router.navigateByUrl('/users');
        },
        error: () => this.notify.error('Não foi possível criar o usuário.')
      });
      return;
    }
    if (!this.user) {
      this.saving = false;
      return;
    }
    this.service.update(this.user.id, {
      username: this.form.value.username!,
      email: this.form.value.email || undefined,
      ativo: !!this.form.value.ativo
    }).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.notify.success('Usuário atualizado.');
        this.router.navigate(['/users', this.user!.id]);
      },
      error: () => this.notify.error('Não foi possível atualizar o usuário.')
    });
  }

  resetPassword() {
    if (!this.user || !this.resetPasswordValue) return;
    this.resetting = true;
    this.service.resetPassword(this.user.id, this.resetPasswordValue).pipe(finalize(() => this.resetting = false)).subscribe({
      next: () => {
        this.resetPasswordValue = '';
        this.notify.success('Senha atualizada.');
      },
      error: () => this.notify.error('Não foi possível atualizar a senha.')
    });
  }

  remove() {
    if (!this.user) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir usuário', message: `Deseja excluir o usuário "${this.user.username}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deleting = true;
      this.service.delete(this.user!.id).pipe(finalize(() => this.deleting = false)).subscribe({
        next: () => {
          this.notify.success('Usuário removido.');
          this.router.navigateByUrl('/users');
        },
        error: () => this.notify.error('Não foi possível remover o usuário.')
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

