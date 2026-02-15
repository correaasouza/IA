import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TipoEntidade {
  id: number;
  nome: string;
  codigoSeed?: string | null;
  tipoPadrao: boolean;
  ativo: boolean;
}

export interface TipoEntidadeRequest {
  nome: string;
  ativo: boolean;
}

export interface TipoEntidadeListResponse {
  content: TipoEntidade[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class EntityTypeService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  list(params: {
    page?: number;
    size?: number;
    sort?: string;
    nome?: string;
    ativo?: boolean | '';
  }): Observable<TipoEntidadeListResponse> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v !== undefined && v !== null && `${v}` !== '') query.set(k, `${v}`);
    });
    return this.http.get<TipoEntidadeListResponse>(`${this.baseUrl}?${query.toString()}`);
  }

  get(id: number): Observable<TipoEntidade> {
    return this.http.get<TipoEntidade>(`${this.baseUrl}/${id}`);
  }

  create(payload: TipoEntidadeRequest): Observable<TipoEntidade> {
    return this.http.post<TipoEntidade>(this.baseUrl, payload);
  }

  update(id: number, payload: TipoEntidadeRequest): Observable<TipoEntidade> {
    return this.http.put<TipoEntidade>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
