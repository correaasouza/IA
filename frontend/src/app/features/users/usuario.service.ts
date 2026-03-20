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
export interface UsuarioEmpresaAcessoResponse {
  empresaIds: number[];
}
export interface UsuarioEmpresaPadraoResponse {
  empresaId: number | null;
}
export interface UsuarioEmpresaOpcao {
  id: number;
  tenantId?: number | null;
  razaoSocial: string;
  nomeFantasia?: string | null;
  ativo: boolean;
}
export interface UsuarioPapelDisponivel {
  id: number;
  nome: string;
  descricao?: string;
  ativo: boolean;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private baseUrl = `${environment.apiBaseUrl}/api/usuarios`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 50, tenantId?: number | null): Observable<any> {
    const qp = new URLSearchParams();
    qp.set('page', String(page));
    qp.set('size', String(size));
    if (tenantId !== undefined && tenantId !== null) {
      qp.set('tenantId', String(tenantId));
    }
    return this.http.get(`${this.baseUrl}?${qp.toString()}`);
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

  getPapeisDisponiveis(id: number): Observable<UsuarioPapelDisponivel[]> {
    return this.http.get<UsuarioPapelDisponivel[]>(`${this.baseUrl}/${id}/papeis-disponiveis`);
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

  getEmpresasAcesso(id: number): Observable<UsuarioEmpresaAcessoResponse> {
    return this.http.get<UsuarioEmpresaAcessoResponse>(`${this.baseUrl}/${id}/empresas-acesso`);
  }

  setEmpresasAcesso(id: number, empresaIds: number[]): Observable<UsuarioEmpresaAcessoResponse> {
    return this.http.post<UsuarioEmpresaAcessoResponse>(`${this.baseUrl}/${id}/empresas-acesso`, { empresaIds });
  }

  getEmpresasDisponiveis(id: number): Observable<UsuarioEmpresaOpcao[]> {
    return this.http.get<UsuarioEmpresaOpcao[]>(`${this.baseUrl}/${id}/empresas-disponiveis`);
  }

  getEmpresaPadrao(id: number): Observable<UsuarioEmpresaPadraoResponse> {
    return this.http.get<UsuarioEmpresaPadraoResponse>(`${this.baseUrl}/${id}/empresa-padrao`);
  }

  setEmpresaPadrao(id: number, empresaId: number): Observable<UsuarioEmpresaPadraoResponse> {
    return this.http.put<UsuarioEmpresaPadraoResponse>(`${this.baseUrl}/${id}/empresa-padrao`, { empresaId });
  }
}
