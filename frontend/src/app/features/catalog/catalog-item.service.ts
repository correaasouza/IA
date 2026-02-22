import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type CatalogCrudType = 'PRODUCTS' | 'SERVICES';
export type CatalogNumberingMode = 'AUTOMATICA' | 'MANUAL';
export type CatalogPriceType = 'PURCHASE' | 'COST' | 'AVERAGE_COST' | 'SALE_BASE';
export type PriceAdjustmentKind = 'FIXED' | 'PERCENT';
export type CatalogPriceEditedField = 'PRICE' | 'ADJUSTMENT';

export interface CatalogItemContext {
  empresaId: number;
  empresaNome: string;
  type: CatalogCrudType;
  catalogConfigurationId: number;
  agrupadorId?: number | null;
  agrupadorNome?: string | null;
  numberingMode: CatalogNumberingMode;
  vinculado: boolean;
  motivo?: string | null;
  mensagem?: string | null;
}

export interface CatalogItemPayload {
  codigo?: number | null;
  nome: string;
  descricao?: string | null;
  catalogGroupId?: number | null;
  tenantUnitId: string;
  unidadeAlternativaTenantUnitId?: string | null;
  fatorConversaoAlternativa?: number | null;
  ativo: boolean;
  prices?: CatalogItemPricePayload[];
}

export interface CatalogItemPricePayload {
  priceType: CatalogPriceType;
  priceFinal?: number | null;
  adjustmentKind?: PriceAdjustmentKind | null;
  adjustmentValue?: number | null;
  lastEditedField?: CatalogPriceEditedField | null;
}

export interface CatalogItemPricePreviewPayload {
  catalogItemId?: number | null;
  prices: CatalogItemPricePayload[];
}

export interface CatalogItemPrice {
  priceType: CatalogPriceType;
  priceFinal: number;
  adjustmentKind: PriceAdjustmentKind;
  adjustmentValue: number;
}

export interface CatalogItem {
  id: number;
  type: CatalogCrudType;
  catalogConfigurationId: number;
  agrupadorEmpresaId: number;
  agrupadorEmpresaNome?: string | null;
  catalogGroupId?: number | null;
  catalogGroupNome?: string | null;
  codigo: number;
  nome: string;
  descricao?: string | null;
  tenantUnitId: string;
  tenantUnitSigla?: string | null;
  tenantUnitNome?: string | null;
  unidadeAlternativaTenantUnitId?: string | null;
  unidadeAlternativaSigla?: string | null;
  unidadeAlternativaNome?: string | null;
  fatorConversaoAlternativa?: number | null;
  prices?: CatalogItemPrice[];
  hasStockMovements?: boolean;
  ativo: boolean;
}

export interface CatalogItemListResponse {
  content: CatalogItem[];
  totalElements?: number;
  page?: {
    totalElements: number;
  };
}

@Injectable({ providedIn: 'root' })
export class CatalogItemService {
  private baseUrl = `${environment.apiBaseUrl}/api/catalog`;

  constructor(private http: HttpClient) {}

  contextoEmpresa(type: CatalogCrudType): Observable<CatalogItemContext> {
    return this.http.get<CatalogItemContext>(`${this.baseUrl}/${type}/contexto-empresa`);
  }

  list(
    type: CatalogCrudType,
    params: {
      page?: number;
      size?: number;
      codigo?: number | null;
      text?: string;
      grupoId?: number | null;
      ativo?: boolean | '';
    }
  ): Observable<CatalogItemListResponse> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}` !== '') query.set(key, `${value}`);
    });
    return this.http.get<CatalogItemListResponse>(`${this.baseUrl}/${type}/items?${query.toString()}`);
  }

  get(type: CatalogCrudType, id: number): Observable<CatalogItem> {
    return this.http.get<CatalogItem>(`${this.baseUrl}/${type}/items/${id}`);
  }

  previewPrices(type: CatalogCrudType, payload: CatalogItemPricePreviewPayload): Observable<CatalogItemPrice[]> {
    return this.http.post<CatalogItemPrice[]>(`${this.baseUrl}/${type}/items/prices/preview`, payload);
  }

  create(type: CatalogCrudType, payload: CatalogItemPayload): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(`${this.baseUrl}/${type}/items`, payload);
  }

  update(type: CatalogCrudType, id: number, payload: CatalogItemPayload): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${this.baseUrl}/${type}/items/${id}`, payload);
  }

  delete(type: CatalogCrudType, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${type}/items/${id}`);
  }
}
