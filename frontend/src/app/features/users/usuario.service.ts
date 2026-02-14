import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UsuarioRequest {
  username: string;
  email?: string;
  password: string;
  ativo: boolean;
  roles: string[];
}

export interface UsuarioResponse {
  id: number;
  username: string;
  email?: string;
  ativo: boolean;
  papeis?: string[];
}
export interface UsuarioUpdateRequest {
  username: string;
  email?: string;
  ativo: boolean;
}
export interface UsuarioPapelResponse {
  papelIds: number[];
  papeis: string[];
}
export interface UsuarioLocatarioAcessoResponse {
  locatarioIds: number[];
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private baseUrl = `${environment.apiBaseUrl}/api/usuarios`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 50): Observable<any> {
    return this.http.get(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  create(request: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.baseUrl, request);
  }

  get(id: number): Observable<UsuarioResponse> {
    return this.http.get<UsuarioResponse>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: UsuarioUpdateRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.baseUrl}/${id}`, request);
  }

  disable(id: number): Observable<UsuarioResponse> {
    return this.http.patch<UsuarioResponse>(`${this.baseUrl}/${id}/disable`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/reset-password`, { newPassword });
  }

  getPapeis(id: number): Observable<UsuarioPapelResponse> {
    return this.http.get<UsuarioPapelResponse>(`${this.baseUrl}/${id}/papeis`);
  }

  setPapeis(id: number, papelIds: number[]): Observable<UsuarioPapelResponse> {
    return this.http.post<UsuarioPapelResponse>(`${this.baseUrl}/${id}/papeis`, { papelIds });
  }

  getLocatarios(id: number): Observable<UsuarioLocatarioAcessoResponse> {
    return this.http.get<UsuarioLocatarioAcessoResponse>(`${this.baseUrl}/${id}/locatarios`);
  }

  setLocatarios(id: number, locatarioIds: number[]): Observable<UsuarioLocatarioAcessoResponse> {
    return this.http.post<UsuarioLocatarioAcessoResponse>(`${this.baseUrl}/${id}/locatarios`, { locatarioIds });
  }
}
