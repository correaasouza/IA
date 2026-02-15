import { Component, HostListener } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AuthService } from './core/auth/auth.service';
import { AccessControlService } from './core/access/access-control.service';
import { AtalhoService, AtalhoUsuario } from './core/atalhos/atalho.service';
import { MenuService, MenuItem } from './core/menu/menu.service';
import { AtalhoOrdenarDialogComponent } from './core/atalhos/atalho-ordenar-dialog.component';
import { IconService } from './core/menu/icon.service';
import { AccessControlConfigDialogComponent } from './core/access/access-control-config-dialog.component';
import { environment } from '../environments/environment';
import { CompanyService, EmpresaResponse } from './features/companies/company.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterOutlet,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatDialogModule,
    MatMenuModule,
    MatTooltipModule,
    FormsModule
  ],
  templateUrl: './app.component.html',
  
})
export class AppComponent {
  sidebarOpen = true;
  isMobile = false;
  menu: MenuItem[] = [];
  atalhos: AtalhoUsuario[] = [];
  atalhosTop: MenuItem[] = [];
  shortcutState: Record<string, boolean> = {};
  permissions: string[] = [];
  tenantRoles: string[] = [];
  hasTenant = false;
  isSelectionRoute = true;
  currentRoute = '';
  expandedGroups: Record<string, boolean> = {};
  private readonly expandedKey = 'menu:expanded';
  loggedIn = false;
  userName = '';
  userInitials = 'U';
  empresaContextOptions: EmpresaResponse[] = [];
  empresaContextId: number | null = null;
  loadingEmpresaContext = false;
  private loadingDefaultEmpresaId = 0;
  private currentTenantId = '';
  private readonly shortcutRemoving = new Set<string>();
  private readonly shortcutCreating = new Set<string>();
  private readonly menuLabelAliases: Record<string, string> = {
    metadata: 'Tipos Ent.'
  };

