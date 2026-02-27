import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { AccessControlService } from '../../core/access/access-control.service';
import { MenuItem, MenuService } from '../../core/menu/menu.service';
import { RolesService } from '../roles/roles.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-access-control-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './access-control-form.component.html'
})
export class AccessControlFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Nova regra de acesso';
  roleOptions: string[] = ['MASTER', 'USER'];
  currentKey: string | null = null;
  private fallbackRolesByKey: Record<string, string[]> = {};

  form = this.fb.group({
    controlKey: ['', Validators.required],
    roles: this.fb.control<string[]>(['MASTER'], Validators.required)
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private access: AccessControlService,
    private menuService: MenuService,
    private rolesService: RolesService,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.fallbackRolesByKey = this.buildFallbackRolesByKey(this.menuService.items);
    this.loadRoleOptions();
    const keyParam = this.route.snapshot.paramMap.get('key');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (!keyParam) {
      this.mode = 'new';
      this.title = 'Nova regra de acesso';
      this.form.enable();
      this.ensureMasterRole();
      return;
    }

    this.currentKey = decodeURIComponent(keyParam);
    this.mode = isEdit ? 'edit' : 'view';
    this.title = this.mode === 'edit' ? 'Editar regra de acesso' : 'Consultar regra de acesso';
    this.loadPolicy();
  }

  toEdit(): void {
    if (!this.currentKey) return;
    this.router.navigate(['/access-controls', encodeURIComponent(this.currentKey), 'edit']);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const controlKey = (this.form.get('controlKey')?.value || '').trim().toLowerCase();
    if (!controlKey) return;
    const roles = this.normalizeRoles(this.form.get('roles')?.value || []);
    this.access.setRoles(controlKey, roles);
    this.notify.success(this.mode === 'new' ? 'Regra criada.' : 'Regra atualizada.');
    this.router.navigateByUrl('/access-controls');
  }

  remove(): void {
    if (!this.currentKey) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir regra de acesso',
        message: `Deseja excluir a regra "${this.currentKey}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok || !this.currentKey) return;
      this.access.deletePolicy(this.currentKey);
      this.notify.success('Regra excluída.');
      this.router.navigateByUrl('/access-controls');
    });
  }

  back(): void {
    this.router.navigateByUrl('/access-controls');
  }

  isMasterRole(role: string): boolean {
    return (role || '').toUpperCase() === 'MASTER';
  }

  onRolesChange(): void {
    this.ensureMasterRole();
  }

  private loadPolicy(): void {
    if (!this.currentKey) return;
    const policy = this.access.listPolicies().find(p => p.controlKey === this.currentKey);
    const fallback = this.fallbackRolesByKey[this.currentKey] || [];
    const effectiveRoles = policy ? policy.roles : this.access.getRoles(this.currentKey, fallback);

    if (!policy && fallback.length === 0) {
      this.notify.error('Regra não encontrada.');
      this.router.navigateByUrl('/access-controls');
      return;
    }

    this.form.patchValue({
      controlKey: this.currentKey,
      roles: this.normalizeRoles(effectiveRoles)
    });
    if (this.mode === 'view') {
      this.form.disable();
    } else {
      this.form.enable();
    }
  }

  private loadRoleOptions(): void {
    this.rolesService.list().subscribe({
      next: (items) => {
        const roles = (items || [])
          .filter(r => r?.ativo !== false)
          .map(r => (r?.nome || '').trim().toUpperCase())
          .filter(r => r.length > 0);
        this.roleOptions = Array.from(new Set(['MASTER', 'USER', ...roles])).sort((a, b) => a.localeCompare(b));
      },
      error: () => {
        this.roleOptions = ['MASTER', 'USER'];
      }
    });
  }

  private ensureMasterRole(): void {
    const roles = this.normalizeRoles(this.form.get('roles')?.value || []);
    this.form.patchValue({ roles }, { emitEvent: false });
  }

  private normalizeRoles(values: string[]): string[] {
    return Array.from(new Set((values || [])
      .map(v => this.normalizeRole(v || ''))
      .filter(v => v.length > 0)
      .concat(['MASTER'])));
  }

  private normalizeRole(value: string): string {
    let role = (value || '').trim().toUpperCase();
    if (!role) {
      return '';
    }
    const sepIdx = role.lastIndexOf(':');
    if (sepIdx >= 0 && sepIdx < role.length - 1) {
      role = role.substring(sepIdx + 1);
    }
    if (role.startsWith('ROLE_')) {
      role = role.substring(5);
    }
    return role;
  }

  private buildFallbackRolesByKey(items: MenuItem[]): Record<string, string[]> {
    const map: Record<string, string[]> = {};
    const stack = [...(items || [])];
    while (stack.length) {
      const item = stack.shift();
      if (!item) continue;
      if (item.children && item.children.length > 0) {
        stack.push(...item.children);
        continue;
      }
      if (!item.id) continue;
      map[`menu.${item.id}`] = (item.roles || []).map(r => (r || '').toUpperCase());
    }
    return map;
  }
}


