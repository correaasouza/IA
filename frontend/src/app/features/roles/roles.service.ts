import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Papel {
  id: number;
  nome: string;
  descricao?: string;
  ativo: boolean;
}

export interface PermissaoCatalog {
  id: number;
  codigo: string;
  label: string;
}

@Injectable({ providedIn: 'root' })
export class RolesService {
  private baseUrl = `${environment.apiBaseUrl}/api/papeis`;

  constructor(private http: HttpClient) {}

  list(): Observable<Papel[]> {
    return this.http.get<Papel[]>(this.baseUrl);
  }

  get(id: number): Observable<Papel> {
    return this.http.get<Papel>(`${this.baseUrl}/${id}`);
  }

  create(payload: { nome: string; descricao?: string; ativo: boolean }): Observable<Papel> {
    return this.http.post<Papel>(this.baseUrl, payload);
  }

  update(id: number, payload: { nome: string; descricao?: string; ativo: boolean }): Observable<Papel> {
    return this.http.put<Papel>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listPermissoes(id: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/${id}/permissoes`);
  }

  setPermissoes(id: number, permissoes: string[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/permissoes`, { permissoes });
  }

  listCatalog(): Observable<PermissaoCatalog[]> {
    return this.http.get<PermissaoCatalog[]>(`${environment.apiBaseUrl}/api/permissoes`);
  }

  createPerm(payload: { codigo: string; label: string; ativo: boolean }): Observable<PermissaoCatalog> {
    return this.http.post<PermissaoCatalog>(`${environment.apiBaseUrl}/api/permissoes`, payload);
  }

  updatePerm(id: number, codigo: string, label: string): Observable<PermissaoCatalog> {
    const item = { codigo, label, ativo: true };
    return this.http.put<PermissaoCatalog>(`${environment.apiBaseUrl}/api/permissoes/${id}`, item);
  }
}
