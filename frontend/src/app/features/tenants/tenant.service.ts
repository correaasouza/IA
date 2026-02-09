import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toIsoDate } from '../../shared/date-utils';

export interface LocatarioRequest {
  nome: string;
  dataLimiteAcesso: string;
  ativo: boolean;
}

export interface LocatarioResponse {
  id: number;
  nome: string;
  dataLimiteAcesso: string;
  ativo: boolean;
  bloqueado: boolean;
}

export interface LocatarioListResponse {
  content: LocatarioResponse[];
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class TenantService {
  private baseUrl = `${environment.apiBaseUrl}/api/locatarios`;

  constructor(private http: HttpClient) {}

  list(params: {
    page?: number;
    size?: number;
    sort?: string;
    nome?: string;
    ativo?: string;
    bloqueado?: string;
  }): Observable<LocatarioListResponse> {
    const qp = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        qp.set(key, String(value));
      }
    });
    return this.http.get<LocatarioListResponse>(`${this.baseUrl}?${qp.toString()}`);
  }

  create(request: LocatarioRequest): Observable<LocatarioResponse> {
    const payload = { ...request, dataLimiteAcesso: toIsoDate(request.dataLimiteAcesso) };
    return this.http.post<LocatarioResponse>(this.baseUrl, payload);
  }

  update(id: number, request: LocatarioRequest): Observable<LocatarioResponse> {
    const payload = { ...request, dataLimiteAcesso: toIsoDate(request.dataLimiteAcesso) };
    return this.http.put<LocatarioResponse>(`${this.baseUrl}/${id}`, payload);
  }

  updateAccessLimit(id: number, dataLimiteAcesso: string): Observable<LocatarioResponse> {
    return this.http.patch<LocatarioResponse>(`${this.baseUrl}/${id}/data-limite`, { dataLimiteAcesso: toIsoDate(dataLimiteAcesso) });
  }

  updateStatus(id: number, ativo: boolean): Observable<LocatarioResponse> {
    return this.http.patch<LocatarioResponse>(`${this.baseUrl}/${id}/status`, { ativo });
  }

  get(id: number): Observable<LocatarioResponse> {
    return this.http.get<LocatarioResponse>(`${this.baseUrl}/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  allowed(): Observable<LocatarioResponse[]> {
    return this.http.get<LocatarioResponse[]>(`${this.baseUrl}/allowed`);
  }
}
