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
}
