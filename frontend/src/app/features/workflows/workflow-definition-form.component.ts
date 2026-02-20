import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { MovementConfigService, MovimentoConfig } from '../movement-configs/movement-config.service';
import { WorkflowBuilderCanvasComponent } from './workflow-builder-canvas.component';
import { WorkflowJsonImportDialogComponent } from './workflow-json-import-dialog.component';
import { WorkflowStatePropertiesPanelComponent } from './workflow-state-properties-panel.component';
import { WorkflowTransitionPropertiesPanelComponent } from './workflow-transition-properties-panel.component';
import { WorkflowService } from './workflow.service';
import {
  WorkflowActionConfig,
  WorkflowDefinition,
  WorkflowDefinitionUpsertRequest,
  WorkflowOrigin,
  WorkflowStateDefinition,
  WorkflowTransitionDefinition
} from './models/workflow.models';

@Component({
  selector: 'app-workflow-definition-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    AccessControlDirective,
    WorkflowBuilderCanvasComponent,
    WorkflowStatePropertiesPanelComponent,
    WorkflowTransitionPropertiesPanelComponent
  ],
  templateUrl: './workflow-definition-form.component.html',
  styleUrls: ['./workflow-definition-form.component.css']
})
export class WorkflowDefinitionFormComponent implements OnInit {
  @Input() embedded = false;
  @Input() embeddedOrigin: WorkflowOrigin | null = null;
  @Input() embeddedContextType: 'MOVIMENTO_CONFIG' = 'MOVIMENTO_CONFIG';
  @Input() embeddedContextId: number | null = null;
  @Input() embeddedReadOnly = false;

  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo workflow';
  loading = false;
  saving = false;
  definition: WorkflowDefinition | null = null;

  states: WorkflowStateDefinition[] = [];
  transitions: WorkflowTransitionDefinition[] = [];
  selectedStateKey: string | null = null;
  selectedTransitionKey: string | null = null;
  selectedStateModel: WorkflowStateDefinition | null = null;
  selectedTransitionModel: WorkflowTransitionDefinition | null = null;
  stateOptionsList: Array<{ key: string; name: string }> = [];
  itemStatusOptionsList: Array<{ key: string; name: string }> = [];
  private stateNameLookup = new Map<string, string>();
  selectedOrigin: WorkflowOrigin = 'ITEM_MOVIMENTO_ESTOQUE';
  loadingContexts = false;
  movimentoConfigs: MovimentoConfig[] = [];
  selectedContextType: 'MOVIMENTO_CONFIG' = 'MOVIMENTO_CONFIG';
  selectedContextId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workflowService: WorkflowService,
    private movementConfigService: MovementConfigService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    if (this.embedded) {
      this.initializeEmbedded();
      return;
    }
    const id = Number(this.route.snapshot.paramMap.get('id') || 0);
    const isEdit = this.route.snapshot.url.some(item => item.path === 'edit');
    const queryOrigin = (this.route.snapshot.queryParamMap.get('origin') || '').trim().toUpperCase();
    const queryContextType = (this.route.snapshot.queryParamMap.get('contextType') || '').trim().toUpperCase();
    const queryContextId = Number(this.route.snapshot.queryParamMap.get('contextId') || 0);
    this.mode = id > 0 ? (isEdit ? 'edit' : 'view') : 'new';
    this.title = this.mode === 'new'
      ? 'Novo workflow'
      : this.mode === 'edit'
        ? 'Editar workflow'
        : 'Consultar workflow';

