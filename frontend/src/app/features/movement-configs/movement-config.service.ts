import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MovimentoTipoOption {
  codigo: string;
  descricao: string;
}

export interface MovimentoConfig {
  id: number;
  tipoMovimento: string;
  nome: string;
  contextoKey?: string | null;
  ativo: boolean;
  version: number;
  empresaIds: number[];
  tiposEntidadePermitidos: number[];
  tipoEntidadePadraoId: number;
  createdAt: string;
  updatedAt: string;
}

export interface MovimentoConfigPage {
  content: MovimentoConfig[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface MovimentoConfigRequest {
  tipoMovimento: string;
  nome: string;
  contextoKey?: string | null;
  ativo?: boolean | null;
  empresaIds: number[];
  tiposEntidadePermitidos: number[];
  tipoEntidadePadraoId: number;
}

export interface MovimentoConfigDuplicarRequest {
  nome?: string | null;
  contextoKey?: string | null;
  ativo?: boolean | null;
}

export interface MovimentoConfigResolverResponse {
  configuracaoId: number;
  tipoMovimento: string;
  empresaId: number;
  contextoKey?: string | null;
  tipoEntidadePadraoId: number;
  tiposEntidadePermitidos: number[];
}

export interface MovimentoConfigCoverageWarning {
  empresaId: number;
  empresaNome: string;
  tipoMovimento: string;
  mensagem: string;
}

@Injectable({ providedIn: 'root' })
export class MovementConfigService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/movimentos/configuracoes`;

  constructor(private http: HttpClient) {}

  listTipos(): Observable<MovimentoTipoOption[]> {
    return this.http.get<MovimentoTipoOption[]>(`${this.baseUrl}/tipos`);
  }

  listByTipo(tipo: string, page = 0, size = 200): Observable<MovimentoConfigPage> {
    const params = new HttpParams()
      .set('tipo', tipo)
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<MovimentoConfigPage>(this.baseUrl, { params });
  }

  getById(id: number): Observable<MovimentoConfig> {
    return this.http.get<MovimentoConfig>(`${this.baseUrl}/${id}`);
  }

  create(payload: MovimentoConfigRequest): Observable<MovimentoConfig> {
    return this.http.post<MovimentoConfig>(this.baseUrl, payload);
  }

  update(id: number, payload: MovimentoConfigRequest): Observable<MovimentoConfig> {
    return this.http.put<MovimentoConfig>(`${this.baseUrl}/${id}`, payload);
  }

  duplicate(id: number, payload?: MovimentoConfigDuplicarRequest): Observable<MovimentoConfig> {
    return this.http.post<MovimentoConfig>(`${this.baseUrl}/${id}/duplicar`, payload ?? {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resolve(tipo: string, empresaId: number, contextoKey?: string | null): Observable<MovimentoConfigResolverResponse> {
    let params = new HttpParams()
      .set('tipo', tipo)
      .set('empresaId', String(empresaId));
    if (contextoKey && contextoKey.trim()) {
      params = params.set('contextoKey', contextoKey.trim());
    }
    return this.http.get<MovimentoConfigResolverResponse>(`${this.baseUrl}/resolver`, { params });
  }

  listCoverageWarnings(): Observable<MovimentoConfigCoverageWarning[]> {
    return this.http.get<MovimentoConfigCoverageWarning[]>(`${this.baseUrl}/warnings`);
  }

  listMenuByEmpresa(empresaId: number): Observable<MovimentoTipoOption[]> {
    const params = new HttpParams().set('empresaId', String(empresaId));
    return this.http.get<MovimentoTipoOption[]>(`${this.baseUrl}/menu`, { params });
  }
}
