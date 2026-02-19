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
  tiposItensPermitidos: MovimentoTipoItemTemplate[];
  nome: string;
}

export interface MovimentoTipoItemTemplate {
  tipoItemId: number;
  nome: string;
  catalogType: 'PRODUCTS' | 'SERVICES';
  cobrar: boolean;
}

export interface MovimentoEstoqueItemRequest {
  movimentoItemTipoId: number;
  catalogItemId: number;
  quantidade: number;
  valorUnitario: number;
  ordem?: number | null;
  observacao?: string | null;
}

export interface MovimentoEstoqueItemResponse {
  id: number;
  movimentoItemTipoId: number;
  movimentoItemTipoNome: string;
  catalogType: 'PRODUCTS' | 'SERVICES';
  catalogItemId: number;
  catalogCodigoSnapshot: number;
  catalogNomeSnapshot: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
  cobrar: boolean;
  ordem: number;
  observacao?: string | null;
}

export interface MovimentoItemCatalogOption {
  id: number;
  catalogType: 'PRODUCTS' | 'SERVICES';
  codigo: number;
  nome: string;
  descricao?: string | null;
}

export interface MovimentoItemCatalogPage {
  content: MovimentoItemCatalogOption[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface MovimentoEstoqueCreateRequest {
  empresaId: number;
  nome: string;
  tipoEntidadeId?: number | null;
  itens: MovimentoEstoqueItemRequest[];
}

export interface MovimentoEstoqueUpdateRequest {
  empresaId: number;
  nome: string;
  tipoEntidadeId?: number | null;
  version: number;
  itens: MovimentoEstoqueItemRequest[];
}

export interface MovimentoEstoqueResponse {
  id: number;
  tipoMovimento: string;
  empresaId: number;
  nome: string;
  movimentoConfigId: number;
  tipoEntidadePadraoId: number | null;
  itens: MovimentoEstoqueItemResponse[];
  totalItens: number;
  totalCobrado: number;
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

  searchCatalogItemsByTipoItem(tipoItemId: number, text = '', page = 0, size = 20): Observable<MovimentoItemCatalogPage> {
    let params = new HttpParams()
      .set('tipoItemId', String(tipoItemId))
      .set('page', String(page))
      .set('size', String(size));
    const normalizedText = (text || '').trim();
    if (normalizedText) {
      params = params.set('text', normalizedText);
    }
    return this.http.get<MovimentoItemCatalogPage>(`${this.baseUrl}/${this.tipoEstoque}/catalogo-itens`, { params });
  }
}
