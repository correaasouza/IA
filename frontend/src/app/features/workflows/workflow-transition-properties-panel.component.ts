import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { WorkflowActionConfig, WorkflowOrigin, WorkflowTransitionDefinition } from './models/workflow.models';

@Component({
  selector: 'app-workflow-transition-properties-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    AccessControlDirective
  ],
  templateUrl: './workflow-transition-properties-panel.component.html',
  styleUrls: ['./workflow-transition-properties-panel.component.css']
})
export class WorkflowTransitionPropertiesPanelComponent {
  @Input() transition: WorkflowTransitionDefinition | null = null;
  @Input() transitionControlKey = '';
  @Input() stateOptions: Array<{ key: string; name: string }> = [];
  @Input() itemStatusOptions: Array<{ key: string; name: string }> = [];
  @Input() origin: WorkflowOrigin = 'ITEM_MOVIMENTO_ESTOQUE';
  @Input() readOnly = false;
  @Output() transitionChange = new EventEmitter<WorkflowTransitionDefinition>();
  @Output() deleteTransition = new EventEmitter<string>();

  emitTransition(): void {
    if (!this.transition) {
      return;
    }
    const currentActions = this.transition.actions || [];
    const normalizedActions = this.isItemStockWorkflow()
      ? this.normalizeItemActions(currentActions)
      : this.isMovementWorkflow()
        ? this.normalizeMovementActions(currentActions)
        : [...currentActions];
    this.transitionChange.emit({
      ...this.transition,
      actions: normalizedActions
    });
  }

  itemActionType(): 'NONE' | 'MOVE_STOCK' | 'UNDO_STOCK' {
    const action = (this.transition?.actions || [])[0];
    const normalized = (action?.type || '').trim().toUpperCase();
    if (normalized === 'UNDO_STOCK') {
      return 'UNDO_STOCK';
    }
    if (normalized === 'MOVE_STOCK') {
      return 'MOVE_STOCK';
    }
    return 'NONE';
  }

  onItemActionTypeChange(value: 'NONE' | 'MOVE_STOCK' | 'UNDO_STOCK'): void {
    if (!this.transition || this.readOnly) {
      return;
    }
    if (value === 'NONE') {
      this.transition.actions = [];
      this.emitTransition();
      return;
    }
    this.transition.actions = [this.normalizeItemAction({
      type: value,
      trigger: 'ON_TRANSITION',
      requiresSuccess: true,
      params: {}
    })];
    this.emitTransition();
  }

  hasSetItemStatusAction(): boolean {
    return (this.transition?.actions || []).some(action => action?.type === 'SET_ITEM_STATUS');
  }

  setSetItemStatusAction(enabled: boolean): void {
    if (!this.transition || this.readOnly) {
      return;
    }
    this.transition.actions = enabled ? [this.normalizeMovementAction(null)] : [];
    this.emitTransition();
  }

  toggleSetItemStatusAction(): void {
    if (!this.transition || this.readOnly) {
      return;
    }
    this.setSetItemStatusAction(!this.hasSetItemStatusAction());
  }

  itemStatusTargetStateKey(): string {
    const action = (this.transition?.actions || []).find(item => item?.type === 'SET_ITEM_STATUS');
    return this.readTargetStateKey(action) || '';
  }

  onItemStatusTargetStateKeyChange(value: string): void {
    if (!this.transition || this.readOnly) {
      return;
    }
    this.transition.actions = [this.normalizeMovementAction({
      type: 'SET_ITEM_STATUS',
      trigger: 'ON_TRANSITION',
      requiresSuccess: true,
      params: { targetStateKey: value || null }
    })];
    this.emitTransition();
  }

  isItemStockWorkflow(): boolean {
    return this.origin === 'ITEM_MOVIMENTO_ESTOQUE';
  }

  isMovementWorkflow(): boolean {
    return this.origin === 'MOVIMENTO_ESTOQUE';
  }

  private normalizeItemActions(actions: WorkflowActionConfig[]): WorkflowActionConfig[] {
    if (!actions.length) {
      return [];
    }
    return [this.normalizeItemAction(actions[0])];
  }

  private normalizeMovementActions(actions: WorkflowActionConfig[]): WorkflowActionConfig[] {
    if (!actions.length) {
      return [];
    }
    return [this.normalizeMovementAction(actions[0])];
  }

  private normalizeItemAction(action: WorkflowActionConfig | null | undefined): WorkflowActionConfig {
    const actionType = (action?.type || '').trim().toUpperCase() === 'UNDO_STOCK'
      ? 'UNDO_STOCK'
      : 'MOVE_STOCK';
    return {
      type: actionType,
      trigger: 'ON_TRANSITION',
      requiresSuccess: true,
      params: action?.params || {}
    };
  }

  private normalizeMovementAction(action: WorkflowActionConfig | null | undefined): WorkflowActionConfig {
    const targetStateKey = this.readTargetStateKey(action);
    return {
      type: 'SET_ITEM_STATUS',
      trigger: 'ON_TRANSITION',
      requiresSuccess: true,
      params: targetStateKey ? { targetStateKey } : {}
    };
  }

  private readTargetStateKey(action: WorkflowActionConfig | null | undefined): string | null {
    const raw = (action?.params?.['targetStateKey'] as string | null | undefined) || '';
    const normalized = raw.trim().toUpperCase();
    return normalized || null;
  }
}
