import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type CatalogConfigurationType = 'PRODUCTS' | 'SERVICES';
export type CatalogNumberingMode = 'AUTOMATICA' | 'MANUAL';

export interface CatalogConfiguration {
  id: number;
  type: CatalogConfigurationType;
  numberingMode: CatalogNumberingMode;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogConfigurationByGroup {
  agrupadorId: number;
  agrupadorNome: string;
  numberingMode: CatalogNumberingMode;
  active: boolean;
}

export interface CatalogStockType {
  id: number;
  codigo: string;
  nome: string;
  ordem: number;
  active: boolean;
  version: number;
}

export interface CatalogStockAdjustment {
  id: number;
  codigo: string;
  nome: string;
  tipo: 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
  ordem: number;
  active: boolean;
  version: number;
  estoqueOrigemAgrupadorId: number | null;
  estoqueOrigemTipoId: number | null;
  estoqueOrigemFilialId: number | null;
  estoqueDestinoAgrupadorId: number | null;
  estoqueDestinoTipoId: number | null;
  estoqueDestinoFilialId: number | null;
}

export interface CatalogStockAdjustmentScopeOption {
  agrupadorId: number;
  agrupadorNome: string;
  estoqueTipoId: number;
  estoqueTipoCodigo: string;
  estoqueTipoNome: string;
  filialId: number;
  filialNome: string;
  label: string;
}

export interface CatalogStockAdjustmentUpsertPayload {
  nome: string;
  tipo: 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
  ordem?: number | null;
  active?: boolean | null;
  estoqueOrigemAgrupadorId?: number | null;
  estoqueOrigemTipoId?: number | null;
  estoqueOrigemFilialId?: number | null;
  estoqueDestinoAgrupadorId?: number | null;
  estoqueDestinoTipoId?: number | null;
  estoqueDestinoFilialId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CatalogConfigurationService {
  private baseUrl = `${environment.apiBaseUrl}/api/catalog/configuration`;

  constructor(private http: HttpClient) {}

  get(type: CatalogConfigurationType): Observable<CatalogConfiguration> {
    return this.http.get<CatalogConfiguration>(`${this.baseUrl}/${type}`);
  }

  update(
    type: CatalogConfigurationType,
    payload: { numberingMode: CatalogNumberingMode }
  ): Observable<CatalogConfiguration> {
    return this.http.put<CatalogConfiguration>(`${this.baseUrl}/${type}`, payload);
  }

  listByGroup(type: CatalogConfigurationType): Observable<CatalogConfigurationByGroup[]> {
    return this.http.get<CatalogConfigurationByGroup[]>(`${this.baseUrl}/${type}/group-config`);
  }

  updateByGroup(
    type: CatalogConfigurationType,
    agrupadorId: number,
    payload: { numberingMode: CatalogNumberingMode }
  ): Observable<CatalogConfigurationByGroup> {
    return this.http.put<CatalogConfigurationByGroup>(`${this.baseUrl}/${type}/group-config/${agrupadorId}`, payload);
  }

  listStockTypesByGroup(type: CatalogConfigurationType, agrupadorId: number): Observable<CatalogStockType[]> {
    return this.http.get<CatalogStockType[]>(`${this.baseUrl}/${type}/group-config/${agrupadorId}/stock-types`);
  }

  createStockTypeByGroup(
    type: CatalogConfigurationType,
    agrupadorId: number,
    payload: { codigo: string; nome: string; ordem?: number | null; active?: boolean | null }
  ): Observable<CatalogStockType> {
    return this.http.put<CatalogStockType>(`${this.baseUrl}/${type}/group-config/${agrupadorId}/stock-types`, payload);
  }

  updateStockTypeByGroup(
    type: CatalogConfigurationType,
    agrupadorId: number,
    stockTypeId: number,
    payload: { codigo: string; nome: string; ordem?: number | null; active?: boolean | null }
  ): Observable<CatalogStockType> {
    return this.http.put<CatalogStockType>(
      `${this.baseUrl}/${type}/group-config/${agrupadorId}/stock-types/${stockTypeId}`,
      payload);
  }

  listStockAdjustmentsByType(type: CatalogConfigurationType): Observable<CatalogStockAdjustment[]> {
    return this.http.get<CatalogStockAdjustment[]>(`${this.baseUrl}/${type}/stock-adjustments`);
  }

  listStockAdjustmentScopeOptionsByType(type: CatalogConfigurationType): Observable<CatalogStockAdjustmentScopeOption[]> {
    return this.http.get<CatalogStockAdjustmentScopeOption[]>(`${this.baseUrl}/${type}/stock-adjustments/options`);
  }

  createStockAdjustmentByType(
    type: CatalogConfigurationType,
    payload: CatalogStockAdjustmentUpsertPayload
  ): Observable<CatalogStockAdjustment> {
    return this.http.put<CatalogStockAdjustment>(`${this.baseUrl}/${type}/stock-adjustments`, payload);
  }

  updateStockAdjustmentByType(
    type: CatalogConfigurationType,
    adjustmentId: number,
    payload: CatalogStockAdjustmentUpsertPayload
  ): Observable<CatalogStockAdjustment> {
    return this.http.put<CatalogStockAdjustment>(
      `${this.baseUrl}/${type}/stock-adjustments/${adjustmentId}`,
      payload);
  }

  deleteStockAdjustmentByType(type: CatalogConfigurationType, adjustmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${type}/stock-adjustments/${adjustmentId}`);
  }
}