  constructor(
    private auth: AuthService,
    private accessControl: AccessControlService,
    private atalhoService: AtalhoService,
    private menuService: MenuService,
    private dialog: MatDialog,
    private breakpoint: BreakpointObserver,
    private iconService: IconService,
    private companyService: CompanyService,
    private http: HttpClient,
    private router: Router
  ) {
    if (typeof document !== 'undefined') {
      document.body.classList.remove('theme-zeta-graphite', 'theme-zeta-steel');
      document.body.classList.add('theme-zeta-slate');
    }

    const cachedExpanded = localStorage.getItem(this.expandedKey);
    if (cachedExpanded) {
      try {
        this.expandedGroups = JSON.parse(cachedExpanded);
      } catch {
        this.expandedGroups = {};
      }
    }
    this.refreshMenu();
    this.updateRouteFlags(this.router.url);
    this.loadShortcuts();
    this.loadMe();
    this.refreshEmpresaContextIfNeeded();

    this.breakpoint.observe(['(max-width: 900px)']).subscribe(result => {
      this.isMobile = result.matches;
      this.sidebarOpen = !result.matches;
    });

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(e => {
      const url = (e as NavigationEnd).urlAfterRedirects || (e as NavigationEnd).url;
      this.updateRouteFlags(url);
      if (this.isMobile && this.sidebarOpen) {
        this.sidebarOpen = false;
      }
      if (!this.isSelectionRoute && localStorage.getItem('tenantId')) {
        this.refreshMenu();
        this.loadShortcuts();
        this.refreshEmpresaContextIfNeeded();
      }
    });
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  @HostListener('window:empresa-context-updated')
  onEmpresaContextUpdated(): void {
    const tenantId = this.getTenantId();
    if (!tenantId || this.isSelectionRoute) {
      this.clearEmpresaContext();
      return;
    }
    this.currentTenantId = tenantId;
    this.loadEmpresaContextOptions();
  }

  login() {
    this.auth.login();
  }

  logout() {
    this.auth.logout();
  }

  isShortcut(menuId: string): boolean {
    return !!this.shortcutState[menuId];
  }

  toggleShortcut(item: MenuItem) {
    if (!item.route) return;
    if (this.shortcutCreating.has(item.id) || this.shortcutRemoving.has(item.id)) {
      return;
    }
    const existing = this.atalhos.filter(a => a.menuId === item.id);
    if (existing.length > 0) {
      this.shortcutRemoving.add(item.id);
      this.setShortcutState(item.id, false);
      this.atalhos = this.atalhos.filter(a => a.menuId !== item.id);
      this.rebuildShortcutsTop();
      const deletions = existing
        .filter(a => !!a.id)
        .map(a => this.atalhoService.delete(a.id));
      if (deletions.length === 0) {
        this.shortcutRemoving.delete(item.id);
        this.loadShortcuts();
        return;
      }
      forkJoin(deletions).pipe(finalize(() => this.shortcutRemoving.delete(item.id))).subscribe({
        next: () => {
          this.notifyShortcutsUpdated();
        },
        error: () => {
          this.setShortcutState(item.id, true);
          this.atalhos = [...this.atalhos, ...existing];
          this.rebuildShortcutsTop();
          this.loadShortcuts();
          this.notifyShortcutsUpdated();
        }
      });
      return;
    }
    this.shortcutCreating.add(item.id);
    const ordem = this.atalhos.length + 1;
    this.setShortcutState(item.id, true);
    this.atalhos = [...this.atalhos, { id: 0, menuId: item.id, icon: item.icon, ordem }];
    this.rebuildShortcutsTop();
    this.notifyShortcutsUpdated();
    this.atalhoService.create({ menuId: item.id, icon: item.icon, ordem })
      .pipe(finalize(() => this.shortcutCreating.delete(item.id)))
      .subscribe({
      next: created => {
        this.atalhos = this.atalhos.map(a =>
          a.menuId === item.id && a.id === 0
            ? { ...a, id: created?.id || a.id, ordem: created?.ordem || a.ordem }
            : a
        );
        this.rebuildShortcutsTop();
        this.notifyShortcutsUpdated();
      },
      error: () => {
        this.atalhos = this.atalhos.filter(a => !(a.menuId === item.id && a.id === 0));
        this.setShortcutState(item.id, false);
        this.rebuildShortcutsTop();
        this.loadShortcuts();
        this.notifyShortcutsUpdated();
      }
    });
  }

  loadShortcuts() {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId || this.isSelectionRoute) {
      this.atalhos = [];
      this.atalhosTop = [];
      this.shortcutState = {};
      this.hasTenant = false;
      this.clearEmpresaContext();
      this.notifyShortcutsUpdated();
      return;
    }
    this.hasTenant = true;
    this.atalhoService.list().subscribe({
      next: data => {
        this.atalhos = data || [];
        this.syncShortcutState();
        this.rebuildShortcutsTop();
        this.notifyShortcutsUpdated();
      }
    });
  }

  private syncShortcutState(): void {
    const next: Record<string, boolean> = {};
    for (const atalho of this.atalhos || []) {
      if (atalho?.menuId) {
        next[atalho.menuId] = true;
      }
    }
    this.shortcutState = next;
  }

  private setShortcutState(menuId: string, enabled: boolean): void {
    this.shortcutState = { ...this.shortcutState, [menuId]: enabled };
  }

  private notifyShortcutsUpdated(): void {
    if (typeof window === 'undefined') return;
    window.dispatchEvent(new CustomEvent('shortcuts-updated'));
  }

  private rebuildShortcutsTop() {
    const allowed = this.flattenMenu(this.menu);
    this.atalhosTop = (this.atalhos || [])
      .sort((a, b) => a.ordem - b.ordem)
      .map(a => allowed.find(m => m.id === a.menuId))
      .filter(Boolean) as MenuItem[];
  }

