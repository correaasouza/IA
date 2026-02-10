import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TipoEntidade {
  id: number;
  codigo: string;
  nome: string;
  ativo: boolean;
  versao: number;
}

export interface TipoEntidadeCampoRegra {
  id: number;
  tipoEntidadeId: number;
  campo: string;
  habilitado: boolean;
  requerido: boolean;
  visivel: boolean;
  editavel: boolean;
  label?: string;
  versao: number;
}

@Injectable({ providedIn: 'root' })
export class MetadataService {
  private baseTipos = `${environment.apiBaseUrl}/api/tipos-entidade`;
  private baseCampos = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  listTipos(tenantId: string): Observable<any> {
    const key = `meta:tipos:${tenantId}`;
    const etagKey = `meta:tipos:etag:${tenantId}`;
    const cached = localStorage.getItem(key);
    const etag = localStorage.getItem(etagKey) || '';

    const headers = etag ? new HttpHeaders({ 'If-None-Match': etag }) : undefined;
    return this.http.get(this.baseTipos, { headers, observe: 'response' }).pipe(
      tap(resp => {
        if (resp.status === 200) {
          localStorage.setItem(key, JSON.stringify(resp.body));
          const newEtag = resp.headers.get('ETag');
          if (newEtag) {
            localStorage.setItem(etagKey, newEtag);
          }
        }
      }),
      (source) => new Observable(observer => {
        source.subscribe({
          next: resp => {
            if (resp.status === 304 && cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.next(resp.body);
            observer.complete();
          },
          error: err => {
            if (cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.error(err);
          }
        });
      })
    );
  }

  createTipo(payload: any): Observable<TipoEntidade> {
    return this.http.post<TipoEntidade>(this.baseTipos, payload);
  }

  listCampos(tenantId: string, tipoEntidadeId: number): Observable<TipoEntidadeCampoRegra[]> {
    const key = `meta:campos:${tenantId}:${tipoEntidadeId}`;
    const cached = localStorage.getItem(key);
    return this.http.get<TipoEntidadeCampoRegra[]>(`${this.baseCampos}/${tipoEntidadeId}/campos`).pipe(
      tap(resp => {
        localStorage.setItem(key, JSON.stringify(resp));
      }),
      (source) => new Observable(observer => {
        source.subscribe({
          next: resp => {
            observer.next(resp);
            observer.complete();
          },
          error: err => {
            if (cached) {
              observer.next(JSON.parse(cached));
              observer.complete();
              return;
            }
            observer.error(err);
          }
        });
      })
    );
  }

  saveCampos(tipoEntidadeId: number, payload: any[]): Observable<TipoEntidadeCampoRegra[]> {
    return this.http.put<TipoEntidadeCampoRegra[]>(`${this.baseCampos}/${tipoEntidadeId}/campos`, payload);
  }
}
