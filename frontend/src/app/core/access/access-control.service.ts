import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

interface AccessControlPolicyDto {
  controlKey: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AccessControlService {
  private policies: Record<string, string[]> = {};
  private changesSubject = new BehaviorSubject<number>(0);
  readonly changes$ = this.changesSubject.asObservable();
  private baseUrl = `${environment.apiBaseUrl}/api/access-controls`;

  constructor(private auth: AuthService, private http: HttpClient) {
    this.load();
    this.refreshPolicies();
  }

  can(controlKey: string, fallbackRoles: string[] = []): boolean {
    if (this.isMasterTenantContext()) return true;
    const key = this.normalizeKey(controlKey);
    if (!key) return true;
    const forced = this.forcedRoles(key);
    const configured = this.policies[key];
    const effective = forced ?? (configured !== undefined ? configured : this.normalize(fallbackRoles));
    if (!effective.length) return true;
    const current = this.currentRoles();
    return effective.some(r => current.includes(r));
  }

  canConfigure(): boolean {
    if (this.isMasterTenantContext()) return true;
    const roles = this.currentRoles();
    return roles.includes('MASTER') || roles.includes('ADMIN');
  }

  securityButtonsVisible(): boolean {
    if (!this.canConfigure()) {
      return false;
    }
    return this.loadSecurityButtonsPreference();
  }

  setSecurityButtonsVisible(visible: boolean): void {
    localStorage.setItem(this.securityButtonsStorageKey(), visible ? '1' : '0');
    this.changesSubject.next(Date.now());
  }

  getRoles(controlKey: string, fallbackRoles: string[] = []): string[] {
    const key = this.normalizeKey(controlKey);
    const forced = this.forcedRoles(key);
    if (forced) {
      return forced;
    }
    const configured = this.policies[key];
    return configured !== undefined ? configured : this.normalize(fallbackRoles);
  }

  setRoles(controlKey: string, roles: string[]): void {
    const key = this.normalizeKey(controlKey);
    if (!key) return;
    this.policies[key] = this.normalize(roles);
    this.persist();
    this.changesSubject.next(Date.now());
    this.http.put<AccessControlPolicyDto>(
      `${this.baseUrl}/${encodeURIComponent(key)}`,
      { roles: this.policies[key] }
    ).subscribe({
      next: () => this.refreshPolicies(),
      error: () => {}
    });
  }

  deletePolicy(controlKey: string): void {
    const key = this.normalizeKey(controlKey);
    if (!key) return;
    delete this.policies[key];
    this.persist();
    this.changesSubject.next(Date.now());
    this.http.delete<void>(`${this.baseUrl}/${encodeURIComponent(key)}`).subscribe({
      next: () => this.refreshPolicies(),
      error: () => {}
    });
  }

  listPolicies(): Array<{ controlKey: string; roles: string[] }> {
    return Object.entries(this.policies)
      .map(([controlKey, roles]) => ({ controlKey, roles: [...roles] }))
      .sort((a, b) => a.controlKey.localeCompare(b.controlKey));
  }

  refreshPolicies(): void {
    this.http.get<AccessControlPolicyDto[]>(this.baseUrl).subscribe({
      next: (items) => {
        const next: Record<string, string[]> = {};
        (items || []).forEach(item => {
          const key = (item?.controlKey || '').trim();
          if (!key) return;
          next[key] = this.normalize(item.roles || []);
        });
        this.policies = next;
        this.persist();
        this.changesSubject.next(Date.now());
      },
      error: () => {}
    });
  }

  private currentRoles(): string[] {
    const tokenRoles = this.normalize(this.auth.getUserRoles() || []);
    let tenantRoles: string[] = [];
    try {
      const raw = localStorage.getItem('tenantRoles');
      tenantRoles = raw ? JSON.parse(raw) : [];
    } catch {
      tenantRoles = [];
    }
    return Array.from(new Set([...tokenRoles, ...this.normalize(tenantRoles)]));
  }

  private isMasterTenantContext(): boolean {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    if (tenantId !== '1') {
      return false;
    }
    return this.currentRoles().includes('MASTER');
  }

  private normalize(values: string[]): string[] {
    return Array.from(new Set((values || [])
      .map(v => (v || '').trim().toUpperCase())
      .filter(v => v.length > 0)));
  }

  private forcedRoles(controlKey: string): string[] | null {
    if (controlKey === 'menu.roles') {
      return ['MASTER'];
    }
    return null;
  }

  private normalizeKey(value: string): string {
    return (value || '').trim().toLowerCase();
  }

  private storageKey(): string {
    const tenantId = localStorage.getItem('tenantId') || 'global';
    return `acl:controls:${tenantId}`;
  }

  private securityButtonsStorageKey(): string {
    const tenantId = localStorage.getItem('tenantId') || 'global';
    return `acl:security-buttons-visible:${tenantId}`;
  }

  private loadSecurityButtonsPreference(): boolean {
    const raw = (localStorage.getItem(this.securityButtonsStorageKey()) || '').trim();
    if (!raw) {
      return true;
    }
    return raw !== '0';
  }

  private load(): void {
    try {
      const raw = localStorage.getItem(this.storageKey());
      this.policies = raw ? JSON.parse(raw) : {};
    } catch {
      this.policies = {};
    }
  }

  private persist(): void {
    localStorage.setItem(this.storageKey(), JSON.stringify(this.policies));
  }
}

