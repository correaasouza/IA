import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EntidadePapel {
  id: number;
  tipoEntidadeId: number;
  pessoaId: number;
  alerta?: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class EntidadePapelService {
  private baseUrl = `${environment.apiBaseUrl}/api/entidades-papel`;

  constructor(private http: HttpClient) {}

  list(tipoEntidadeId: number | null, page = 0, size = 50): Observable<any> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    });
    if (tipoEntidadeId) {
      params.set('tipoEntidadeId', String(tipoEntidadeId));
    }
    return this.http.get(`${this.baseUrl}?${params.toString()}`);
  }

  get(id: number): Observable<EntidadePapel> {
    return this.http.get<EntidadePapel>(`${this.baseUrl}/${id}`);
  }

  create(payload: any): Observable<EntidadePapel> {
    return this.http.post<EntidadePapel>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<EntidadePapel> {
    return this.http.put<EntidadePapel>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
