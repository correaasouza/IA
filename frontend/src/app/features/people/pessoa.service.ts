import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Pessoa {
  id: number;
  nome: string;
  apelido?: string;
  cpf?: string;
  cnpj?: string;
  idEstrangeiro?: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class PessoaService {
  private baseUrl = `${environment.apiBaseUrl}/api/pessoas`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 50): Observable<any> {
    return this.http.get(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  get(id: number): Observable<Pessoa> {
    return this.http.get<Pessoa>(`${this.baseUrl}/${id}`);
  }

  create(payload: any): Observable<Pessoa> {
    return this.http.post<Pessoa>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<Pessoa> {
    return this.http.put<Pessoa>(`${this.baseUrl}/${id}`, payload);
  }

  findByDocumento(documento: string): Observable<Pessoa> {
    return this.http.get<Pessoa>(`${this.baseUrl}/busca?documento=${encodeURIComponent(documento)}`);
  }
}
