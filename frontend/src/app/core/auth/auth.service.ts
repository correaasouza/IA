import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private keycloak: KeycloakService) {}

  login() {
    if (this.isE2ETestMode()) {
      return Promise.resolve();
    }
    return this.keycloak.login();
  }

  logout() {
    if (this.isE2ETestMode()) {
      return Promise.resolve();
    }
    return this.keycloak.logout(window.location.origin);
  }

  async isLoggedIn(): Promise<boolean> {
    if (this.isE2ETestMode()) {
      return true;
    }
    return this.keycloak.isLoggedIn();
  }

  getToken(): Promise<string> {
    if (this.isE2ETestMode()) {
      return Promise.resolve('');
    }
    return this.keycloak.getToken();
  }

  updateToken(minValiditySeconds = 30): Promise<boolean> {
    if (this.isE2ETestMode()) {
      return Promise.resolve(false);
    }
    return this.keycloak.updateToken(minValiditySeconds);
  }

  getUserRoles(): string[] {
    if (this.isE2ETestMode()) {
      return [];
    }
    return this.keycloak.getUserRoles();
  }

  getUserId(): string | undefined {
    if (this.isE2ETestMode()) {
      return 'e2e-user';
    }
    return this.keycloak.getKeycloakInstance().subject;
  }

  getUsername(): string | undefined {
    if (this.isE2ETestMode()) {
      return 'e2e';
    }
    return this.keycloak.getKeycloakInstance().tokenParsed?.['preferred_username'] as string | undefined;
  }

  private isE2ETestMode(): boolean {
    if (typeof window === 'undefined') return false;
    return !!(window as any).__E2E_TEST__;
  }
}
