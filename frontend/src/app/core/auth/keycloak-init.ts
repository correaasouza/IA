import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

export function keycloakInitializer(keycloak: KeycloakService) {
  return async () => {
    if (isE2ETestMode()) {
      return true;
    }
    return keycloak.init({
    config: {
      url: environment.keycloakUrl,
      realm: environment.keycloakRealm,
      clientId: environment.keycloakClientId
    },
    initOptions: {
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false
    },
    enableBearerInterceptor: false
    });
  };
}

function isE2ETestMode(): boolean {
  if (typeof window === 'undefined') return false;
  return !!(window as any).__E2E_TEST__;
}
