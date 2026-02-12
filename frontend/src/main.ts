import 'zone.js';
import './twind';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { APP_INITIALIZER, LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { KeycloakService } from 'keycloak-angular';
import { importProvidersFrom } from '@angular/core';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { keycloakInitializer } from './app/core/auth/keycloak-init';
import { AuthInterceptor } from './app/core/interceptors/auth.interceptor';
import { BlockingInterceptor } from './app/core/interceptors/blocking.interceptor';
import { ErrorInterceptor } from './app/core/interceptors/error.interceptor';
import { SanitizeInterceptor } from './app/core/interceptors/sanitize.interceptor';

registerLocaleData(localePt);

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideHttpClient(withInterceptorsFromDi()),
    importProvidersFrom(MatSnackBarModule),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: keycloakInitializer,
      deps: [KeycloakService],
      multi: true
    },
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: SanitizeInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: BlockingInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ]
}).catch(err => console.error(err));
