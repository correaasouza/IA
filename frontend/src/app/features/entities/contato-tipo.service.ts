import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ContatoTipo {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  obrigatorio: boolean;
  principalUnico: boolean;
  mascara?: string;
  regexValidacao?: string;
}

@Injectable({ providedIn: 'root' })
export class ContatoTipoService {
  private baseUrl = `${environment.apiBaseUrl}/api/contatos/tipos`;

  constructor(private http: HttpClient) {}

  list(): Observable<ContatoTipo[]> {
    return this.http.get<ContatoTipo[]>(this.baseUrl);
  }

  create(payload: any): Observable<ContatoTipo> {
    return this.http.post<ContatoTipo>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<ContatoTipo> {
    return this.http.put<ContatoTipo>(`${this.baseUrl}/${id}`, payload);
  }
}
