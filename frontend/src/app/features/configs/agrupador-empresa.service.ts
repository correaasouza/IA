import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AgrupadorEmpresaItem {
  empresaId: number;
  nome: string;
}

export interface AgrupadorEmpresa {
  id: number;
  nome: string;
  ativo: boolean;
  empresas: AgrupadorEmpresaItem[];
}

@Injectable({ providedIn: 'root' })
export class AgrupadorEmpresaService {
  private baseUrl = `${environment.apiBaseUrl}/api/configuracoes`;

  constructor(private http: HttpClient) {}

  list(configType: string, configId: number): Observable<AgrupadorEmpresa[]> {
    return this.http.get<AgrupadorEmpresa[]>(`${this.baseUrl}/${configType}/${configId}/agrupadores-empresa`);
  }

  create(configType: string, configId: number, nome: string): Observable<AgrupadorEmpresa> {
    return this.http.post<AgrupadorEmpresa>(`${this.baseUrl}/${configType}/${configId}/agrupadores-empresa`, { nome });
  }

  rename(configType: string, configId: number, agrupadorId: number, nome: string): Observable<AgrupadorEmpresa> {
    return this.http.patch<AgrupadorEmpresa>(
      `${this.baseUrl}/${configType}/${configId}/agrupadores-empresa/${agrupadorId}/nome`,
      { nome }
    );
  }

  remove(configType: string, configId: number, agrupadorId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${configType}/${configId}/agrupadores-empresa/${agrupadorId}`);
  }

  addEmpresa(configType: string, configId: number, agrupadorId: number, empresaId: number): Observable<AgrupadorEmpresa> {
    return this.http.post<AgrupadorEmpresa>(
      `${this.baseUrl}/${configType}/${configId}/agrupadores-empresa/${agrupadorId}/empresas`,
      { empresaId }
    );
  }

  removeEmpresa(configType: string, configId: number, agrupadorId: number, empresaId: number): Observable<AgrupadorEmpresa> {
    return this.http.delete<AgrupadorEmpresa>(
      `${this.baseUrl}/${configType}/${configId}/agrupadores-empresa/${agrupadorId}/empresas/${empresaId}`
    );
  }
}
