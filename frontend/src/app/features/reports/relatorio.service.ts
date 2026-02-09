import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private baseUrl = `${environment.apiBaseUrl}/api/relatorios`;

  constructor(private http: HttpClient) {}

  entidades(params?: { entidadeDefinicaoId?: number; criadoDe?: string; criadoAte?: string }): Observable<any> {
    const qs = new URLSearchParams();
    if (params?.entidadeDefinicaoId) qs.set('entidadeDefinicaoId', String(params.entidadeDefinicaoId));
    if (params?.criadoDe) qs.set('criadoDe', params.criadoDe);
    if (params?.criadoAte) qs.set('criadoAte', params.criadoAte);
    const q = qs.toString();
    return this.http.get(`${this.baseUrl}/entidades${q ? '?' + q : ''}`);
  }

  entidadesComparativo(params?: { criadoDe1?: string; criadoAte1?: string; criadoDe2?: string; criadoAte2?: string }): Observable<any> {
    const qs = new URLSearchParams();
    if (params?.criadoDe1) qs.set('criadoDe1', params.criadoDe1);
    if (params?.criadoAte1) qs.set('criadoAte1', params.criadoAte1);
    if (params?.criadoDe2) qs.set('criadoDe2', params.criadoDe2);
    if (params?.criadoAte2) qs.set('criadoAte2', params.criadoAte2);
    const q = qs.toString();
    return this.http.get(`${this.baseUrl}/entidades-comparativo${q ? '?' + q : ''}`);
  }

  contatos(): Observable<any> {
    return this.http.get(`${this.baseUrl}/contatos`);
  }

  pendenciasContato(): Observable<any> {
    return this.http.get(`${this.baseUrl}/pendencias-contato`);
  }

  locatarios(): Observable<any> {
    return this.http.get(`${this.baseUrl}/locatarios`);
  }
}
