import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    const loggedIn = await this.auth.isLoggedIn();
    if (!loggedIn) {
      await this.auth.login();
      return false;
    }
    const tenantId = localStorage.getItem('tenantId');
    const url = state?.url || '';
    if (!tenantId && url !== '/' && url !== '') {
      this.router.navigateByUrl('/');
      return false;
    }
    if (this.requiresMasterTenant(url) && !this.isMasterInMasterTenant()) {
      this.router.navigateByUrl('/home');
      return false;
    }
    return true;
  }

  private requiresMasterTenant(url: string): boolean {
    return url === '/tenants' || url.startsWith('/tenants/');
  }

  private isMasterInMasterTenant(): boolean {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (tenantId !== '1') {
      return false;
    }
    const roles = new Set([
      ...this.normalizeRoles(this.auth.getUserRoles()),
      ...this.tenantRolesFromStorage()
    ]);
    return roles.has('MASTER');
  }

  private tenantRolesFromStorage(): string[] {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (!tenantId) {
      return [];
    }
    const tenantKey = `tenantRoles:${tenantId}`;
    try {
      const raw = localStorage.getItem(tenantKey);
      const parsed = raw ? JSON.parse(raw) : [];
      return this.normalizeRoles(Array.isArray(parsed) ? parsed : []);
    } catch {
      return [];
    }
  }

  private normalizeRoles(values: string[]): string[] {
    return (values || [])
      .map(value => (value || '').trim().toUpperCase())
      .filter(value => value.length > 0)
      .map(value => value.startsWith('ROLE_') ? value.substring(5) : value);
  }
}
