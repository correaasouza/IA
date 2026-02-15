import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, from, of } from 'rxjs';
import { switchMap, catchError, timeout } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return from(this.auth.isLoggedIn()).pipe(
      timeout(3000),
      catchError(() => of(false)),
      switchMap(loggedIn => {
        const tenantId = localStorage.getItem('tenantId');
        const empresaId = localStorage.getItem('empresaContextId');
        if (!loggedIn) {
          let headers = req.headers;
          if (tenantId) {
            headers = headers.set('X-Tenant-Id', tenantId);
          }
          if (empresaId) {
            headers = headers.set('X-Empresa-Id', empresaId);
          }
          return next.handle(req.clone({ headers }));
        }
        return from(this.auth.updateToken(30)).pipe(
          catchError(() => of(false)),
          switchMap(() => from(this.auth.getToken())),
          timeout(3000),
          catchError(() => of('')),
          switchMap(token => {
            if (!token) {
              let headers = req.headers;
              if (tenantId) {
                headers = headers.set('X-Tenant-Id', tenantId);
              }
              if (empresaId) {
                headers = headers.set('X-Empresa-Id', empresaId);
              }
              return next.handle(req.clone({ headers }));
            }
            const roles = this.auth.getUserRoles();
            localStorage.setItem('roles', JSON.stringify(roles));
            const userId = this.auth.getUserId() || '';
            localStorage.setItem('userId', userId);
            const requestId = (typeof crypto !== 'undefined' && 'randomUUID' in crypto)
              ? crypto.randomUUID()
              : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
            let headers = req.headers
              .set('Authorization', `Bearer ${token}`)
              .set('X-Request-Id', requestId);
            if (tenantId) {
              headers = headers.set('X-Tenant-Id', tenantId);
            }
            if (empresaId) {
              headers = headers.set('X-Empresa-Id', empresaId);
            }
            return next.handle(req.clone({ headers }));
          })
        );
      })
    );
  }
}
