import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
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

import { AuthService } from './core/auth/auth.service';
import { AtalhoService, AtalhoUsuario } from './core/atalhos/atalho.service';
import { MenuService, MenuItem } from './core/menu/menu.service';
import { AtalhoOrdenarDialogComponent } from './core/atalhos/atalho-ordenar-dialog.component';
import { IconService } from './core/menu/icon.service';
import { environment } from '../environments/environment';

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
    MatTooltipModule
  ],
  templateUrl: './app.component.html',
  
})
export class AppComponent {
  sidebarOpen = true;
  isMobile = false;
  menu: MenuItem[] = [];
  atalhos: AtalhoUsuario[] = [];
  atalhosTop: MenuItem[] = [];
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
  private readonly menuLabelAliases: Record<string, string> = {
    metadata: 'Tipos Ent.'
  };

  constructor(
    private auth: AuthService,
    private atalhoService: AtalhoService,
    private menuService: MenuService,
    private dialog: MatDialog,
    private breakpoint: BreakpointObserver,
    private iconService: IconService,
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
      }
    });
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  login() {
    this.auth.login();
  }

  logout() {
    this.auth.logout();
  }

  isShortcut(menuId: string): boolean {
    return this.atalhos.some(a => a.menuId === menuId);
  }

  toggleShortcut(item: MenuItem) {
    if (!item.route) return;
    const existing = this.atalhos.find(a => a.menuId === item.id);
    if (existing) {
      this.atalhos = this.atalhos.filter(a => a.id !== existing.id);
      this.rebuildShortcutsTop();
      this.atalhoService.delete(existing.id).subscribe({
        next: () => this.loadShortcuts()
      });
      return;
    }
    const ordem = this.atalhos.length + 1;
    this.atalhos = this.atalhos.concat({
      id: 0,
      menuId: item.id,
      icon: item.icon,
      ordem
    } as AtalhoUsuario);
    this.rebuildShortcutsTop();
    this.atalhoService.create({ menuId: item.id, icon: item.icon, ordem }).subscribe({
      next: () => this.loadShortcuts()
    });
  }

  loadShortcuts() {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId || this.isSelectionRoute) {
      this.atalhos = [];
      this.atalhosTop = [];
      this.hasTenant = false;
      return;
    }
    this.hasTenant = true;
    this.atalhoService.list().subscribe({
      next: data => {
        this.atalhos = data || [];
        this.rebuildShortcutsTop();
      }
    });
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
    const roles = [...this.auth.getUserRoles(), ...this.tenantRoles];
    const roleOk = !item.roles || item.roles.length === 0 || item.roles.some(r => roles.includes(r));
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
        this.setUserIdentity(data);
        this.refreshMenu();
        this.updateRouteFlags(this.router.url);
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
}

