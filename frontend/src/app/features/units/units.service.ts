import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type OfficialUnitOrigin = 'NFE_TABELA_UNIDADE_COMERCIAL' | 'MANUAL';

export interface OfficialUnit {
  id: string;
  codigoOficial: string;
  descricao: string;
  ativo: boolean;
  origem: OfficialUnitOrigin;
}

export interface OfficialUnitPayload {
  codigoOficial: string;
  descricao: string;
  ativo: boolean;
  origem?: OfficialUnitOrigin | null;
}

export interface TenantUnit {
  id: string;
  tenantId: number;
  unidadeOficialId: string;
  unidadeOficialCodigo: string;
  unidadeOficialDescricao: string;
  unidadeOficialAtiva: boolean;
  sigla: string;
  nome: string;
  fatorParaOficial: number;
  systemMirror: boolean;
  padrao: boolean;
}

export interface TenantUnitPayload {
  unidadeOficialId: string;
  sigla: string;
  nome: string;
  fatorParaOficial: number;
  padrao?: boolean;
}

export interface TenantUnitReconcileResult {
  tenantId: number;
  createdMirrors: number;
}

export interface TenantUnitConversion {
  id: string;
  tenantId: number;
  unidadeOrigemId: string;
  unidadeOrigemSigla: string;
  unidadeDestinoId: string;
  unidadeDestinoSigla: string;
  fator: number;
}

export interface TenantUnitConversionPayload {
  unidadeOrigemId: string;
  unidadeDestinoId: string;
  fator: number;
}

@Injectable({ providedIn: 'root' })
export class UnitsService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  listOfficial(filters?: { ativo?: boolean | ''; text?: string }): Observable<OfficialUnit[]> {
    let params = new HttpParams();
    if (typeof filters?.ativo === 'boolean') {
      params = params.set('ativo', String(filters.ativo));
    }
    const text = (filters?.text || '').trim();
    if (text) {
      params = params.set('text', text);
    }
    return this.http.get<OfficialUnit[]>(`${this.baseUrl}/api/global/official-units`, { params });
  }

  getOfficial(id: string): Observable<OfficialUnit> {
    return this.http.get<OfficialUnit>(`${this.baseUrl}/api/global/official-units/${id}`);
  }

  createOfficial(payload: OfficialUnitPayload): Observable<OfficialUnit> {
    return this.http.post<OfficialUnit>(`${this.baseUrl}/api/global/official-units`, payload);
  }

  updateOfficial(id: string, payload: OfficialUnitPayload): Observable<OfficialUnit> {
    return this.http.put<OfficialUnit>(`${this.baseUrl}/api/global/official-units/${id}`, payload);
  }

  deleteOfficial(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/global/official-units/${id}`);
  }

  listTenantUnits(text?: string): Observable<TenantUnit[]> {
    let params = new HttpParams();
    const normalizedText = (text || '').trim();
    if (normalizedText) {
      params = params.set('text', normalizedText);
    }
    return this.http.get<TenantUnit[]>(`${this.baseUrl}/api/tenant/units`, { params });
  }

  getTenantUnit(id: string): Observable<TenantUnit> {
    return this.http.get<TenantUnit>(`${this.baseUrl}/api/tenant/units/${id}`);
  }

  createTenantUnit(payload: TenantUnitPayload): Observable<TenantUnit> {
    return this.http.post<TenantUnit>(`${this.baseUrl}/api/tenant/units`, payload);
  }

  updateTenantUnit(id: string, payload: TenantUnitPayload): Observable<TenantUnit> {
    return this.http.put<TenantUnit>(`${this.baseUrl}/api/tenant/units/${id}`, payload);
  }

  deleteTenantUnit(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/tenant/units/${id}`);
  }

  reconcileTenantUnits(): Observable<TenantUnitReconcileResult> {
    return this.http.post<TenantUnitReconcileResult>(`${this.baseUrl}/api/tenant/units/reconcile`, {});
  }

  listTenantUnitConversions(): Observable<TenantUnitConversion[]> {
    return this.http.get<TenantUnitConversion[]>(`${this.baseUrl}/api/tenant/unit-conversions`);
  }

  getTenantUnitConversion(id: string): Observable<TenantUnitConversion> {
    return this.http.get<TenantUnitConversion>(`${this.baseUrl}/api/tenant/unit-conversions/${id}`);
  }

  createTenantUnitConversion(payload: TenantUnitConversionPayload): Observable<TenantUnitConversion> {
    return this.http.post<TenantUnitConversion>(`${this.baseUrl}/api/tenant/unit-conversions`, payload);
  }

  updateTenantUnitConversion(id: string, payload: TenantUnitConversionPayload): Observable<TenantUnitConversion> {
    return this.http.put<TenantUnitConversion>(`${this.baseUrl}/api/tenant/unit-conversions/${id}`, payload);
  }

  deleteTenantUnitConversion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/tenant/unit-conversions/${id}`);
  }
}