  openOrdenar() {
    const items = this.atalhosTop.map((m, idx) => ({
      id: this.atalhos.find(a => a.menuId === m.id)?.id || 0,
      label: m.label,
      ordem: idx + 1
    }));
    const ref = this.dialog.open(AtalhoOrdenarDialogComponent, {
      data: { items },
      width: '420px',
      maxWidth: '92vw',
      maxHeight: '80vh'
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const payload = result.map((r: any, idx: number) => ({ id: r.id, ordem: idx + 1 }));
      this.atalhoService.reorder(payload).subscribe({ next: () => this.loadShortcuts() });
    });
  }

  toggleGroup(item: MenuItem) {
    if (!item.children || item.children.length === 0) return;
    const next = !this.isGroupExpanded(item);
    this.expandedGroups[item.id] = next;
    this.persistExpanded();
  }

  isGroupExpanded(item: MenuItem): boolean {
    if (!item.children || item.children.length === 0) return false;
    return !!this.expandedGroups[item.id] || this.hasActiveChild(item);
  }

  hasActiveChild(item: MenuItem): boolean {
    return !!item.children?.some(child => this.isActiveRoute(child.route || ''));
  }

  private isAllowedLeaf(item: MenuItem): boolean {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId && item.id !== 'home') {
      return false;
    }
    const roleOk = !item.roles || item.roles.length === 0
      || this.accessControl.can(`menu.${item.id}`, item.roles);
    const permOk = !item.perms || item.perms.length === 0 || item.perms.some(p => this.permissions.includes(p));
    if (item.roles && item.roles.length > 0 && item.perms && item.perms.length > 0) {
      return roleOk || permOk;
    }
    return roleOk && permOk;
  }

  allowedMenu(item: MenuItem): boolean {
    if (item.children && item.children.length > 0) {
      return item.children.some(child => this.isAllowedLeaf(child));
    }
    return this.isAllowedLeaf(item);
  }

  loadMe() {
    this.http.get<any>(`${environment.apiBaseUrl}/api/me`).subscribe({
      next: data => {
        this.permissions = data?.permissions || [];
        this.tenantRoles = data?.tenantRoles || [];
        localStorage.setItem('tenantRoles', JSON.stringify(this.tenantRoles));
        const apiTenantId = data?.tenantId ? String(data.tenantId) : '';
        if (apiTenantId && !localStorage.getItem('tenantId')) {
          localStorage.setItem('tenantId', apiTenantId);
        }
        this.accessControl.refreshPolicies();
        this.setUserIdentity(data);
        this.refreshMenu();
        this.updateRouteFlags(this.router.url);
        this.refreshEmpresaContextIfNeeded();
        if (!this.isSelectionRoute) {
          this.loadShortcuts();
        }
        const tenantId = localStorage.getItem('tenantId');
        if (!tenantId) {
          this.router.navigateByUrl('/');
        }
      },
      error: () => {
        this.loggedIn = false;
        this.userName = '';
        this.userInitials = 'U';
      }
    });
  }

  refreshMenu() {
    this.menu = this.normalizeMenu(this.menuService.items);
    this.expandForRoute();
  }

  private normalizeMenu(items: MenuItem[]): MenuItem[] {
    return items.map(item => {
      if (item.children && item.children.length > 0) {
        const children = item.children
          .filter(child => this.isAllowedLeaf(child))
          .map(child => ({
            ...child,
            icon: this.iconService.resolveIcon(child.id, child.icon)
          }));
        if (children.length === 0) return null;
        return {
          ...item,
          icon: this.iconService.resolveIcon(item.id, item.icon),
          children
        } as MenuItem;
      }
      if (!this.isAllowedLeaf(item)) return null;
      return {
        ...item,
        icon: this.iconService.resolveIcon(item.id, item.icon)
      } as MenuItem;
    }).filter(Boolean) as MenuItem[];
  }

  private flattenMenu(items: MenuItem[]): MenuItem[] {
    return items.flatMap(item => item.children && item.children.length > 0 ? item.children : [item]);
  }

  private expandForRoute() {
    this.menu.forEach(item => {
      if (item.children && item.children.length > 0 && this.hasActiveChild(item)) {
        this.expandedGroups[item.id] = true;
      }
      if (item.children && item.children.length === 1 && !(item.id in this.expandedGroups)) {
        this.expandedGroups[item.id] = true;
      }
    });
    this.persistExpanded();
  }

  private persistExpanded() {
    localStorage.setItem(this.expandedKey, JSON.stringify(this.expandedGroups));
  }

  private updateRouteFlags(url: string) {
    this.isSelectionRoute = url === '/' || url === '';
    const tenantId = localStorage.getItem('tenantId');
    this.hasTenant = !!tenantId && !this.isSelectionRoute;
    this.currentRoute = url;
    this.expandForRoute();
  }

  private refreshEmpresaContextIfNeeded(): void {
    const tenantId = this.getTenantId();
    if (!tenantId || this.isSelectionRoute) {
      this.clearEmpresaContext();
      return;
    }
    if (this.currentTenantId !== tenantId) {
      this.empresaContextOptions = [];
      this.clearEmpresaContextSelection();
      this.currentTenantId = tenantId;
      this.loadEmpresaContextOptions();
      return;
    }
    if (!this.empresaContextOptions.length) {
      this.loadEmpresaContextOptions();
    }
  }

  private loadEmpresaContextOptions(): void {
    const requestTenantId = this.getTenantId();
    if (!requestTenantId) {
      this.clearEmpresaContext();
      return;
    }
    // Prevent stale UI (wrong selected/default badge) while data is refreshing.
    this.empresaContextOptions = [];
    this.clearEmpresaContextSelection();
    this.loadingEmpresaContext = true;
    this.companyService.list({ page: 0, size: 500 }).subscribe({
      next: companies => {
        if (this.isStaleTenantRequest(requestTenantId)) {
          this.loadingEmpresaContext = false;
          return;
        }
        const items = (companies?.content || []) as EmpresaResponse[];
        this.empresaContextOptions = items
          .sort((a, b) => {
            if (a.tipo !== b.tipo) {
              return a.tipo === 'MATRIZ' ? -1 : 1;
            }
            return (a.razaoSocial || '').localeCompare(b.razaoSocial || '');
          });
        const defaultFromList = this.empresaContextOptions.find(e => !!e.padrao)?.id || 0;
        this.restoreEmpresaContextSelection(defaultFromList);
        this.loadingEmpresaContext = false;
      },
      error: () => {
        if (this.isStaleTenantRequest(requestTenantId)) {
          this.loadingEmpresaContext = false;
          return;
        }
        this.clearEmpresaContext();
        this.loadingEmpresaContext = false;
      }
    });
  }

  private restoreEmpresaContextSelection(defaultId: number): void {
    const byDefault = defaultId ? this.empresaContextOptions.find(e => e.id === defaultId) : undefined;
    if (byDefault) {
      this.setEmpresaContext(byDefault.id);
      return;
    }

    if (defaultId) {
      this.clearEmpresaContextSelection();
      this.loadDefaultEmpresaAndApply(defaultId);
      return;
    }

    this.applyFirstSelection();
  }

  private loadDefaultEmpresaAndApply(defaultId: number): void {
    if (!defaultId) {
      this.applyFirstSelection();
      return;
    }
    if (this.loadingDefaultEmpresaId === defaultId) {
      return;
    }

    const requestTenantId = this.getTenantId();
    if (!requestTenantId) {
      this.clearEmpresaContextSelection();
      return;
    }

    this.loadingDefaultEmpresaId = defaultId;
    this.companyService.get(defaultId).pipe(finalize(() => (this.loadingDefaultEmpresaId = 0))).subscribe({
      next: empresa => {
        if (this.isStaleTenantRequest(requestTenantId)) {
          return;
        }
        if (!empresa) {
          this.applyFirstSelection();
          return;
        }
        const exists = this.empresaContextOptions.some(e => e.id === empresa.id);
        if (!exists) {
          this.empresaContextOptions = [...this.empresaContextOptions, empresa].sort((a, b) => {
            if (a.tipo !== b.tipo) {
              return a.tipo === 'MATRIZ' ? -1 : 1;
            }
            return (a.razaoSocial || '').localeCompare(b.razaoSocial || '');
          });
        }
        this.setEmpresaContext(empresa.id);
      },
      error: () => {
        if (this.isStaleTenantRequest(requestTenantId)) {
          return;
        }
        this.clearEmpresaContextSelection();
      }
    });
  }

  private applyFirstSelection(): void {
    // Do not auto-pick a non-default company on refresh/login.
    this.clearEmpresaContextSelection();
  }

  onEmpresaContextChange(rawId: string | number | null): void {
    const id = Number(rawId || 0);
    if (!id) return;
    this.setEmpresaContext(id);
  }

  private clearEmpresaContextSelection(): void {
    this.empresaContextId = null;
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');
  }

  private setEmpresaContext(id: number): void {
    const empresa = this.empresaContextOptions.find(e => e.id === id);
    if (!empresa) return;
    this.empresaContextId = empresa.id;
    localStorage.setItem('empresaContextId', String(empresa.id));
    localStorage.setItem('empresaContextTipo', empresa.tipo || '');
    localStorage.setItem('empresaContextNome', empresa.razaoSocial || '');
  }

  private clearEmpresaContext(): void {
    this.empresaContextOptions = [];
    this.empresaContextId = null;
    this.currentTenantId = '';
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');
  }

  private getTenantId(): string {
    return (localStorage.getItem('tenantId') || '').trim();
  }

  private isStaleTenantRequest(requestTenantId: string): boolean {
    const activeTenantId = this.getTenantId();
    return !requestTenantId || !activeTenantId || requestTenantId !== activeTenantId || this.currentTenantId !== requestTenantId;
  }

  isEmpresaPadraoSelecionada(): boolean {
    const selectedId = this.empresaContextId || 0;
    if (!selectedId) return false;
    const selected = this.empresaContextOptions.find(e => e.id === selectedId);
    return !!selected?.padrao;
  }

  empresaContextLabel(empresa: EmpresaResponse): string {
    if (!empresa) return '';
    return `${empresa.tipo === 'MATRIZ' ? 'Matriz' : 'Filial'} - ${empresa.razaoSocial}`;
  }

  private setUserIdentity(data?: any) {
    const fromApi = data?.username || data?.email || '';
    const fromToken = this.auth.getUsername() || '';
    const name = (fromApi || fromToken || '').trim();
    this.userName = name;
    this.loggedIn = !!name;
    this.userInitials = name ? name.slice(0, 2).toUpperCase() : 'U';
  }

  isActiveRoute(route: string): boolean {
    if (!route) return false;
    if (route === '/') return this.isSelectionRoute;
    return this.currentRoute === route || this.currentRoute.startsWith(route + '/');
  }

  getMenuLabel(item: MenuItem): string {
    return this.menuLabelAliases[item.id] || item.label;
  }

  canConfigureAccess(): boolean {
    return this.accessControl.canConfigure();
  }

  configureMenuAccess(item: MenuItem, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const key = `menu.${item.id}`;
    const current = this.accessControl.getRoles(key, item.roles || []);
    this.dialog.open(AccessControlConfigDialogComponent, {
      width: '460px',
      maxWidth: '92vw',
      data: {
        title: `Configurar acesso do menu "${this.getMenuLabel(item)}"`,
        controlKey: key,
        selectedRoles: current,
        fallbackRoles: item.roles || []
      }
    }).afterClosed().subscribe((roles: string[] | undefined) => {
      if (!roles) return;
      this.accessControl.setRoles(key, roles);
      this.refreshMenu();
      this.rebuildShortcutsTop();
    });
  }
}



