import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Contato {
  id: number;
  entidadeRegistroId: number;
  tipo: string;
  valor: string;
  principal: boolean;
}

@Injectable({ providedIn: 'root' })
export class ContatoService {
  private baseUrl = `${environment.apiBaseUrl}/api/contatos`;

  constructor(private http: HttpClient) {}

  list(entidadeRegistroId: number): Observable<Contato[]> {
    return this.http.get<Contato[]>(`${this.baseUrl}?entidadeRegistroId=${entidadeRegistroId}`);
  }

  create(payload: any): Observable<Contato> {
    return this.http.post<Contato>(this.baseUrl, payload);
  }

  update(id: number, payload: any): Observable<Contato> {
    return this.http.put<Contato>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
