import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CatalogCrudType } from './catalog-item.service';

export interface CatalogGroupNode {
  id: number;
  nome: string;
  parentId?: number | null;
  nivel: number;
  ordem: number;
  path: string;
  ativo: boolean;
  totalItems?: number | null;
  children: CatalogGroupNode[];
}

@Injectable({ providedIn: 'root' })
export class CatalogGroupService {
  private baseUrl = `${environment.apiBaseUrl}/api/catalog`;

  constructor(private http: HttpClient) {}

  tree(type: CatalogCrudType): Observable<CatalogGroupNode[]> {
    return this.http.get<CatalogGroupNode[]>(`${this.baseUrl}/${type}/groups/tree`);
  }

  create(type: CatalogCrudType, payload: { nome: string; parentId?: number | null }): Observable<CatalogGroupNode> {
    return this.http.post<CatalogGroupNode>(`${this.baseUrl}/${type}/groups`, {
      nome: payload.nome,
      parentId: payload.parentId ?? null
    });
  }

  update(
    type: CatalogCrudType,
    groupId: number,
    payload: { nome: string; parentId?: number | null; ordem?: number | null }
  ): Observable<CatalogGroupNode> {
    return this.http.put<CatalogGroupNode>(`${this.baseUrl}/${type}/groups/${groupId}`, {
      nome: payload.nome,
      parentId: payload.parentId ?? null,
      ordem: payload.ordem ?? null
    });
  }

  delete(type: CatalogCrudType, groupId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${type}/groups/${groupId}`);
  }
}
