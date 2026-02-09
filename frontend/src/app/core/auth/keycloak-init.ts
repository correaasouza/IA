import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

export function keycloakInitializer(keycloak: KeycloakService) {
  return async () => keycloak.init({
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
}
