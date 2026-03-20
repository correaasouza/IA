import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RhQualificacao {
  id: number;
  nome: string;
  completo: boolean;
  tipo?: string | null;
  ativo: boolean;
}

export interface RhQualificacaoPayload {
  nome: string;
  completo?: boolean | null;
  tipo?: string | null;
  ativo?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class RhQualificacoesService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/rh/qualificacoes`;

  constructor(private readonly http: HttpClient) {}

  list(filters?: { text?: string; ativo?: boolean | '' }): Observable<RhQualificacao[]> {
    let params = new HttpParams();
    const text = (filters?.text || '').trim();
    if (text) params = params.set('text', text);
    if (typeof filters?.ativo === 'boolean') params = params.set('ativo', String(filters.ativo));
    return this.http.get<RhQualificacao[]>(this.baseUrl, { params });
  }

  getById(id: number | string): Observable<RhQualificacao> {
    return this.http.get<RhQualificacao>(`${this.baseUrl}/${id}`);
  }

  create(payload: RhQualificacaoPayload): Observable<RhQualificacao> {
    return this.http.post<RhQualificacao>(this.baseUrl, payload);
  }

  update(id: number | string, payload: RhQualificacaoPayload): Observable<RhQualificacao> {
    return this.http.put<RhQualificacao>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

