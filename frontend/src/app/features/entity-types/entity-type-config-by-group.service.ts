import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TipoEntidadeConfigPorAgrupador {
  agrupadorId: number;
  agrupadorNome: string;
  obrigarUmTelefone: boolean;
  ativo: boolean;
}

export interface EntidadeFormFieldConfig {
  id?: number | null;
  fieldKey: string;
  label?: string | null;
  ordem: number;
  visible: boolean;
  editable: boolean;
  required: boolean;
}

export interface EntidadeFormGroupConfig {
  id?: number | null;
  groupKey: string;
  label?: string | null;
  ordem: number;
  enabled: boolean;
  collapsedByDefault: boolean;
  fields: EntidadeFormFieldConfig[];
}

export interface EntidadeFormConfigByGroup {
  tipoEntidadeId: number;
  agrupadorId: number;
  agrupadorNome: string;
  obrigarUmTelefone: boolean;
  groups: EntidadeFormGroupConfig[];
}

@Injectable({ providedIn: 'root' })
export class EntityTypeConfigByGroupService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  list(tipoEntidadeId: number): Observable<TipoEntidadeConfigPorAgrupador[]> {
    return this.http.get<TipoEntidadeConfigPorAgrupador[]>(`${this.baseUrl}/${tipoEntidadeId}/config-agrupadores`);
  }

  update(tipoEntidadeId: number, agrupadorId: number, obrigarUmTelefone: boolean): Observable<TipoEntidadeConfigPorAgrupador> {
    return this.http.put<TipoEntidadeConfigPorAgrupador>(
      `${this.baseUrl}/${tipoEntidadeId}/config-agrupadores/${agrupadorId}`,
      { obrigarUmTelefone }
    );
  }

  getFormConfig(tipoEntidadeId: number, agrupadorId: number): Observable<EntidadeFormConfigByGroup> {
    return this.http.get<EntidadeFormConfigByGroup>(`${this.baseUrl}/${tipoEntidadeId}/config-agrupadores/${agrupadorId}/ficha`);
  }

  updateFormConfig(
    tipoEntidadeId: number,
    agrupadorId: number,
    payload: { obrigarUmTelefone: boolean; groups: EntidadeFormGroupConfig[] }
  ): Observable<EntidadeFormConfigByGroup> {
    return this.http.put<EntidadeFormConfigByGroup>(
      `${this.baseUrl}/${tipoEntidadeId}/config-agrupadores/${agrupadorId}/ficha`,
      payload
    );
  }
}
