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
import { EntityTypeService, TipoEntidade } from './features/entity-types/entity-type.service';
import { MovementConfigService, MovimentoTipoOption } from './features/movement-configs/movement-config.service';

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
  sidebarOpen = false;
  isMobile = false;
  menu: MenuItem[] = [];
  allMenu: MenuItem[] = [];
  private dynamicEntityTypeItems: MenuItem[] = [];
  private dynamicMovementTypeItems: MenuItem[] = [];
  private loadingEntityTypeItems = false;
  private loadingMovementTypeItems = false;
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
  private readonly menuModeKey = 'menu:mode';
  private featureFlags: Record<string, boolean> = {};
  loggedIn = false;
  userName = '';
  userInitials = 'U';
  empresaContextOptions: EmpresaResponse[] = [];
  empresaContextId: number | null = null;
  loadingEmpresaContext = false;
  private loadingDefaultEmpresaId = 0;
  private currentTenantId = '';
  private securityTenantId = '';
  private readonly shortcutRemoving = new Set<string>();
  private readonly shortcutCreating = new Set<string>();
  menuMode: 'operacao' | 'configuracao' = 'operacao';
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
    private entityTypeService: EntityTypeService,
    private companyService: CompanyService,
    private movementConfigService: MovementConfigService,
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
    const cachedMenuMode = localStorage.getItem(this.menuModeKey);
    if (cachedMenuMode === 'operacao' || cachedMenuMode === 'configuracao') {
      this.menuMode = cachedMenuMode;
    }
    this.loadFeatureFlags();
    this.refreshMenu();
    this.updateRouteFlags(this.router.url);
    this.loadShortcuts();
    this.loadMe();
    this.refreshEmpresaContextIfNeeded();
    this.accessControl.changes$.subscribe(() => {
      if (this.isSelectionRoute) {
        return;
      }
      this.refreshMenu();
      this.rebuildShortcutsTop();
    });

    this.breakpoint.observe(['(max-width: 900px)']).subscribe(result => {
      this.isMobile = result.matches;
      if (result.matches) {
        this.sidebarOpen = false;
      }
    });

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(e => {
      const url = (e as NavigationEnd).urlAfterRedirects || (e as NavigationEnd).url;
      this.updateRouteFlags(url);
      if (this.isMobile && this.sidebarOpen) {
        this.sidebarOpen = false;
      }
      if (!this.isSelectionRoute && localStorage.getItem('tenantId')) {
        this.reloadSecurityContextIfNeeded();
        this.refreshMenu();
        this.loadShortcuts();
        this.refreshEmpresaContextIfNeeded();
      }
    });
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  @HostListener('window:empresa-context-updated', ['$event'])
  onEmpresaContextUpdated(event?: CustomEvent<{ source?: string }>): void {
    const tenantId = this.getTenantId();
    if (!tenantId || this.isSelectionRoute) {
      this.clearEmpresaContext();
      return;
    }
    // When the event comes from the company selector itself, avoid reloading options
    // to prevent forcing the selection back to the default company.
    if (event?.detail?.source === 'selector') {
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
    const allowed = this.flattenMenu(this.allMenu.length ? this.allMenu : this.menu);
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
    return !!item.children?.some(child => this.isMenuItemActive(child));
  }

  private isAllowedLeaf(item: MenuItem): boolean {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId && item.id !== 'home') {
      return false;
    }
    // Hard rule: "Locatários" is visible only for MASTER inside master tenant.
    if (item.id === 'tenants') {
      return this.isMasterTenantContext() && this.hasMasterRole();
    }
    const controlKey = item.accessKey || `menu.${item.id}`;
    if (item.id === 'movement-configs' && !this.isFeatureEnabled('movementConfigEnabled', true)) {
      return false;
    }
    const roleOk = !item.roles || item.roles.length === 0
      || this.accessControl.can(controlKey, item.roles);
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
        this.storeFeatureFlags(data?.features);
        localStorage.removeItem('tenantRoles');
        const apiTenantId = data?.tenantId ? String(data.tenantId) : '';
        if (apiTenantId && !localStorage.getItem('tenantId')) {
          localStorage.setItem('tenantId', apiTenantId);
        }
        const tenantStorageId = (localStorage.getItem('tenantId') || apiTenantId || '').trim();
        if (tenantStorageId) {
          localStorage.setItem(this.tenantRolesStorageKey(tenantStorageId), JSON.stringify(this.tenantRoles));
          this.securityTenantId = tenantStorageId;
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
        this.storeFeatureFlags({});
      }
    });
  }

  private reloadSecurityContextIfNeeded(): void {
    const tenantId = this.getTenantId();
    if (!tenantId) {
      this.securityTenantId = '';
      return;
    }
    if (this.securityTenantId === tenantId) {
      return;
    }
    this.loadMe();
  }

  refreshMenu() {
    this.rebuildMenuTree();
    this.loadEntityTypeMenuItems();
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

  private filterMenuByMode(items: MenuItem[]): MenuItem[] {
    return (items || []).filter(item => {
      if (this.menuMode === 'operacao') {
        return item.id === 'group-cadastros' || item.id === 'group-stock';
      }
      if (item.id === 'group-globals') {
        return this.isMasterTenantContext();
      }
      return item.id === 'group-access' || item.id === 'group-config';
    });
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

  toggleMenuMode(): void {
    this.menuMode = this.menuMode === 'operacao' ? 'configuracao' : 'operacao';
    localStorage.setItem(this.menuModeKey, this.menuMode);
    this.refreshMenu();
  }

  menuModeLabel(): string {
    return this.menuMode === 'operacao' ? 'Operação' : 'Configuração';
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
    const previousId = this.empresaContextId || 0;
    this.setEmpresaContext(id);
    if (previousId !== id) {
      this.notifyEmpresaContextUpdated('selector');
    }
  }

  private clearEmpresaContextSelection(): void {
    this.empresaContextId = null;
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');
    this.setDynamicMovementTypeItems([]);
  }

  private setEmpresaContext(id: number): void {
    const empresa = this.empresaContextOptions.find(e => e.id === id);
    if (!empresa) return;
    this.empresaContextId = empresa.id;
    localStorage.setItem('empresaContextId', String(empresa.id));
    localStorage.setItem('empresaContextTipo', empresa.tipo || '');
    localStorage.setItem('empresaContextNome', empresa.razaoSocial || '');
    this.loadMovementTypeMenuItems();
  }

  private notifyEmpresaContextUpdated(source: 'selector' | 'external' = 'external'): void {
    if (typeof window === 'undefined') return;
    window.dispatchEvent(new CustomEvent('empresa-context-updated', { detail: { source } }));
  }

  private clearEmpresaContext(): void {
    this.empresaContextOptions = [];
    this.empresaContextId = null;
    this.currentTenantId = '';
    this.setDynamicMovementTypeItems([]);
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');
  }

  private getTenantId(): string {
    return (localStorage.getItem('tenantId') || '').trim();
  }

  private tenantRolesStorageKey(tenantId: string): string {
    return `tenantRoles:${tenantId}`;
  }

  private isMasterTenantContext(): boolean {
    return this.getTenantId() === '1';
  }

  private hasMasterRole(): boolean {
    const normalize = (role: string) => {
      const value = (role || '').trim().toUpperCase();
      return value.startsWith('ROLE_') ? value.substring(5) : value;
    };
    const tokenRoles = (this.auth.getUserRoles() || []).map(normalize);
    const tenantRoles = (this.tenantRoles || []).map(normalize);
    return Array.from(new Set([...tokenRoles, ...tenantRoles])).includes('MASTER');
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
    const currentPath = this.currentPath();
    return currentPath === route || currentPath.startsWith(route + '/');
  }

  isMenuItemActive(item: MenuItem): boolean {
    if (!item?.route) return false;
    if (!this.isActiveRoute(item.route)) return false;
    const expected = item.queryParams || {};
    const expectedKeys = Object.keys(expected);
    if (expectedKeys.length === 0) return true;
    const currentQuery = this.currentQueryParams();
    return expectedKeys.every(key => String(currentQuery[key] ?? '') === String(expected[key]));
  }

  menuQueryParams(item: MenuItem): Record<string, string | number | boolean> | null {
    return item.queryParams || null;
  }

  private currentPath(): string {
    const url = this.currentRoute || this.router.url || '';
    const path = url.split('?')[0]?.split('#')[0] || '';
    return path || '/';
  }

  private currentQueryParams(): Record<string, unknown> {
    try {
      const parsed = this.router.parseUrl(this.currentRoute || this.router.url || '/');
      return parsed.queryParams || {};
    } catch {
      return {};
    }
  }

  getMenuLabel(item: MenuItem): string {
    return this.menuLabelAliases[item.id] || item.label;
  }

  canConfigureAccess(): boolean {
    return this.accessControl.canConfigure();
  }

  isSecurityButtonsVisible(): boolean {
    return this.accessControl.securityButtonsVisible();
  }

  toggleSecurityButtonsVisibility(): void {
    this.accessControl.setSecurityButtonsVisible(!this.isSecurityButtonsVisible());
  }

  configureMenuAccess(item: MenuItem, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const key = item.accessKey || `menu.${item.id}`;
    const current = this.accessControl.getRoles(key, item.roles || []);
    this.dialog.open(AccessControlConfigDialogComponent, {
      width: '460px',
      maxWidth: '92vw',
      data: {
        title: `Configurar acesso para ${this.getMenuLabel(item)}`,
        description: this.getMenuLabel(item),
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

  private menuSource(): MenuItem[] {
    const source = (this.menuService.items || []).map(item => ({
      ...item,
      children: item.children ? [...item.children] : undefined
    }));
    const cadastros = source.find(item => item.id === 'group-cadastros');
    if (cadastros) {
      const staticChildren = cadastros.children || [];
      const staticNonEntityChildren = staticChildren.filter(child => !child.id.startsWith('entities-'));
      const staticEntityChildren = staticChildren.filter(child => child.id.startsWith('entities-'));
      const entityChildren = this.dynamicEntityTypeItems.length > 0
        ? this.dynamicEntityTypeItems
        : staticEntityChildren;
      cadastros.children = [...entityChildren, ...staticNonEntityChildren];
    }
    const estoque = source.find(item => item.id === 'group-stock');
    if (estoque && this.dynamicMovementTypeItems.length > 0) {
      const staticChildren = estoque.children || [];
      const semMovimentoEstatico = staticChildren.filter(child => child.id !== 'stock-movements');
      estoque.children = [...semMovimentoEstatico, ...this.dynamicMovementTypeItems];
    }
    return source;
  }

  private loadEntityTypeMenuItems(): void {
    const tenantId = this.getTenantId();
    if (!tenantId || this.isSelectionRoute || this.loadingEntityTypeItems) {
      return;
    }
    this.loadingEntityTypeItems = true;
    this.entityTypeService.list({ page: 0, size: 300, ativo: true })
      .pipe(finalize(() => (this.loadingEntityTypeItems = false)))
      .subscribe({
        next: data => {
          const types = (data?.content || []).filter(t => t.ativo);
          this.dynamicEntityTypeItems = this.mapEntityTypesToMenu(types);
          this.rebuildMenuTree();
        },
        error: () => {
          // keep static fallback menu on failures
        }
      });
  }

  private loadMovementTypeMenuItems(): void {
    const tenantId = this.getTenantId();
    const empresaId = this.empresaContextId || 0;
    if (!tenantId || this.isSelectionRoute || !empresaId || !this.isFeatureEnabled('movementConfigEnabled', true)) {
      this.setDynamicMovementTypeItems([]);
      return;
    }
    if (this.loadingMovementTypeItems) {
      return;
    }
    this.loadingMovementTypeItems = true;
    const requestTenantId = tenantId;
    const requestEmpresaId = empresaId;
    this.movementConfigService.listMenuByEmpresa(empresaId)
      .pipe(finalize(() => (this.loadingMovementTypeItems = false)))
      .subscribe({
        next: types => {
          if (this.isStaleTenantRequest(requestTenantId) || (this.empresaContextId || 0) !== requestEmpresaId) {
            return;
          }
          this.setDynamicMovementTypeItems(this.mapMovementTypesToMenu(types || []));
        },
        error: () => {
          if (this.isStaleTenantRequest(requestTenantId) || (this.empresaContextId || 0) !== requestEmpresaId) {
            return;
          }
          this.setDynamicMovementTypeItems([]);
        }
      });
  }

  private mapMovementTypesToMenu(types: MovimentoTipoOption[]): MenuItem[] {
    return (types || []).map(type => {
      const codigo = (type?.codigo || '').trim().toUpperCase();
      const label = (type?.descricao || codigo || 'Movimento').trim();
      const route = codigo === 'MOVIMENTO_ESTOQUE' ? '/movimentos/estoque' : undefined;
      return {
        id: `movement-action-${codigo.toLowerCase()}`,
        label,
        icon: codigo === 'MOVIMENTO_ESTOQUE' ? 'inventory' : 'play_circle',
        route,
        accessKey: `menu.movement.action.${codigo.toLowerCase()}`,
        roles: ['MASTER', 'ADMIN'],
        perms: codigo === 'MOVIMENTO_ESTOQUE' ? ['MOVIMENTO_ESTOQUE_OPERAR'] : undefined
      } as MenuItem;
    });
  }

  private setDynamicMovementTypeItems(items: MenuItem[]): void {
    const normalized = (items || []).map(item => ({ ...item }));
    const current = JSON.stringify(this.dynamicMovementTypeItems);
    const next = JSON.stringify(normalized);
    if (current === next) {
      return;
    }
    this.dynamicMovementTypeItems = normalized;
    this.rebuildMenuTree();
  }

  private rebuildMenuTree(): void {
    this.allMenu = this.normalizeMenu(this.menuSource());
    this.menu = this.filterMenuByMode(this.allMenu);
    this.expandForRoute();
    this.rebuildShortcutsTop();
  }

  private mapEntityTypesToMenu(types: TipoEntidade[]): MenuItem[] {
    const orderBySeed: Record<string, number> = {
      CLIENTE: 1,
      FORNECEDOR: 2,
      EQUIPE: 3
    };
    return [...(types || [])]
      .sort((a, b) => {
        const seedA = (a.codigoSeed || '').trim().toUpperCase();
        const seedB = (b.codigoSeed || '').trim().toUpperCase();
        const orderA = orderBySeed[seedA] ?? 999;
        const orderB = orderBySeed[seedB] ?? 999;
        if (orderA !== orderB) return orderA - orderB;
        return (a.nome || '').localeCompare(b.nome || '');
      })
      .map(type => this.toEntityTypeMenuItem(type));
  }

  private toEntityTypeMenuItem(type: TipoEntidade): MenuItem {
    const seed = (type.codigoSeed || '').trim().toUpperCase();
    if (seed === 'CLIENTE') {
      return {
        id: 'entities-clientes',
        label: this.formatTipoMenuLabel(type.nome || 'Clientes'),
        route: '/entities',
        queryParams: { tipoEntidadeId: type.id, tipoSeed: 'CLIENTE' },
        accessKey: 'menu.entities-clientes',
        icon: 'badge',
        roles: ['MASTER', 'ADMIN'],
        perms: ['ENTIDADE_EDIT']
      };
    }
    if (seed === 'FORNECEDOR') {
      return {
        id: 'entities-fornecedores',
        label: this.formatTipoMenuLabel(type.nome || 'Fornecedores'),
        route: '/entities',
        queryParams: { tipoEntidadeId: type.id, tipoSeed: 'FORNECEDOR' },
        accessKey: 'menu.entities-fornecedores',
        icon: 'local_shipping',
        roles: ['MASTER', 'ADMIN'],
        perms: ['ENTIDADE_EDIT']
      };
    }
    if (seed === 'EQUIPE') {
      return {
        id: 'entities-equipe',
        label: this.formatTipoMenuLabel(type.nome || 'Equipe'),
        route: '/entities',
        queryParams: { tipoEntidadeId: type.id, tipoSeed: 'EQUIPE' },
        accessKey: 'menu.entities-equipe',
        icon: 'groups',
        roles: ['MASTER', 'ADMIN'],
        perms: ['ENTIDADE_EDIT']
      };
    }
    return {
      id: `entities-tipo-${type.id}`,
      label: this.formatTipoMenuLabel(type.nome || `Tipo ${type.id}`),
      route: '/entities',
      queryParams: { tipoEntidadeId: type.id },
      accessKey: `menu.entities.tipo.${type.id}`,
      icon: 'view_list',
      roles: ['MASTER', 'ADMIN'],
      perms: ['ENTIDADE_EDIT']
    };
  }

  private formatTipoMenuLabel(value: string): string {
    const raw = (value || '').trim();
    if (!raw) return 'Entidades';
    if (raw === raw.toUpperCase()) {
      return raw
        .toLowerCase()
        .split(' ')
        .filter(part => !!part)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
    }
    return raw;
  }

  private isFeatureEnabled(key: string, defaultValue: boolean): boolean {
    const normalized = (key || '').trim();
    if (!normalized) {
      return defaultValue;
    }
    if (!(normalized in this.featureFlags)) {
      return defaultValue;
    }
    return !!this.featureFlags[normalized];
  }

  private loadFeatureFlags(): void {
    try {
      const raw = localStorage.getItem('featureFlags');
      const parsed = raw ? JSON.parse(raw) : {};
      if (!parsed || typeof parsed !== 'object') {
        this.featureFlags = {};
        return;
      }
      const next: Record<string, boolean> = {};
      for (const [key, value] of Object.entries(parsed)) {
        next[key] = !!value;
      }
      this.featureFlags = next;
    } catch {
      this.featureFlags = {};
    }
  }

  private storeFeatureFlags(flags: any): void {
    if (!flags || typeof flags !== 'object') {
      this.featureFlags = {};
      localStorage.removeItem('featureFlags');
      return;
    }
    const next: Record<string, boolean> = {};
    for (const [key, value] of Object.entries(flags)) {
      next[key] = !!value;
    }
    this.featureFlags = next;
    localStorage.setItem('featureFlags', JSON.stringify(next));
  }
}



