import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CatalogCrudType } from './catalog-item.service';

export type CatalogMovementMetricType = 'QUANTIDADE' | 'PRECO';
export type CatalogMovementOriginType = 'MUDANCA_GRUPO' | 'MOVIMENTO_ESTOQUE' | 'SYSTEM' | 'WORKFLOW_ACTION';

export interface CatalogStockBalanceRow {
  estoqueTipoId: number;
  estoqueTipoCodigo?: string | null;
  estoqueTipoNome?: string | null;
  filialId: number;
  filialNome?: string | null;
  quantidadeAtual: number;
  precoAtual: number;
}

export interface CatalogStockConsolidatedRow {
  estoqueTipoId: number;
  estoqueTipoCodigo?: string | null;
  estoqueTipoNome?: string | null;
  quantidadeTotal: number;
  precoTotal: number;
}

export interface CatalogStockBalanceView {
  catalogoId: number;
  agrupadorEmpresaId: number;
  rows: CatalogStockBalanceRow[];
  consolidado: CatalogStockConsolidatedRow[];
}

export interface CatalogMovementLine {
  id: number;
  agrupadorEmpresaId: number;
  estoqueTipoId: number;
  estoqueTipoCodigo?: string | null;
  estoqueTipoNome?: string | null;
  filialId: number;
  filialNome?: string | null;
  metricType: CatalogMovementMetricType;
  beforeValue: number;
  delta: number;
  afterValue: number;
}

export interface CatalogMovement {
  id: number;
  catalogoId: number;
  agrupadorEmpresaId: number;
  origemMovimentacaoTipo: CatalogMovementOriginType;
  origemMovimentacaoCodigo?: string | null;
  origemMovimentacaoId?: number | null;
  movimentoTipo?: string | null;
  origemMovimentoItemCodigo?: string | null;
  workflowOrigin?: string | null;
  workflowEntityId?: number | null;
  workflowTransitionKey?: string | null;
  dataHoraMovimentacao: string;
  observacao?: string | null;
  lines: CatalogMovementLine[];
}

export interface CatalogLedgerResponse {
  content: CatalogMovement[];
  totalElements?: number;
  page?: {
    totalElements: number;
  };
}

@Injectable({ providedIn: 'root' })
export class CatalogStockService {
  private baseUrl = `${environment.apiBaseUrl}/api/catalog`;

  constructor(private http: HttpClient) {}

  getBalances(
    type: CatalogCrudType,
    catalogoId: number,
    params?: {
      agrupadorId?: number | null;
      estoqueTipoId?: number | null;
      filialId?: number | null;
    }
  ): Observable<CatalogStockBalanceView> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}` !== '') query.set(key, `${value}`);
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return this.http.get<CatalogStockBalanceView>(`${this.baseUrl}/${type}/items/${catalogoId}/stock/balances${suffix}`);
  }

  getLedger(
    type: CatalogCrudType,
    catalogoId: number,
    params?: {
      page?: number;
      size?: number;
      agrupadorId?: number | null;
      origemTipo?: CatalogMovementOriginType | '';
      origemCodigo?: string;
      origemId?: number | null;
      movimentoTipo?: string;
      usuario?: string;
      metricType?: CatalogMovementMetricType | '';
      estoqueTipoId?: number | null;
      filialId?: number | null;
      fromDate?: string;
      toDate?: string;
      tzOffsetMinutes?: number | null;
    }
  ): Observable<CatalogLedgerResponse> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}` !== '') query.set(key, `${value}`);
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return this.http.get<CatalogLedgerResponse>(`${this.baseUrl}/${type}/items/${catalogoId}/stock/ledger${suffix}`);
  }
}
