import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegistroEntidadeContexto {
  empresaId: number;
  empresaNome: string;
  tipoEntidadeId: number;
  agrupadorId?: number | null;
  agrupadorNome?: string | null;
  tipoEntidadeConfigAgrupadorId?: number | null;
  vinculado: boolean;
  motivo?: string | null;
  mensagem?: string | null;
}

export interface PessoaVinculoPayload {
  nome: string;
  apelido?: string;
  tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
  registroFederal: string;
}

export interface RegistroEntidadePayload {
  grupoEntidadeId?: number | null;
  ativo: boolean;
  pessoa: PessoaVinculoPayload;
}

export interface RegistroEntidade {
  id: number;
  tipoEntidadeConfigAgrupadorId: number;
  codigo: number;
  grupoEntidadeId?: number | null;
  grupoEntidadeNome?: string | null;
  ativo: boolean;
  pessoa: {
    id: number;
    nome: string;
    apelido?: string | null;
    tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    registroFederal: string;
  };
}

export interface RegistroEntidadeListResponse {
  content: RegistroEntidade[];
  totalElements?: number;
  page?: {
    totalElements: number;
  };
}

@Injectable({ providedIn: 'root' })
export class EntityRecordService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  contextoEmpresa(tipoEntidadeId: number): Observable<RegistroEntidadeContexto> {
    return this.http.get<RegistroEntidadeContexto>(`${this.baseUrl}/${tipoEntidadeId}/entidades/contexto-empresa`);
  }

  list(
    tipoEntidadeId: number,
    params: {
      page?: number;
      size?: number;
      codigo?: number | null;
      pessoaNome?: string;
      registroFederal?: string;
      grupoId?: number | null;
      ativo?: boolean | '';
    }
  ): Observable<RegistroEntidadeListResponse> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}` !== '') query.set(key, `${value}`);
    });
    return this.http.get<RegistroEntidadeListResponse>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades?${query.toString()}`
    );
  }

  get(tipoEntidadeId: number, id: number): Observable<RegistroEntidade> {
    return this.http.get<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`);
  }

  create(tipoEntidadeId: number, payload: RegistroEntidadePayload): Observable<RegistroEntidade> {
    return this.http.post<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades`, payload);
  }

  update(tipoEntidadeId: number, id: number, payload: RegistroEntidadePayload): Observable<RegistroEntidade> {
    return this.http.put<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`, payload);
  }

  delete(tipoEntidadeId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`);
  }
}
