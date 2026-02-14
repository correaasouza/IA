import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmpresaResponse {
  id: number;
  tipo: 'MATRIZ' | 'FILIAL';
  matrizId?: number | null;
  razaoSocial: string;
  nomeFantasia?: string | null;
  cnpj: string;
  ativo: boolean;
}

export interface EmpresaMatrizRequest {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  ativo: boolean;
}

export interface EmpresaFilialRequest {
  matrizId: number;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  ativo: boolean;
}

export interface EmpresaUpdateRequest {
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private baseUrl = `${environment.apiBaseUrl}/api/empresas`;

  constructor(private http: HttpClient) {}

  list(params: Record<string, any>): Observable<any> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v !== undefined && v !== null && `${v}` !== '') query.set(k, `${v}`);
    });
    return this.http.get<any>(`${this.baseUrl}?${query.toString()}`);
  }

  createMatriz(request: EmpresaMatrizRequest): Observable<EmpresaResponse> {
    return this.http.post<EmpresaResponse>(`${this.baseUrl}/matrizes`, request);
  }

  createFilial(request: EmpresaFilialRequest): Observable<EmpresaResponse> {
    return this.http.post<EmpresaResponse>(`${this.baseUrl}/filiais`, request);
  }

  get(id: number): Observable<EmpresaResponse> {
    return this.http.get<EmpresaResponse>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: EmpresaUpdateRequest): Observable<EmpresaResponse> {
    return this.http.put<EmpresaResponse>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, ativo: boolean): Observable<EmpresaResponse> {
    return this.http.patch<EmpresaResponse>(`${this.baseUrl}/${id}/status`, { ativo });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
