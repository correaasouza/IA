import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private baseColunas = `${environment.apiBaseUrl}/api/config/colunas`;
  private baseForm = `${environment.apiBaseUrl}/api/config/formularios`;

  constructor(private http: HttpClient) {}

  getColunas(screenId: string, userId?: string, roles?: string): Observable<any> {
    const cacheKey = `cfg:col:${screenId}:${userId || ''}:${roles || ''}`;
    const etagKey = `cfg:col:etag:${screenId}:${userId || ''}:${roles || ''}`;
    const cached = localStorage.getItem(cacheKey);
    const etag = localStorage.getItem(etagKey) || '';
    const params = new URLSearchParams({ screenId });
    if (userId) params.set('userId', userId);
    if (roles) params.set('roles', roles);

    const headers = etag ? new HttpHeaders({ 'If-None-Match': etag }) : undefined;
    return this.http.get(`${this.baseColunas}?${params.toString()}`, { headers, observe: 'response' }).pipe(
      tap(resp => {
        if (resp.status === 200) {
          localStorage.setItem(cacheKey, JSON.stringify(resp.body));
          const newEtag = resp.headers.get('ETag');
          if (newEtag) localStorage.setItem(etagKey, newEtag);
        }
      }),
      (source) => new Observable(observer => {
        source.subscribe({
          next: resp => {
            if (resp.status === 304 && cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.next(resp.body);
            observer.complete();
          },
          error: err => {
            if (cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.error(err);
          }
        });
      })
    );
  }

  saveColunas(payload: any): Observable<void> {
    return this.http.post<void>(this.baseColunas, payload);
  }

  getForm(screenId: string, userId?: string, roles?: string): Observable<any> {
    const cacheKey = `cfg:form:${screenId}:${userId || ''}:${roles || ''}`;
    const etagKey = `cfg:form:etag:${screenId}:${userId || ''}:${roles || ''}`;
    const cached = localStorage.getItem(cacheKey);
    const etag = localStorage.getItem(etagKey) || '';
    const params = new URLSearchParams({ screenId });
    if (userId) params.set('userId', userId);
    if (roles) params.set('roles', roles);

    const headers = etag ? new HttpHeaders({ 'If-None-Match': etag }) : undefined;
    return this.http.get(`${this.baseForm}?${params.toString()}`, { headers, observe: 'response' }).pipe(
      tap(resp => {
        if (resp.status === 200) {
          localStorage.setItem(cacheKey, JSON.stringify(resp.body));
          const newEtag = resp.headers.get('ETag');
          if (newEtag) localStorage.setItem(etagKey, newEtag);
        }
      }),
      (source) => new Observable(observer => {
        source.subscribe({
          next: resp => {
            if (resp.status === 304 && cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.next(resp.body);
            observer.complete();
          },
          error: err => {
            if (cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.error(err);
          }
        });
      })
    );
  }

  saveForm(payload: any): Observable<void> {
    return this.http.post<void>(this.baseForm, payload);
  }
}
