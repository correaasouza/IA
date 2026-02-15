import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmpresaPadraoResponse {
  empresaId: number | null;
}

@Injectable({ providedIn: 'root' })
export class CompanyContextService {
  private baseUrl = `${environment.apiBaseUrl}/api/me/empresa-padrao`;

  constructor(private http: HttpClient) {}

  private tenantHeaders(): HttpHeaders {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    return tenantId ? new HttpHeaders({ 'X-Tenant-Id': tenantId }) : new HttpHeaders();
  }

  getDefault(): Observable<EmpresaPadraoResponse> {
    const params = new HttpParams().set('_ts', String(Date.now()));
    return this.http.get<EmpresaPadraoResponse>(this.baseUrl, {
      params,
      headers: this.tenantHeaders()
    });
  }

  setDefault(empresaId: number): Observable<EmpresaPadraoResponse> {
    return this.http.put<EmpresaPadraoResponse>(this.baseUrl, { empresaId }, {
      headers: this.tenantHeaders()
    });
  }

  clearDefault(): Observable<void> {
    return this.http.delete<void>(this.baseUrl, {
      headers: this.tenantHeaders()
    });
  }
}
