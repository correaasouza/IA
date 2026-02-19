export type WorkflowOrigin = 'MOVIMENTO_ESTOQUE' | 'ITEM_MOVIMENTO_ESTOQUE';
export type WorkflowDefinitionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type WorkflowActionType = 'MOVE_STOCK';
export type WorkflowTriggerType = 'ON_TRANSITION' | 'ON_ENTER_STATE';

export interface WorkflowActionConfig {
  type: WorkflowActionType;
  trigger: WorkflowTriggerType;
  requiresSuccess: boolean;
  params?: Record<string, unknown>;
}

export interface WorkflowStateDefinition {
  id?: number;
  key: string;
  name: string;
  color?: string | null;
  isInitial: boolean;
  isFinal: boolean;
  uiX: number;
  uiY: number;
  metadataJson?: string | null;
}

export interface WorkflowTransitionDefinition {
  id?: number;
  key: string;
  name: string;
  fromStateKey: string;
  toStateKey: string;
  enabled: boolean;
  priority: number;
  uiMetaJson?: string | null;
  actions: WorkflowActionConfig[];
}

export interface WorkflowDefinition {
  id: number;
  origin: WorkflowOrigin;
  name: string;
  versionNum: number;
  status: WorkflowDefinitionStatus;
  description?: string | null;
  layoutJson?: string | null;
  publishedAt?: string | null;
  publishedBy?: string | null;
  active: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
  states: WorkflowStateDefinition[];
  transitions: WorkflowTransitionDefinition[];
}

export interface WorkflowDefinitionUpsertRequest {
  origin: WorkflowOrigin;
  name: string;
  description?: string | null;
  layoutJson?: string | null;
  states: WorkflowStateDefinition[];
  transitions: WorkflowTransitionDefinition[];
}

export interface WorkflowValidationResponse {
  valid: boolean;
  errors: string[];
}

export interface WorkflowImportRequest {
  definitionJson: string;
}

export interface WorkflowPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface WorkflowAvailableTransition {
  key: string;
  name: string;
  toStateKey: string;
  toStateName?: string | null;
}

export interface WorkflowRuntimeState {
  instanceId: number | null;
  origin: WorkflowOrigin;
  entityId: number;
  currentStateKey: string | null;
  currentStateName?: string | null;
  definitionVersion: number | null;
  updatedAt?: string | null;
  transitions: WorkflowAvailableTransition[];
}

export interface WorkflowTransitionRequest {
  transitionKey: string;
  notes?: string | null;
  expectedCurrentStateKey?: string | null;
  clientRequestId?: string | null;
}

export interface WorkflowActionExecutionResult {
  type: string;
  status: string;
  executionKey: string;
  result?: string | null;
  errorMessage?: string | null;
}

export interface WorkflowTransitionResponse {
  instanceId: number;
  origin: WorkflowOrigin;
  entityId: number;
  fromState: string;
  toState: string;
  transitionKey: string;
  changedAt: string;
  changedBy: string;
  actions: WorkflowActionExecutionResult[];
}

export interface WorkflowHistoryEntry {
  id: number;
  origin: WorkflowOrigin;
  entityId: number;
  fromStateKey: string;
  toStateKey: string;
  transitionKey: string;
  triggeredBy: string;
  triggeredAt: string;
  notes?: string | null;
  actionResultsJson?: string | null;
  success: boolean;
}
