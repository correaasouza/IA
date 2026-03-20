import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class MasterOnlyGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.isMasterInMasterTenant()) {
      return true;
    }
    this.router.navigateByUrl('/home');
    return false;
  }

  private isMasterInMasterTenant(): boolean {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (tenantId !== '1') {
      return false;
    }
    const username = (this.auth.getUsername() || '').trim().toLowerCase();
    if (username === 'master') {
      return true;
    }
    const roles = new Set([
      ...this.normalizeRoles(this.auth.getUserRoles()),
      ...this.readTenantRoles(tenantId)
    ]);
    return roles.has('MASTER');
  }

  private readTenantRoles(tenantId: string): string[] {
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

  private normalizeRoles(values: string[]): string[] {
    return (values || [])
      .map(value => (value || '').trim().toUpperCase())
      .filter(value => value.length > 0)
      .map(value => value.startsWith('ROLE_') ? value.substring(5) : value);
  }
}
