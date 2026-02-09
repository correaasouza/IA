import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
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
    MatDialogModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  sidebarOpen = true;
  menu: MenuItem[] = [];
  atalhos: AtalhoUsuario[] = [];
  atalhosTop: MenuItem[] = [];
  permissions: string[] = [];
  tenantRoles: string[] = [];
  hasTenant = false;
  isSelectionRoute = true;
  currentRoute = '';

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
    this.refreshMenu();
    this.updateRouteFlags(this.router.url);
    this.loadShortcuts();
    this.loadMe();

    this.breakpoint.observe(['(max-width: 900px)']).subscribe(result => {
      this.sidebarOpen = !result.matches;
    });

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(e => {
      const url = (e as NavigationEnd).urlAfterRedirects || (e as NavigationEnd).url;
      this.updateRouteFlags(url);
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
    const allowed = this.menu.filter(m => this.allowedMenu(m));
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

  allowedMenu(item: MenuItem): boolean {
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
        this.refreshMenu();
        this.updateRouteFlags(this.router.url);
        if (!this.isSelectionRoute) {
          this.loadShortcuts();
        }
        const tenantId = localStorage.getItem('tenantId');
        if (!tenantId) {
          this.router.navigateByUrl('/');
        }
      }
    });
  }

  refreshMenu() {
    this.menu = this.menuService.items.filter(m => this.allowedMenu(m)).map(m => ({
      ...m,
      icon: this.iconService.resolveIcon(m.id, m.icon)
    }));
  }

  private updateRouteFlags(url: string) {
    this.isSelectionRoute = url === '/' || url === '';
    const tenantId = localStorage.getItem('tenantId');
    this.hasTenant = !!tenantId && !this.isSelectionRoute;
    this.currentRoute = url;
  }

  isActiveRoute(route: string): boolean {
    if (!route) return false;
    if (route === '/') return this.isSelectionRoute;
    return this.currentRoute === route || this.currentRoute.startsWith(route + '/');
  }
}
