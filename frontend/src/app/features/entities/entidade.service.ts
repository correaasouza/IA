import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EntidadeDefinicao {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  roleRequired?: string;
}

export interface EntidadeRegistro {
  id: number;
  entidadeDefinicaoId: number;
  nome: string;
  apelido?: string;
  cpfCnpj: string;
  tipoPessoa?: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class EntidadeService {
  private baseDef = `${environment.apiBaseUrl}/api/entidades-definicao`;
  private baseReg = `${environment.apiBaseUrl}/api/entidades`;

  constructor(private http: HttpClient) {}

  listDef(page = 0, size = 50): Observable<any> {
    return this.http.get(`${this.baseDef}?page=${page}&size=${size}`);
  }

  updateDef(id: number, payload: any): Observable<EntidadeDefinicao> {
    return this.http.put<EntidadeDefinicao>(`${this.baseDef}/${id}`, payload);
  }

  listReg(entidadeDefinicaoId: number, page = 0, size = 50, filters?: { nome?: string; cpfCnpj?: string; ativo?: string; }): Observable<any> {
    const params = new URLSearchParams({
      entidadeDefinicaoId: String(entidadeDefinicaoId),
      page: String(page),
      size: String(size)
    });
    if (filters?.nome) params.set('nome', filters.nome);
    if (filters?.cpfCnpj) params.set('cpfCnpj', filters.cpfCnpj);
    if (filters?.ativo) params.set('ativo', filters.ativo);
    return this.http.get(`${this.baseReg}?${params.toString()}`);
  }

  createReg(payload: any): Observable<EntidadeRegistro> {
    return this.http.post<EntidadeRegistro>(this.baseReg, payload);
  }

  updateReg(id: number, payload: any): Observable<EntidadeRegistro> {
    return this.http.put<EntidadeRegistro>(`${this.baseReg}/${id}`, payload);
  }

  getReg(id: number): Observable<EntidadeRegistro> {
    return this.http.get<EntidadeRegistro>(`${this.baseReg}/${id}`);
  }

  deleteReg(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseReg}/${id}`);
  }
}
