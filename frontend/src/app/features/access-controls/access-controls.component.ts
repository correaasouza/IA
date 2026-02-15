import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { FormsModule } from '@angular/forms';

import { AccessControlService } from '../../core/access/access-control.service';
import { MenuItem, MenuService } from '../../core/menu/menu.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { EntityTypeService } from '../entity-types/entity-type.service';

@Component({
  selector: 'app-access-controls',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatTableModule,
    MatTooltipModule,
    MatDialogModule,
    MatMenuModule
  ],
  templateUrl: './access-controls.component.html'
})
export class AccessControlsComponent implements OnInit {
  policies: Array<{ controlKey: string; roles: string[] }> = [];
  filteredPolicies: Array<{ controlKey: string; roles: string[] }> = [];
  displayedColumns = ['controlKey', 'roles', 'acoes'];
  canConfigure = false;
  searchTerm = '';
  isMobile = false;
  mobileFiltersOpen = false;
  private fallbackRolesByKey: Record<string, string[]> = {};

  constructor(
    private access: AccessControlService,
    private menuService: MenuService,
    private entityTypeService: EntityTypeService,
    private breakpoint: BreakpointObserver,
    private dialog: MatDialog,
    private notify: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.canConfigure = this.access.canConfigure();
    this.breakpoint.observe(['(max-width: 900px)']).subscribe(result => {
      this.isMobile = result.matches;
      if (!result.matches) this.mobileFiltersOpen = false;
    });
    this.reload();
  }

  reload() {
    this.access.refreshPolicies();
    const baseFallbacks = this.buildFallbackRolesByKey(this.menuService.items);
    this.entityTypeService.list({ page: 0, size: 300, ativo: true }).subscribe({
      next: data => {
        const typeFallbacks = this.buildEntityTypeFallbacks(data?.content || []);
        this.fallbackRolesByKey = { ...baseFallbacks, ...typeFallbacks };
        this.composePolicies();
      },
      error: () => {
        this.fallbackRolesByKey = baseFallbacks;
        this.composePolicies();
      }
    });
  }

  viewPolicy(item: { controlKey: string }): void {
    this.router.navigate(['/access-controls', this.encodeKey(item.controlKey)]);
  }

  editPolicy(item: { controlKey: string }): void {
    this.router.navigate(['/access-controls', this.encodeKey(item.controlKey), 'edit']);
  }

  createPolicy(): void {
    this.router.navigate(['/access-controls', 'new']);
  }

  removePolicy(item: { controlKey: string }): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir regra de acesso',
        message: `Deseja excluir a regra "${item.controlKey}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.access.deletePolicy(item.controlKey);
      this.notify.success('Regra removida.');
      this.reload();
    });
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    return this.searchTerm.trim() ? 1 : 0;
  }

  onSearchTermChange(value: string): void {
    this.searchTerm = value || '';
    this.applyFilters();
  }

  encodeKey(key: string): string {
    return encodeURIComponent(key);
  }

  private applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredPolicies = [...this.policies];
      return;
    }
    this.filteredPolicies = this.policies.filter(p =>
      p.controlKey.toLowerCase().includes(term) ||
      (p.roles || []).join(' ').toLowerCase().includes(term)
    );
  }

  private normalizeRoles(values: string[]): string[] {
    return Array.from(new Set((values || [])
      .map(v => (v || '').trim().toUpperCase())
      .filter(v => v.length > 0)
      .concat(['MASTER'])));
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
      const key = (item.accessKey || `menu.${item.id}`).toLowerCase();
      map[key] = (item.roles || []).map(r => (r || '').toUpperCase());
    }
    return map;
  }

  private buildEntityTypeFallbacks(types: Array<{ id: number; tipoPadrao?: boolean; ativo?: boolean }>): Record<string, string[]> {
    const map: Record<string, string[]> = {};
    (types || [])
      .filter(t => t?.ativo !== false && !!t?.id && !t?.tipoPadrao)
      .forEach(t => {
        map[`menu.entities.tipo.${t.id}`] = ['MASTER', 'ADMIN'];
      });
    return map;
  }

  private composePolicies(): void {
    const map = new Map<string, string[]>();
    Object.entries(this.fallbackRolesByKey).forEach(([key, fallback]) => {
      map.set(key, this.normalizeRoles(this.access.getRoles(key, fallback)));
    });
    this.access.listPolicies().forEach(p => {
      map.set(p.controlKey, this.normalizeRoles(p.roles));
    });
    this.policies = Array.from(map.entries())
      .map(([controlKey, roles]) => ({ controlKey, roles }))
      .sort((a, b) => a.controlKey.localeCompare(b.controlKey));
    this.applyFilters();
  }
}
