import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CatalogConfigurationType } from './catalog-configuration.service';

export type CatalogPriceType = 'PURCHASE' | 'COST' | 'AVERAGE_COST' | 'SALE_BASE';
export type PriceBaseMode = 'NONE' | 'BASE_PRICE';
export type PriceAdjustmentKind = 'FIXED' | 'PERCENT';
export type PriceUiLockMode = 'I' | 'II' | 'III' | 'IV';

export interface CatalogPriceRule {
  id: number;
  priceType: CatalogPriceType;
  customName?: string | null;
  baseMode: PriceBaseMode;
  basePriceType?: CatalogPriceType | null;
  adjustmentKindDefault: PriceAdjustmentKind;
  adjustmentDefault: number;
  uiLockMode: PriceUiLockMode;
  active: boolean;
}

export interface CatalogPriceRuleUpsert {
  priceType: CatalogPriceType;
  customName?: string | null;
  baseMode: PriceBaseMode;
  basePriceType?: CatalogPriceType | null;
  adjustmentKindDefault: PriceAdjustmentKind;
  adjustmentDefault?: number | null;
  uiLockMode: PriceUiLockMode;
  active: boolean;
}

export interface PriceBook {
  id: number;
  name: string;
  active: boolean;
  defaultBook: boolean;
}

export interface PriceBookPayload {
  name: string;
  active: boolean;
  defaultBook?: boolean | null;
}

export interface PriceVariant {
  id: number;
  name: string;
  active: boolean;
}

export interface PriceVariantPayload {
  name: string;
  active: boolean;
}

export interface SalePriceGridRow {
  id?: number | null;
  priceBookId: number;
  variantId?: number | null;
  catalogType: CatalogConfigurationType;
  catalogItemId: number;
  catalogItemName?: string | null;
  catalogGroupName?: string | null;
  catalogBasePrice?: number | null;
  tenantUnitId?: string | null;
  priceFinal?: number | null;
}

export interface SalePriceBulkItem {
  catalogType: CatalogConfigurationType;
  catalogItemId: number;
  tenantUnitId?: string | null;
  priceFinal?: number | null;
}

export interface SalePriceResolveRequest {
  priceBookId: number;
  variantId?: number | null;
  catalogType: CatalogConfigurationType;
  catalogItemId: number;
  tenantUnitId?: string | null;
}

export interface SalePriceResolveResponse {
  priceFinal: number;
  salePriceId?: number | null;
  resolvedVariantId?: number | null;
  source: 'EXACT_VARIANT' | 'BOOK_BASE' | 'CATALOG_BASE' | 'INACTIVE_VARIANT_FALLBACK' | 'MANUAL';
}

export interface SalePriceByItemRow {
  priceBookId: number;
  priceBookName: string;
  priceBookActive: boolean;
  variantId?: number | null;
  variantName?: string | null;
  variantActive?: boolean | null;
  priceFinal: number;
  salePriceId?: number | null;
  resolvedVariantId?: number | null;
  source: 'EXACT_VARIANT' | 'BOOK_BASE' | 'CATALOG_BASE' | 'INACTIVE_VARIANT_FALLBACK' | 'MANUAL';
}

export interface SalePriceGroupOption {
  id: number;
  nome: string;
  nivel: number;
}

export type SalePriceApplyMode = 'PERCENT' | 'FIXED';

export interface SalePriceApplyByGroupRequest {
  priceBookId: number;
  variantId?: number | null;
  catalogType: CatalogConfigurationType;
  catalogGroupId?: number | null;
  text?: string | null;
  catalogItemId?: number | null;
  adjustmentKind?: SalePriceApplyMode | null;
  adjustmentValue: number;
  includeChildren?: boolean | null;
  overwriteExisting?: boolean | null;
}

