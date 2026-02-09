const host =
  typeof window !== 'undefined' && window.location && window.location.hostname
    ? window.location.hostname
    : 'localhost';

export const environment = {
  production: false,
  apiBaseUrl: `http://${host}:8082`,
  keycloakUrl: `http://${host}:8081`,
  keycloakRealm: 'ia',
  keycloakClientId: 'frontend'
};
