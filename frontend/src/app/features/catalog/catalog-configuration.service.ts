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
}
