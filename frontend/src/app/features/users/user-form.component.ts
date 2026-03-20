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
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import {
  UsuarioService,
  UsuarioResponse,
  UsuarioEmpresaAcessoResponse,
  UsuarioEmpresaOpcao,
  UsuarioEmpresaPadraoResponse
} from './usuario.service';
import { TenantService, LocatarioResponse } from '../tenants/tenant.service';
import { EmpresaResponse } from '../companies/company.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { AuthService } from '../../core/auth/auth.service';
import { RolesService } from '../roles/roles.service';

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
  locatariosLoading = false;
  empresasLoading = false;
  locatariosDisponiveis: LocatarioResponse[] = [];
  empresasDisponiveis: UsuarioEmpresaOpcao[] = [];
  empresasDisponiveisAll: UsuarioEmpresaOpcao[] = [];
  canCreateUser = false;
  canManageLocatarios = false;
  canManageCompanyAccess = false;
  roleOptions: string[] = ['USER'];

  form = this.fb.group({
    username: ['', Validators.required],
    email: [''],
    password: ['', Validators.required],
    roles: this.fb.control<string[]>(['USER'], Validators.required),
    ativo: [true]
  });
  locatariosAcessoForm = this.fb.group({
    locatarioIds: [[] as number[]]
  });
  empresasAcessoForm = this.fb.group({
    empresaIds: [[] as number[]]
  });
  empresaPadraoForm = this.fb.group({
    empresaId: [null as number | null]
  });

  constructor(
    private fb: FormBuilder,
    private service: UsuarioService,
    private rolesService: RolesService,
    private tenantService: TenantService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadRoleOptions();
    this.canCreateUser = this.hasRole(['MASTER', 'ADMIN']);
    this.canManageLocatarios = this.isGlobalMaster();
    this.canManageCompanyAccess = this.hasRole(['MASTER', 'ADMIN']);
    this.locatariosAcessoForm.get('locatarioIds')?.valueChanges.subscribe(() => {
      this.applyEmpresasFilterByLocatarios();
    });
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
    this.empresasLoading = true;
    const locatarios$ = this.canManageLocatarios
      ? this.tenantService.list({ page: 0, size: 500, sort: 'nome,asc' }).pipe(
          catchError(() => this.tenantService.allowed().pipe(catchError(() => of([] as LocatarioResponse[]))))
        )
      : this.tenantService.allowed().pipe(catchError(() => of([] as LocatarioResponse[])));
    forkJoin({
      user: this.service.get(id),
      locatarios: locatarios$,
      acessos: this.service.getLocatarios(id).pipe(catchError(() => of({ locatarioIds: [] as number[] }))),
      empresasDisponiveis: this.service.getEmpresasDisponiveis(id).pipe(
        catchError(() => of([] as UsuarioEmpresaOpcao[]))
      ),
      empresasAcesso: this.service.getEmpresasAcesso(id).pipe(catchError(() => of({ empresaIds: [] as number[] }))),
      empresaPadrao: this.service.getEmpresaPadrao(id).pipe(catchError(() => of({ empresaId: null })))
    }).pipe(finalize(() => {
      this.loading = false;
      this.locatariosLoading = false;
      this.empresasLoading = false;
    })).subscribe({
      next: ({ user, locatarios, acessos, empresasDisponiveis, empresasAcesso, empresaPadrao }) => {
        this.user = user;
        this.locatariosDisponiveis = this.normalizeLocatarios(locatarios);
        this.empresasDisponiveisAll = this.normalizeEmpresasDisponiveis(empresasDisponiveis);
        this.form.patchValue({
          username: user.username,
          email: user.email || '',
          ativo: user.ativo
        });
        this.locatariosAcessoForm.patchValue({
          locatarioIds: acessos.locatarioIds || []
        });
        this.empresasAcessoForm.patchValue({
          empresaIds: this.normalizeEmpresaIds(empresasAcesso)
        });
        this.empresaPadraoForm.patchValue({
          empresaId: this.extractEmpresaPadrao(empresaPadrao)
        });
        this.applyEmpresasFilterByLocatarios();
        if (this.mode === 'view') {
          this.form.disable();
          this.locatariosAcessoForm.disable();
          this.empresasAcessoForm.disable();
          this.empresaPadraoForm.disable();
        } else {
          this.form.enable();
          this.form.get('password')?.disable();
          this.form.get('roles')?.disable();
          if (this.canManageLocatarios) {
            this.locatariosAcessoForm.enable();
          } else {
            this.locatariosAcessoForm.disable();
          }
          if (this.canManageCompanyAccess) {
            this.empresasAcessoForm.enable();
          } else {
            this.empresasAcessoForm.disable();
          }
          this.empresaPadraoForm.enable();
        }
        this.updateTitle();
      },
      error: () => this.notify.error('Não foi possível carregar o usuário.')
    });
  }

  save() {
    if (this.mode === 'new' && !this.canCreateUser) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    if (this.mode === 'new') {
      const roles = this.normalizeRoles(this.form.get('roles')?.value || []);
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
    const empresaIds = this.empresasAcessoForm.value.empresaIds || [];
    const empresaPadraoId = this.empresaPadraoForm.value.empresaId;
    this.service.update(this.user.id, {
      username: this.form.value.username!,
      email: this.form.value.email || undefined,
      ativo: !!this.form.value.ativo
    }).pipe(
      tap(user => {
        this.user = user;
      }),
      switchMap(() => {
        if (this.mode !== 'edit' || !this.canManageLocatarios) {
          return of(null);
        }
        return this.service.setLocatarios(this.user!.id, locatarioIds).pipe(
          tap((acessos) => {
            this.locatariosAcessoForm.patchValue({
              locatarioIds: acessos.locatarioIds || []
            });
          })
        );
      }),
      switchMap(() => {
        if (this.mode !== 'edit' || !this.canManageCompanyAccess) {
          return of(null);
        }
        return this.service.setEmpresasAcesso(this.user!.id, empresaIds).pipe(
          tap((acessos) => {
            const normalizedEmpresaIds = this.normalizeEmpresaIds(acessos);
            this.empresasAcessoForm.patchValue({
              empresaIds: normalizedEmpresaIds
            });
            const atualPadrao = this.empresaPadraoForm.value.empresaId;
            if (atualPadrao && !normalizedEmpresaIds.includes(atualPadrao)) {
              this.empresaPadraoForm.patchValue({ empresaId: null });
            }
          })
        );
      }),
      switchMap(() => {
        if (this.mode !== 'edit' || !empresaPadraoId) {
          return of(null);
        }
        return this.service.setEmpresaPadrao(this.user!.id, empresaPadraoId).pipe(
          tap((padrao) => {
            this.empresaPadraoForm.patchValue({
              empresaId: this.extractEmpresaPadrao(padrao)
            });
          })
        );
      }),
      finalize(() => this.saving = false)
    ).subscribe({
      next: () => {
        this.notify.success('Usuário atualizado.');
        this.router.navigateByUrl('/users');
      },
      error: (err) => this.notify.error(this.extractApiError(err, 'Não foi possível atualizar o usuário.'))
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
    if (!this.canCreateUser) return;
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
    if (!this.canCreateUser) return;
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

  onRolesChange(): void {
    this.enforceMasterRoleConstraint();
  }

  onUsernameChange(): void {
    this.enforceMasterRoleConstraint();
  }

  isRoleDisabled(role: string): boolean {
    return this.isMasterRole(role) && !this.isMasterUser();
  }

  private loadRoleOptions(): void {
    this.rolesService.list().subscribe({
      next: (items) => {
        const roles = (items || [])
          .filter(r => r?.ativo !== false)
          .map(r => (r?.nome || '').trim().toUpperCase())
          .filter(r => r.length > 0);
        this.roleOptions = Array.from(new Set(['USER', ...roles])).sort((a, b) => a.localeCompare(b));
        this.enforceMasterRoleConstraint();
      },
      error: () => {
        this.roleOptions = ['USER'];
      }
    });
  }

  private enforceMasterRoleConstraint(): void {
    const roles = this.normalizeRoles(this.form.get('roles')?.value || [])
      .filter(role => !this.isMasterRole(role) || this.isMasterUser());
    this.form.patchValue({
      roles: roles.length > 0 ? roles : ['USER']
    }, { emitEvent: false });
  }

  private isMasterRole(role: string): boolean {
    return (role || '').trim().toUpperCase() === 'MASTER';
  }

  private isMasterUser(): boolean {
    return this.isMasterUsername(this.form.get('username')?.value || '');
  }

  private isMasterUsername(username: string): boolean {
    return (username || '').trim().toLowerCase() === 'master';
  }

  private normalizeRoles(values: string[]): string[] {
    return Array.from(new Set((values || [])
      .map(v => (v || '').trim().toUpperCase())
      .filter(v => v.length > 0)));
  }

  private normalizeEmpresaIds(value: UsuarioEmpresaAcessoResponse | null | undefined): number[] {
    return Array.from(new Set((value?.empresaIds || []).filter((id): id is number => typeof id === 'number')));
  }

  private extractEmpresaPadrao(value: UsuarioEmpresaPadraoResponse | null | undefined): number | null {
    const empresaId = value?.empresaId;
    return typeof empresaId === 'number' ? empresaId : null;
  }

  private normalizeLocatarios(value: unknown): LocatarioResponse[] {
    if (Array.isArray(value)) {
      return value as LocatarioResponse[];
    }
    const pageContent = (value as { content?: LocatarioResponse[] } | null)?.content;
    return Array.isArray(pageContent) ? pageContent : [];
  }

  private normalizeEmpresasDisponiveis(value: unknown): UsuarioEmpresaOpcao[] {
    if (Array.isArray(value)) {
      return (value as UsuarioEmpresaOpcao[]).map((item) => ({
        ...item,
        tenantId: typeof item?.tenantId === 'number' ? item.tenantId : this.currentTenantId()
      }));
    }
    const pageContent = (value as { content?: EmpresaResponse[] } | null)?.content;
    if (!Array.isArray(pageContent)) {
      return [];
    }
    return pageContent.map((e) => ({
      id: e.id,
      tenantId: typeof e.tenantId === 'number' ? e.tenantId : this.currentTenantId(),
      razaoSocial: e.razaoSocial,
      nomeFantasia: e.nomeFantasia,
      ativo: e.ativo
    }));
  }

  private applyEmpresasFilterByLocatarios(): void {
    if (!this.canManageLocatarios) {
      this.empresasDisponiveis = [...this.empresasDisponiveisAll];
      return;
    }
    const selectedLocatarios = new Set(
      (this.locatariosAcessoForm.value.locatarioIds || [])
        .filter((id): id is number => typeof id === 'number')
    );
    this.empresasDisponiveis = this.empresasDisponiveisAll.filter((empresa) => {
      const tenantId = typeof empresa.tenantId === 'number' ? empresa.tenantId : this.currentTenantId();
      if (tenantId == null) {
        return false;
      }
      return selectedLocatarios.has(tenantId);
    });

    const allowedCompanyIds = new Set(this.empresasDisponiveis.map((empresa) => empresa.id));
    const empresaIdsAtual = (this.empresasAcessoForm.value.empresaIds || [])
      .filter((id): id is number => typeof id === 'number');
    const empresaIdsFiltrados = empresaIdsAtual.filter((id) => allowedCompanyIds.has(id));
    if (empresaIdsFiltrados.length !== empresaIdsAtual.length) {
      this.empresasAcessoForm.patchValue({ empresaIds: empresaIdsFiltrados }, { emitEvent: false });
    }
    const empresaPadraoAtual = this.empresaPadraoForm.value.empresaId;
    if (empresaPadraoAtual && !allowedCompanyIds.has(empresaPadraoAtual)) {
      this.empresaPadraoForm.patchValue({ empresaId: null }, { emitEvent: false });
    }
  }

  private currentTenantId(): number | null {
    const raw = (localStorage.getItem('tenantId') || '').trim();
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private isGlobalMaster(): boolean {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (tenantId !== '1') {
      return false;
    }
    const username = (this.auth.getUsername() || '').trim().toLowerCase();
    if (username === 'master') {
      return true;
    }
    return this.hasRole(['MASTER']);
  }

  private hasRole(roles: string[]): boolean {
    const normalizedTarget = new Set(roles.map(role => role.trim().toUpperCase()));
    const current = new Set([
      ...this.normalizeRoles(this.auth.getUserRoles() || []),
      ...this.readTenantRoles()
    ]);
    if ((this.auth.getUsername() || '').trim().toLowerCase() === 'master') {
      current.add('MASTER');
    }
    for (const role of normalizedTarget) {
      if (current.has(role)) {
        return true;
      }
    }
    return false;
  }

  private readTenantRoles(): string[] {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (!tenantId) {
      return [];
    }
    try {
      const raw = localStorage.getItem(`tenantRoles:${tenantId}`);
      const parsed = raw ? JSON.parse(raw) : [];
      return this.normalizeRoles(Array.isArray(parsed) ? parsed : []);
    } catch {
      return [];
    }
  }

}




