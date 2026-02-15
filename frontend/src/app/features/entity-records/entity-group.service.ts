import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GrupoEntidadeNode {
  id: number;
  nome: string;
  parentId?: number | null;
  nivel: number;
  ordem: number;
  path: string;
  ativo: boolean;
  totalRegistros?: number | null;
  children: GrupoEntidadeNode[];
}

@Injectable({ providedIn: 'root' })
export class EntityGroupService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  tree(tipoEntidadeId: number): Observable<GrupoEntidadeNode[]> {
    return this.http.get<GrupoEntidadeNode[]>(`${this.baseUrl}/${tipoEntidadeId}/grupos-entidade/tree`);
  }

  create(tipoEntidadeId: number, nome: string, parentId?: number | null): Observable<GrupoEntidadeNode> {
    return this.http.post<GrupoEntidadeNode>(`${this.baseUrl}/${tipoEntidadeId}/grupos-entidade`, {
      nome,
      parentId: parentId ?? null
    });
  }

  update(tipoEntidadeId: number, grupoId: number, payload: { nome: string; parentId?: number | null; ordem?: number | null }): Observable<GrupoEntidadeNode> {
    return this.http.put<GrupoEntidadeNode>(`${this.baseUrl}/${tipoEntidadeId}/grupos-entidade/${grupoId}`, {
      nome: payload.nome,
      parentId: payload.parentId ?? null,
      ordem: payload.ordem ?? null
    });
  }

  delete(tipoEntidadeId: number, grupoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/grupos-entidade/${grupoId}`);
  }
}
