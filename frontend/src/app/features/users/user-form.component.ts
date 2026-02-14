import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { forkJoin, of } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { TenantService, LocatarioResponse } from '../tenants/tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { AccessControlService } from '../../core/access/access-control.service';

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
    MatSelectModule,
    FormsModule,
    MatIconModule,
    MatDialogModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './user-form.component.html',
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
  toggling = false;
  savingLocatarios = false;
  locatariosLoading = false;
  locatariosDisponiveis: LocatarioResponse[] = [];
  canCreateUser = false;

  form = this.fb.group({
    username: ['', Validators.required],
    email: [''],
    password: ['', Validators.required],
    roles: ['USER'],
    ativo: [true]
  });
  locatariosAcessoForm = this.fb.group({
    locatarioIds: [[] as number[]]
  });

  constructor(
    private fb: FormBuilder,
    private service: UsuarioService,
    private tenantService: TenantService,
    private accessControl: AccessControlService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.canCreateUser = this.accessControl.can('users.create', ['MASTER']);
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
    this.locatariosLoading = true;
    forkJoin({
      user: this.service.get(id),
      locatarios: this.tenantService.list({ page: 0, size: 500, sort: 'nome,asc' }),
      acessos: this.service.getLocatarios(id)
    }).pipe(finalize(() => {
      this.loading = false;
      this.locatariosLoading = false;
    })).subscribe({
      next: ({ user, locatarios, acessos }) => {
        this.user = user;
        this.locatariosDisponiveis = locatarios.content || [];
        this.form.patchValue({
          username: user.username,
          email: user.email || '',
          ativo: user.ativo
        });
        this.locatariosAcessoForm.patchValue({
          locatarioIds: acessos.locatarioIds || []
        });
        if (this.mode === 'view') {
          this.form.disable();
          this.locatariosAcessoForm.disable();
        } else {
          this.form.enable();
          this.form.get('password')?.disable();
          this.form.get('roles')?.disable();
          this.locatariosAcessoForm.enable();
        }
        this.updateTitle();
      },
      error: () => this.notify.error('Não foi possível carregar o usuário.')
    });
  }

  save() {
    if (this.mode === 'new' && !this.accessControl.can('users.create', ['MASTER'])) return;
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
        error: (err) => this.notify.error(this.extractApiError(err, 'Não foi possível criar o usuário.'))
      });
      return;
    }
    if (!this.user) {
      this.saving = false;
      return;
    }
    const locatarioIds = this.locatariosAcessoForm.value.locatarioIds || [];
    forkJoin({
      user: this.service.update(this.user.id, {
        username: this.form.value.username!,
        email: this.form.value.email || undefined,
        ativo: !!this.form.value.ativo
      }),
      acessos: this.mode === 'edit'
        ? this.service.setLocatarios(this.user.id, locatarioIds)
        : of({ locatarioIds: [] as number[] })
    }).pipe(finalize(() => this.saving = false)).subscribe({
      next: ({ user, acessos }) => {
        this.user = user;
        if (this.mode === 'edit') {
          this.locatariosAcessoForm.patchValue({
            locatarioIds: acessos.locatarioIds || []
          });
        }
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
    if (!this.accessControl.can('users.delete', ['MASTER'])) return;
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

  toggleStatus() {
    if (!this.accessControl.can('users.toggleStatus', ['MASTER'])) return;
    if (!this.user || this.mode === 'new') return;
    const nextStatus = !this.user.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: nextStatus ? 'Ativar usuário' : 'Desativar usuário',
        message: nextStatus
          ? `Deseja ativar o usuário "${this.user.username}"?`
          : `Deseja desativar o usuário "${this.user.username}"?`,
        confirmText: nextStatus ? 'Ativar' : 'Desativar',
        confirmColor: nextStatus ? 'primary' : 'warn',
        confirmAriaLabel: `${nextStatus ? 'Ativar' : 'Desativar'} usuário`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.user) return;
      this.toggling = true;
      this.service.update(this.user.id, {
        username: this.form.value.username!,
        email: this.form.value.email || undefined,
        ativo: nextStatus
      }).pipe(finalize(() => (this.toggling = false))).subscribe({
        next: updated => {
          this.user = updated;
          this.form.patchValue({ ativo: updated.ativo });
          this.notify.success(nextStatus ? 'Usuário ativado.' : 'Usuário desativado.');
        },
        error: () => this.notify.error('Não foi possível atualizar o status do usuário.')
      });
    });
  }

  saveLocatariosAcesso() {
    if (!this.accessControl.can('users.manageAccess', ['MASTER'])) return;
    if (!this.user || this.mode === 'view') return;
    const locatarioIds = this.locatariosAcessoForm.value.locatarioIds || [];
    this.savingLocatarios = true;
    this.service.setLocatarios(this.user.id, locatarioIds)
      .pipe(finalize(() => (this.savingLocatarios = false)))
      .subscribe({
        next: (data) => {
          this.locatariosAcessoForm.patchValue({
            locatarioIds: data.locatarioIds || []
          });
          this.notify.success('Acesso por locatário atualizado.');
        },
        error: () => this.notify.error('Não foi possível salvar os locatários de acesso.')
      });
  }

  toEdit() {
    if (!this.user) return;
    this.router.navigate(['/users', this.user.id, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/users');
  }

  private extractApiError(err: any, fallback: string): string {
    const detail = err?.error?.detail;
    if (typeof detail === 'string' && detail.trim().length > 0) {
      return detail;
    }
    return fallback;
  }

}