    if (id > 0) {
      this.loadDefinition(id);
      this.loadMovimentoConfigs();
      return;
    }
    if (queryOrigin === 'MOVIMENTO_ESTOQUE' || queryOrigin === 'ITEM_MOVIMENTO_ESTOQUE') {
      this.selectedOrigin = queryOrigin as WorkflowOrigin;
    }
    if (queryContextType === 'MOVIMENTO_CONFIG' && queryContextId > 0) {
      this.selectedContextType = 'MOVIMENTO_CONFIG';
      this.selectedContextId = queryContextId;
    }
    this.loadMovimentoConfigs();
    this.bootstrapDefaultDefinition();
    this.loadItemStatusOptions();
  }

  back(): void {
    if (this.embedded) {
      return;
    }
    this.router.navigateByUrl('/configs/workflows');
  }

  toEdit(): void {
    if (this.embedded) {
      return;
    }
    if (!this.definition?.id) {
      return;
    }
    this.router.navigate(['/configs/workflows', this.definition.id, 'edit']);
  }

  save(): void {
    if (!this.canEditDefinition()) {
      return;
    }
    const requiresContext = this.mode === 'new' || this.definition?.contextType === 'MOVIMENTO_CONFIG';
    if (requiresContext && !this.selectedContextId) {
      this.notify.error('Selecione a configuracao de movimento do workflow.');
      return;
    }
    if (!this.states.length) {
      this.notify.error('Adicione ao menos um estado.');
      return;
    }
    const payload = this.buildPayload();
    this.saving = true;
    this.persistDefinition(payload);
  }

  validateDefinition(): void {
    if (this.embedded) {
      return;
    }
    if (!this.definition?.id) {
      this.notify.info('Salve o workflow para validar.');
      return;
    }
    this.workflowService.validateDefinition(this.definition.id, this.buildPayload()).subscribe({
      next: result => {
        if (result.valid) {
          this.notify.success('Workflow valido.');
          return;
        }
        this.notify.error(`Workflow invalido: ${result.errors.join(', ')}`);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel validar o workflow.')
    });
  }

  exportDefinition(): void {
    if (this.embedded) {
      return;
    }
    if (!this.definition?.id) {
      this.notify.info('Salve o workflow antes de exportar.');
      return;
    }
    this.workflowService.exportDefinition(this.definition.id).subscribe({
      next: payload => {
        const content = payload?.definitionJson || '';
        const blob = new Blob([content], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `workflow-${this.definition?.origin?.toLowerCase() || 'definition'}-v${this.definition?.versionNum || 1}.json`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel exportar o workflow.')
    });
  }

  importDefinition(): void {
    if (this.embedded) {
      return;
    }
    const ref = this.dialog.open(WorkflowJsonImportDialogComponent, {
      width: '980px',
      maxWidth: '96vw'
    });
    ref.afterClosed().subscribe((jsonText?: string) => {
      const normalized = (jsonText || '').trim();
      if (!normalized) {
        return;
      }
      this.workflowService.importDefinition({ definitionJson: normalized }).subscribe({
        next: created => {
          this.notify.success('Workflow importado.');
          this.router.navigate(['/configs/workflows', created.id, 'edit']);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel importar JSON.')
      });
    });
  }

  addState(): void {
    if (this.mode === 'view') {
      return;
    }
    const index = this.states.length + 1;
    const key = this.generateClientRefKey();
    const state: WorkflowStateDefinition = {
      key,
      name: `Estado ${index}`,
      color: '#3b82f6',
      isInitial: this.states.length === 0,
      isFinal: false,
      uiX: 30 + (index % 4) * 210,
      uiY: 50 + Math.floor(index / 4) * 120
    };
    this.states = [...this.states, state];
    this.selectedStateKey = state.key;
    this.rebuildStateCache();
    if (state.isInitial) {
      this.enforceSingleInitial(state.key);
    }
    this.syncSelectedModels();
  }

  addTransition(): void {
    if (this.mode === 'view' || this.states.length < 2) {
      return;
    }
    const fromState = this.states[0];
    const toState = this.states[1];
    if (!fromState || !toState) {
      return;
    }
    this.addTransitionBetweenStates(fromState.key, toState.key);
  }

  createTransitionFromCanvas(event: { fromStateKey: string; toStateKey: string }): void {
    if (this.mode === 'view') {
      return;
    }
    this.addTransitionBetweenStates(event.fromStateKey, event.toStateKey);
  }

  selectState(key: string): void {
    this.selectedStateKey = key;
    this.selectedTransitionKey = null;
    this.syncSelectedModels();
  }

  selectTransition(key: string): void {
    this.selectedTransitionKey = key;
    this.selectedStateKey = null;
    this.syncSelectedModels();
  }

  onMoveState(event: { key: string; x: number; y: number }): void {
    this.states = this.states.map(item =>
      item.key === event.key ? { ...item, uiX: event.x, uiY: event.y } : item);
    this.syncSelectedModels();
  }

  onStateColorChange(event: { key: string; color: string }): void {
    if (this.mode === 'view') {
      return;
    }
    const color = (event.color || '').trim();
    if (!color) {
      return;
    }
    this.states = this.states.map(item =>
      item.key === event.key ? { ...item, color } : item);
    this.syncSelectedModels();
  }

  onStateChange(updated: WorkflowStateDefinition): void {
    if (this.mode === 'view') {
      return;
    }
    this.states = this.states.map(item =>
      item.key === this.selectedStateKey ? { ...updated } : item);
    this.rebuildStateCache();
    if (updated.isInitial) {
      this.enforceSingleInitial(updated.key);
    }
    this.syncSelectedModels();
  }

  onDeleteState(key: string): void {
    if (this.mode === 'view') {
      return;
    }
    this.states = this.states.filter(item => item.key !== key);
    this.transitions = this.transitions.filter(item => item.fromStateKey !== key && item.toStateKey !== key);
    if (this.selectedStateKey === key) {
      this.selectedStateKey = null;
    }
    this.rebuildStateCache();
    this.syncSelectedModels();
  }

  onTransitionChange(updated: WorkflowTransitionDefinition): void {
    if (this.mode === 'view') {
      return;
    }
    const effectiveOrigin = this.definition?.origin || this.selectedOrigin;
    const normalizedTransition = {
      ...updated,
      actions: (updated.actions || []).map(action => this.normalizeAction(action, effectiveOrigin))
    };
    this.transitions = this.transitions.map(item =>
      item.key === this.selectedTransitionKey ? normalizedTransition : item);
    this.syncSelectedModels();
  }

  onDeleteTransition(key: string): void {
    if (this.mode === 'view') {
      return;
    }
    this.transitions = this.transitions.filter(item => item.key !== key);
    if (this.selectedTransitionKey === key) {
      this.selectedTransitionKey = null;
    }
    this.syncSelectedModels();
  }

  canEditDefinition(): boolean {
    return this.mode !== 'view';
  }

  onContextChange(): void {
    this.loadItemStatusOptions();
  }

  contextLabel(): string {
    const selected = this.movimentoConfigs.find(item => item.id === this.selectedContextId);
    if (selected) {
      return selected.nome;
    }
    if (this.selectedContextId) {
      return `ID ${this.selectedContextId}`;
    }
    return '-';
  }

  originLabel(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE' ? 'Item Movimento Estoque' : 'Movimento Estoque';
  }

  trackByState(_index: number, item: WorkflowStateDefinition): string {
    return item.key;
  }

  trackByTransition(_index: number, item: WorkflowTransitionDefinition): string {
    return item.key;
  }

  stateNameByKey(key: string): string {
    return this.stateNameLookup.get(key) || 'Estado';
  }

  private loadDefinition(id: number): void {
    this.loading = true;
    this.workflowService.getDefinition(id).subscribe({
      next: definition => {
        this.loading = false;
        this.definition = definition;
        this.patchFromDefinition(definition);
      },
      error: err => {
        this.loading = false;
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar o workflow.');
        if (!this.embedded) {
          this.router.navigateByUrl('/configs/workflows');
        }
      }
    });
  }

  private patchFromDefinition(definition: WorkflowDefinition): void {
    this.selectedOrigin = definition.origin;
    this.selectedContextType = 'MOVIMENTO_CONFIG';
    this.selectedContextId = definition.contextType === 'MOVIMENTO_CONFIG' ? Number(definition.contextId || 0) || null : null;

    this.states = (definition.states || []).map(item => ({
      ...item,
      key: this.normalizeRefKey(item.key),
      uiX: Number(item.uiX || 0),
      uiY: Number(item.uiY || 0)
    }));
    this.transitions = (definition.transitions || []).map(item => ({
      ...item,
      key: this.normalizeRefKey(item.key),
      fromStateKey: this.normalizeRefKey(item.fromStateKey),
      toStateKey: this.normalizeRefKey(item.toStateKey),
      actions: (item.actions || []).map(action => this.normalizeAction(action, definition.origin))
    }));
    this.selectedStateKey = this.states[0]?.key || null;
    this.selectedTransitionKey = null;
    this.rebuildStateCache();
    this.syncSelectedModels();
    this.loadItemStatusOptions();
  }

  private bootstrapDefaultDefinition(): void {
    const stateKey = this.generateClientRefKey();
    const defaultState: WorkflowStateDefinition = {
      key: stateKey,
      name: 'Aberto',
      color: '#64748b',
      isInitial: true,
      isFinal: false,
      uiX: 40,
      uiY: 80
    };
    this.states = [defaultState];
    this.selectedStateKey = defaultState.key;
    this.rebuildStateCache();
    this.syncSelectedModels();
  }

  private enforceSingleInitial(currentKey: string): void {
    this.states = this.states.map(item => ({
      ...item,
      isInitial: item.key === currentKey
    }));
    this.syncSelectedModels();
  }

  private normalizeAction(action: WorkflowActionConfig, origin: WorkflowOrigin = this.selectedOrigin): WorkflowActionConfig {
    if (origin === 'ITEM_MOVIMENTO_ESTOQUE') {
      return {
        type: 'MOVE_STOCK',
        trigger: 'ON_TRANSITION',
        requiresSuccess: true,
        params: action?.params || {}
      };
    }
    if (origin === 'MOVIMENTO_ESTOQUE') {
      const targetStateKey = this.normalizeTargetStateKey(action);
      return {
        type: 'SET_ITEM_STATUS',
        trigger: 'ON_TRANSITION',
        requiresSuccess: true,
        params: targetStateKey ? { targetStateKey } : {}
      };
    }
    return {
      type: action?.type || 'MOVE_STOCK',
      trigger: action?.trigger || 'ON_TRANSITION',
      requiresSuccess: action?.requiresSuccess !== false,
      params: action?.params || {}
    };
  }

  private buildPayload(): WorkflowDefinitionUpsertRequest {
    const origin = this.definition?.origin || this.selectedOrigin;
    const contextId = this.selectedContextId || null;
    const contextType = contextId ? this.selectedContextType : null;
    const name = this.defaultWorkflowName(origin);
    const layout = {
      canvas: {
        zoom: 1,
        offsetX: 0,
        offsetY: 0
      }
    };

    return {
      origin,
      contextType,
      contextId,
      name,
      description: null,
      layoutJson: JSON.stringify(layout),
      states: this.states.map(item => ({
        ...item,
        key: this.normalizeRefKey(item.key),
        name: (item.name || '').trim(),
        color: (item.color || '').trim() || null
      })),
      transitions: this.transitions.map(item => ({
        ...item,
        key: this.normalizeRefKey(item.key),
        name: (item.name || '').trim(),
        fromStateKey: this.normalizeRefKey(item.fromStateKey),
        toStateKey: this.normalizeRefKey(item.toStateKey),
        uiMetaJson: (item.uiMetaJson || '').trim() || null,
        actions: (item.actions || []).map(action => this.normalizeAction(action, origin))
      }))
    };
  }

  private persistDefinition(payload: WorkflowDefinitionUpsertRequest): void {
    if (!this.definition?.id) {
      this.workflowService.createDefinition(payload).subscribe({
        next: saved => {
          this.saving = false;
          this.definition = saved;
          this.mode = this.embeddedReadOnly ? 'view' : 'edit';
          this.title = this.embedded ? this.title : 'Consultar workflow';
          this.notify.success('Workflow salvo.');
          this.patchFromDefinition(saved);
          if (!this.embedded) {
            this.router.navigate(['/configs/workflows', saved.id]);
          }
        },
        error: err => {
          this.saving = false;
          this.notify.error(err?.error?.detail || 'Nao foi possivel salvar o workflow.');
        }
      });
      return;
    }

    this.workflowService.updateDefinition(this.definition.id, payload).subscribe({
      next: saved => {
        this.saving = false;
        this.definition = saved;
        this.mode = this.embeddedReadOnly ? 'view' : 'edit';
        this.title = this.embedded ? this.title : 'Consultar workflow';
        this.notify.success('Workflow salvo.');
        this.patchFromDefinition(saved);
        if (!this.embedded) {
          this.router.navigate(['/configs/workflows', saved.id]);
        }
      },
      error: err => {
        this.saving = false;
        this.notify.error(err?.error?.detail || 'Nao foi possivel salvar o workflow.');
      }
    });
  }

  private defaultWorkflowName(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE'
      ? 'Workflow Item Movimento Estoque'
      : 'Workflow Movimento Estoque';
  }

  private normalizeRefKey(value?: string | null): string {
    const normalized = (value || '').trim().toUpperCase();
    if (!normalized) {
      return this.generateClientRefKey();
    }
    return normalized.length > 80 ? normalized.slice(0, 80) : normalized;
  }

  private generateClientRefKey(): string {
    const uuid = globalThis.crypto?.randomUUID?.();
    if (uuid) {
      return uuid.toUpperCase();
    }
    return `WF_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`.toUpperCase();
  }

  private rebuildStateCache(): void {
    this.stateOptionsList = this.states.map(item => ({
      key: item.key,
      name: (item.name || '').trim() || 'Estado'
    }));
    this.stateNameLookup = new Map(this.stateOptionsList.map(item => [item.key, item.name]));
  }

  private syncSelectedModels(): void {
    this.selectedStateModel = this.selectedStateKey
      ? this.states.find(item => item.key === this.selectedStateKey) || null
      : null;
    this.selectedTransitionModel = this.selectedTransitionKey
      ? this.transitions.find(item => item.key === this.selectedTransitionKey) || null
      : null;
  }

  private addTransitionBetweenStates(fromStateKey: string, toStateKey: string): void {
    if (!fromStateKey || !toStateKey || fromStateKey === toStateKey) {
      return;
    }
    const fromExists = this.states.some(item => item.key === fromStateKey);
    const toExists = this.states.some(item => item.key === toStateKey);
    if (!fromExists || !toExists) {
      return;
    }

    const key = this.generateClientRefKey();
    const transition: WorkflowTransitionDefinition = {
      key,
      name: `Transicao ${this.transitions.length + 1}`,
      fromStateKey,
      toStateKey,
      enabled: true,
      priority: 100,
      uiMetaJson: '',
      actions: []
    };
    this.transitions = [...this.transitions, transition];
    this.selectedTransitionKey = transition.key;
    this.selectedStateKey = null;
    this.syncSelectedModels();
  }

  private loadMovimentoConfigs(): void {
    if (this.embedded) {
      return;
    }
    this.loadingContexts = true;
    this.movementConfigService.listByTipo('MOVIMENTO_ESTOQUE', 0, 500).subscribe({
      next: page => {
        this.loadingContexts = false;
        this.movimentoConfigs = (page?.content || []).filter(item => item?.id && item.ativo !== false);
        if (!this.selectedContextId) {
          this.selectedContextId = this.movimentoConfigs[0]?.id || null;
        }
        this.loadItemStatusOptions();
      },
      error: err => {
        this.loadingContexts = false;
        this.movimentoConfigs = [];
        this.selectedContextId = null;
        this.itemStatusOptionsList = [];
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar configuracoes de movimento.');
      }
    });
  }

  private loadItemStatusOptions(): void {
    const effectiveOrigin = this.definition?.origin || this.selectedOrigin;
    if (effectiveOrigin !== 'MOVIMENTO_ESTOQUE' || !this.selectedContextId) {
      this.itemStatusOptionsList = [];
      return;
    }
    this.workflowService.getDefinitionByOrigin('ITEM_MOVIMENTO_ESTOQUE', {
      type: this.selectedContextType,
      id: this.selectedContextId
    }).subscribe({
      next: definition => {
        this.itemStatusOptionsList = (definition?.states || []).map(state => ({
          key: this.normalizeOptionalRefKey(state.key),
          name: (state.name || '').trim() || 'Estado'
        })).filter(item => !!item.key) as Array<{ key: string; name: string }>;
      },
      error: () => {
        this.itemStatusOptionsList = [];
      }
    });
  }

  private normalizeTargetStateKey(action: WorkflowActionConfig | null | undefined): string | null {
    const raw = (action?.params?.['targetStateKey'] as string | null | undefined) || '';
    return this.normalizeOptionalRefKey(raw);
  }

  private normalizeOptionalRefKey(value?: string | null): string | null {
    const normalized = (value || '').trim().toUpperCase();
    if (!normalized) {
      return null;
    }
    return normalized.length > 80 ? normalized.slice(0, 80) : normalized;
  }

  private initializeEmbedded(): void {
    const origin = this.embeddedOrigin || 'MOVIMENTO_ESTOQUE';
    const contextId = Number(this.embeddedContextId || 0);
    this.selectedOrigin = origin;
    this.selectedContextType = this.embeddedContextType || 'MOVIMENTO_CONFIG';
    this.selectedContextId = contextId > 0 ? contextId : null;
    this.mode = this.embeddedReadOnly ? 'view' : 'edit';
    this.title = `Workflow ${this.originLabel(origin)}`;

    if (!this.selectedContextId) {
      this.definition = null;
      this.states = [];
      this.transitions = [];
      this.selectedStateKey = null;
      this.selectedTransitionKey = null;
      this.selectedStateModel = null;
      this.selectedTransitionModel = null;
      this.stateOptionsList = [];
      this.itemStatusOptionsList = [];
      return;
    }

    this.loading = true;
    this.workflowService.getDefinitionByOrigin(origin, {
      type: this.selectedContextType,
      id: this.selectedContextId
    }).subscribe({
      next: definition => {
        this.loading = false;
        if (!definition?.id) {
          this.definition = null;
          this.bootstrapDefaultDefinition();
          this.loadItemStatusOptions();
          return;
        }
        this.definition = definition;
        this.patchFromDefinition(definition);
      },
      error: err => {
        this.loading = false;
        if (err?.status === 404) {
          this.definition = null;
          this.bootstrapDefaultDefinition();
          this.loadItemStatusOptions();
          return;
        }
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar o workflow.');
      }
    });
  }
}
