import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AtalhoUsuario {
  id: number;
  menuId: string;
  icon?: string;
  ordem: number;
}

@Injectable({ providedIn: 'root' })
export class AtalhoService {
  private baseUrl = `${environment.apiBaseUrl}/api/atalhos`;

  constructor(private http: HttpClient) {}

  private tenantHeaders(): HttpHeaders {
    const tenantId = localStorage.getItem('tenantId');
    return tenantId ? new HttpHeaders({ 'X-Tenant-Id': tenantId }) : new HttpHeaders();
  }

  list(): Observable<AtalhoUsuario[]> {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId) {
      return of([]);
    }
    const params = new HttpParams().set('_ts', String(Date.now()));
    return this.http.get<AtalhoUsuario[]>(this.baseUrl, {
      headers: this.tenantHeaders(),
      params
    });
  }

  create(payload: any): Observable<AtalhoUsuario> {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId) {
      return throwError(() => new Error('tenant_required'));
    }
    return this.http.post<AtalhoUsuario>(this.baseUrl, payload, { headers: this.tenantHeaders() });
  }

  delete(id: number): Observable<void> {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId) {
      return throwError(() => new Error('tenant_required'));
    }
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { headers: this.tenantHeaders() });
  }

  reorder(items: { id: number; ordem: number }[]): Observable<void> {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId) {
      return throwError(() => new Error('tenant_required'));
    }
    return this.http.put<void>(`${this.baseUrl}/ordem`, items, { headers: this.tenantHeaders() });
  }
}
