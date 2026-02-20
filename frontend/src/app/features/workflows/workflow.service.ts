import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  WorkflowDefinition,
  WorkflowDefinitionUpsertRequest,
  WorkflowHistoryEntry,
  WorkflowImportRequest,
  WorkflowContextType,
  WorkflowOrigin,
  WorkflowPage,
  WorkflowRuntimeState,
  WorkflowTransitionRequest,
  WorkflowTransitionResponse,
  WorkflowValidationResponse
} from './models/workflow.models';

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly definitionsUrl = `${environment.apiBaseUrl}/api/workflows/definitions`;
  private readonly runtimeUrl = `${environment.apiBaseUrl}/api/workflows/runtime`;

  constructor(private http: HttpClient) {}

  getDefinition(id: number): Observable<WorkflowDefinition> {
    return this.http.get<WorkflowDefinition>(`${this.definitionsUrl}/${id}`);
  }

  getDefinitionByOrigin(
      origin: WorkflowOrigin,
      context?: { type?: WorkflowContextType | null; id?: number | null }): Observable<WorkflowDefinition | null> {
    let params = new HttpParams().set('origin', origin);
    const type = (context?.type || '').trim();
    const id = Number(context?.id || 0);
    if (type) {
      params = params.set('contextType', type);
    }
    if (id > 0) {
      params = params.set('contextId', String(id));
    }
    return this.http.get<WorkflowDefinition>(`${this.definitionsUrl}/by-origin`, { params });
  }

  createDefinition(payload: WorkflowDefinitionUpsertRequest): Observable<WorkflowDefinition> {
    return this.http.post<WorkflowDefinition>(this.definitionsUrl, payload);
  }

  updateDefinition(id: number, payload: WorkflowDefinitionUpsertRequest): Observable<WorkflowDefinition> {
    return this.http.put<WorkflowDefinition>(`${this.definitionsUrl}/${id}`, payload);
  }

  validateDefinition(id: number, payload?: WorkflowDefinitionUpsertRequest): Observable<WorkflowValidationResponse> {
    return this.http.post<WorkflowValidationResponse>(`${this.definitionsUrl}/${id}/validate`, payload || {});
  }

  exportDefinition(id: number): Observable<{ definitionJson: string }> {
    return this.http.get<{ definitionJson: string }>(`${this.definitionsUrl}/${id}/export`);
  }

  importDefinition(payload: WorkflowImportRequest): Observable<WorkflowDefinition> {
    return this.http.post<WorkflowDefinition>(`${this.definitionsUrl}/import`, payload);
  }

  getRuntimeState(origin: WorkflowOrigin, entityId: number): Observable<WorkflowRuntimeState> {
    return this.http.get<any>(`${this.runtimeUrl}/${origin}/${entityId}`).pipe(
      map((payload: any) => ({
        instanceId: payload?.instanceId ?? null,
        origin: payload?.origin ?? origin,
        entityId: payload?.entityId ?? entityId,
        currentStateKey: payload?.currentStateKey ?? null,
        currentStateName: payload?.currentStateName ?? null,
        definitionVersion: payload?.definitionVersion ?? null,
        updatedAt: payload?.updatedAt ?? null,
        transitions: payload?.transitions ?? payload?.availableTransitions ?? []
      }))
    );
  }

  transition(origin: WorkflowOrigin, entityId: number, payload: WorkflowTransitionRequest): Observable<WorkflowTransitionResponse> {
    return this.http.post<WorkflowTransitionResponse>(`${this.runtimeUrl}/${origin}/${entityId}/transition`, payload);
  }

  listHistory(origin: WorkflowOrigin, entityId: number, page = 0, size = 20): Observable<WorkflowPage<WorkflowHistoryEntry>> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<WorkflowPage<WorkflowHistoryEntry>>(`${this.runtimeUrl}/${origin}/${entityId}/history`, { params });
  }
}
