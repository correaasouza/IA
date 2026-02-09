import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { NotificationService } from '../notifications/notification.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private notify: NotificationService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401 && error.status !== 403 && error.status !== 423) {
          const detail = (error.error && (error.error.detail || error.error.message)) || error.message;
          this.notify.error(detail || 'Erro inesperado.');
        }
        return throwError(() => error);
      })
    );
  }
}
