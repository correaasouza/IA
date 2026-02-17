import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MovimentoTemplateRequest {
  empresaId: number;
}

export interface MovimentoEstoqueTemplateResponse {
  tipoMovimento: string;
  empresaId: number;
  movimentoConfigId: number;
  tipoEntidadePadraoId: number | null;
  tiposEntidadePermitidos: number[];
  nome: string;
  dataMovimento: string;
}

export interface MovimentoEstoqueCreateRequest {
  empresaId: number;
  nome: string;
  dataMovimento: string | null;
}

export interface MovimentoEstoqueUpdateRequest {
  empresaId: number;
  nome: string;
  dataMovimento: string | null;
  version: number;
}

export interface MovimentoEstoqueResponse {
  id: number;
  tipoMovimento: string;
  empresaId: number;
  nome: string;
  dataMovimento: string | null;
  movimentoConfigId: number;
  tipoEntidadePadraoId: number | null;
  version: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MovimentoEstoquePage {
  content: MovimentoEstoqueResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface MovimentoEstoqueListFilters {
  page?: number;
  size?: number;
  nome?: string;
  dataInicio?: string | null;
  dataFim?: string | null;
}

@Injectable({ providedIn: 'root' })
export class MovementOperationService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/movimentos`;
  private readonly tipoEstoque = 'MOVIMENTO_ESTOQUE';

  constructor(private http: HttpClient) {}

  buildTemplate(tipo: string, empresaId: number): Observable<MovimentoEstoqueTemplateResponse> {
    const payload: MovimentoTemplateRequest = { empresaId };
    return this.http.post<MovimentoEstoqueTemplateResponse>(`${this.baseUrl}/${tipo}/template`, payload);
  }

  listEstoque(filters: MovimentoEstoqueListFilters = {}): Observable<MovimentoEstoquePage> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));
    const nome = (filters.nome || '').trim();
    if (nome) {
      params = params.set('nome', nome);
    }
    if (filters.dataInicio) {
      params = params.set('dataInicio', filters.dataInicio);
    }
    if (filters.dataFim) {
      params = params.set('dataFim', filters.dataFim);
    }
    return this.http.get<MovimentoEstoquePage>(`${this.baseUrl}/${this.tipoEstoque}`, { params });
  }

  getEstoque(id: number): Observable<MovimentoEstoqueResponse> {
    return this.http.get<MovimentoEstoqueResponse>(`${this.baseUrl}/${this.tipoEstoque}/${id}`);
  }

  createEstoque(payload: MovimentoEstoqueCreateRequest): Observable<MovimentoEstoqueResponse> {
    return this.http.post<MovimentoEstoqueResponse>(`${this.baseUrl}/${this.tipoEstoque}`, payload);
  }

  updateEstoque(id: number, payload: MovimentoEstoqueUpdateRequest): Observable<MovimentoEstoqueResponse> {
    return this.http.put<MovimentoEstoqueResponse>(`${this.baseUrl}/${this.tipoEstoque}/${id}`, payload);
  }

  deleteEstoque(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${this.tipoEstoque}/${id}`);
  }
}