export interface SalePriceApplyByGroupResponse {
  catalogGroupId: number | null;
  totalItemsInScope: number;
  processedItems: number;
  createdItems: number;
  updatedItems: number;
  skippedWithoutBasePrice: number;
  skippedExisting: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogPricingService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/catalog/pricing`;
  private readonly configUrl = `${environment.apiBaseUrl}/api/catalog/configuration`;

  constructor(private readonly http: HttpClient) {}

  listPriceRules(type: CatalogConfigurationType, agrupadorId: number): Observable<CatalogPriceRule[]> {
    return this.http.get<CatalogPriceRule[]>(`${this.configUrl}/${type}/group-config/${agrupadorId}/price-rules`);
  }

  upsertPriceRules(type: CatalogConfigurationType, agrupadorId: number, rules: CatalogPriceRuleUpsert[]): Observable<CatalogPriceRule[]> {
    return this.http.put<CatalogPriceRule[]>(`${this.configUrl}/${type}/group-config/${agrupadorId}/price-rules`, { rules });
  }

  listBooks(): Observable<PriceBook[]> {
    return this.http.get<PriceBook[]>(`${this.baseUrl}/books`);
  }

  getBook(id: number): Observable<PriceBook> {
    return this.http.get<PriceBook>(`${this.baseUrl}/books/${id}`);
  }

  createBook(payload: PriceBookPayload): Observable<PriceBook> {
    return this.http.post<PriceBook>(`${this.baseUrl}/books`, payload);
  }

  updateBook(id: number, payload: PriceBookPayload): Observable<PriceBook> {
    return this.http.put<PriceBook>(`${this.baseUrl}/books/${id}`, payload);
  }

  deleteBook(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/books/${id}`);
  }

  listVariants(): Observable<PriceVariant[]> {
    return this.http.get<PriceVariant[]>(`${this.baseUrl}/variants`);
  }

  getVariant(id: number): Observable<PriceVariant> {
    return this.http.get<PriceVariant>(`${this.baseUrl}/variants/${id}`);
  }

  createVariant(payload: PriceVariantPayload): Observable<PriceVariant> {
    return this.http.post<PriceVariant>(`${this.baseUrl}/variants`, payload);
  }

  updateVariant(id: number, payload: PriceVariantPayload): Observable<PriceVariant> {
    return this.http.put<PriceVariant>(`${this.baseUrl}/variants/${id}`, payload);
  }

  deleteVariant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/variants/${id}`);
  }

  gridSalePrices(
    priceBookId: number,
    variantId?: number | null,
    catalogType?: CatalogConfigurationType | null,
    text?: string | null,
    catalogItemId?: number | null,
    catalogGroupId?: number | null,
    includeGroupChildren?: boolean | null,
    page = 0,
    size = 50
  ): Observable<{ content: SalePriceGridRow[]; totalElements: number }> {
    const params = new URLSearchParams();
    params.set('priceBookId', `${priceBookId}`);
    params.set('page', `${page}`);
    params.set('size', `${size}`);
    if (variantId != null) params.set('variantId', `${variantId}`);
    if (catalogType) params.set('catalogType', catalogType);
    if (text && text.trim()) params.set('text', text.trim());
    if (catalogItemId != null && catalogItemId > 0) params.set('catalogItemId', `${catalogItemId}`);
    if (catalogGroupId != null && catalogGroupId > 0) params.set('catalogGroupId', `${catalogGroupId}`);
    if (catalogGroupId != null && catalogGroupId > 0) params.set('includeGroupChildren', `${!!includeGroupChildren}`);
    return this.http.get<{ content: SalePriceGridRow[]; totalElements: number }>(`${this.baseUrl}/sale-prices/grid?${params.toString()}`);
  }

  bulkUpsertSalePrices(priceBookId: number, variantId: number | null, items: SalePriceBulkItem[]): Observable<SalePriceGridRow[]> {
    return this.http.put<SalePriceGridRow[]>(`${this.baseUrl}/sale-prices/bulk`, {
      priceBookId,
      variantId,
      items
    });
  }

  listSalePriceGroupOptions(catalogType: CatalogConfigurationType): Observable<SalePriceGroupOption[]> {
    const params = new URLSearchParams();
    params.set('catalogType', catalogType);
    return this.http.get<SalePriceGroupOption[]>(`${this.baseUrl}/sale-prices/group-options?${params.toString()}`);
  }

  applySalePricesByGroup(payload: SalePriceApplyByGroupRequest): Observable<SalePriceApplyByGroupResponse> {
    return this.http.post<SalePriceApplyByGroupResponse>(`${this.baseUrl}/sale-prices/apply-by-group`, payload);
  }

  deleteSalePrice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sale-prices/${id}`);
  }

  resolveSalePrice(payload: SalePriceResolveRequest): Observable<SalePriceResolveResponse> {
    return this.http.post<SalePriceResolveResponse>(`${this.baseUrl}/sale-prices/resolve`, payload);
  }

  listSalePricesByItem(
    catalogType: CatalogConfigurationType,
    catalogItemId: number,
    tenantUnitId?: string | null
  ): Observable<SalePriceByItemRow[]> {
    const params = new URLSearchParams();
    params.set('catalogType', catalogType);
    params.set('catalogItemId', `${catalogItemId}`);
    if (tenantUnitId && `${tenantUnitId}`.trim()) {
      params.set('tenantUnitId', `${tenantUnitId}`.trim());
    }
    return this.http.get<SalePriceByItemRow[]>(`${this.baseUrl}/sale-prices/by-item?${params.toString()}`);
  }
}
