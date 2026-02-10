import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TipoEntidade {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  versao?: number;
}

@Injectable({ providedIn: 'root' })
export class TipoEntidadeService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 200): Observable<any> {
    return this.http.get(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  create(payload: any): Observable<TipoEntidade> {
    return this.http.post<TipoEntidade>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<TipoEntidade> {
    return this.http.put<TipoEntidade>(`${this.baseUrl}/${id}`, payload);
  }
}
