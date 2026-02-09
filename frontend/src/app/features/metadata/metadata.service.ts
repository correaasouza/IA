import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TipoEntidade {
  id: number;
  nome: string;
  versao: number;
}

export interface CampoDefinicao {
  id: number;
  tipoEntidadeId: number;
  nome: string;
  label?: string;
  tipo: string;
  obrigatorio: boolean;
  tamanho?: number;
  versao: number;
}

@Injectable({ providedIn: 'root' })
export class MetadataService {
  private baseTipos = `${environment.apiBaseUrl}/api/tipos-entidade`;
  private baseCampos = `${environment.apiBaseUrl}/api/campos-definicao`;

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

  createTipo(nome: string): Observable<TipoEntidade> {
    return this.http.post<TipoEntidade>(this.baseTipos, { nome });
  }

  listCampos(tenantId: string, tipoEntidadeId: number): Observable<any> {
    const key = `meta:campos:${tenantId}:${tipoEntidadeId}`;
    const etagKey = `meta:campos:etag:${tenantId}:${tipoEntidadeId}`;
    const cached = localStorage.getItem(key);
    const etag = localStorage.getItem(etagKey) || '';
    const headers = etag ? new HttpHeaders({ 'If-None-Match': etag }) : undefined;

    return this.http.get(`${this.baseCampos}?tipoEntidadeId=${tipoEntidadeId}`, { headers, observe: 'response' }).pipe(
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

  createCampo(payload: any): Observable<CampoDefinicao> {
    return this.http.post<CampoDefinicao>(this.baseCampos, payload);
  }
}
