import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private keycloak: KeycloakService) {}

  login() {
    return this.keycloak.login();
  }

  logout() {
    return this.keycloak.logout(window.location.origin);
  }

  async isLoggedIn(): Promise<boolean> {
    return this.keycloak.isLoggedIn();
  }

  getToken(): Promise<string> {
    return this.keycloak.getToken();
  }

  updateToken(minValiditySeconds = 30): Promise<boolean> {
    return this.keycloak.updateToken(minValiditySeconds);
  }

  getUserRoles(): string[] {
    return this.keycloak.getUserRoles();
  }

  getUserId(): string | undefined {
    return this.keycloak.getKeycloakInstance().subject;
  }

  getUsername(): string | undefined {
    return this.keycloak.getKeycloakInstance().tokenParsed?.['preferred_username'] as string | undefined;
  }
}
