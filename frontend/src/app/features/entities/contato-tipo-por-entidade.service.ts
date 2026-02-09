import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ContatoTipoPorEntidade {
  id: number;
  entidadeDefinicaoId: number;
  contatoTipoId: number;
  obrigatorio: boolean;
  principalUnico: boolean;
}

@Injectable({ providedIn: 'root' })
export class ContatoTipoPorEntidadeService {
  private baseUrl = `${environment.apiBaseUrl}/api/contatos/tipos-por-entidade`;

  constructor(private http: HttpClient) {}

  list(entidadeDefinicaoId: number): Observable<ContatoTipoPorEntidade[]> {
    return this.http.get<ContatoTipoPorEntidade[]>(`${this.baseUrl}?entidadeDefinicaoId=${entidadeDefinicaoId}`);
  }

  create(payload: any): Observable<ContatoTipoPorEntidade> {
    return this.http.post<ContatoTipoPorEntidade>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<ContatoTipoPorEntidade> {
    return this.http.put<ContatoTipoPorEntidade>(`${this.baseUrl}/${id}`, payload);
  }
}
