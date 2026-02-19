import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { WorkflowActionConfig, WorkflowTransitionDefinition } from './models/workflow.models';

@Component({
  selector: 'app-workflow-transition-properties-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './workflow-transition-properties-panel.component.html',
  styleUrls: ['./workflow-transition-properties-panel.component.css']
})
export class WorkflowTransitionPropertiesPanelComponent {
  @Input() transition: WorkflowTransitionDefinition | null = null;
  @Input() stateOptions: Array<{ key: string; name: string }> = [];
  @Input() readOnly = false;
  @Output() transitionChange = new EventEmitter<WorkflowTransitionDefinition>();
  @Output() deleteTransition = new EventEmitter<string>();

  emitTransition(): void {
    if (!this.transition) {
      return;
    }
    this.transitionChange.emit({
      ...this.transition,
      actions: [...(this.transition.actions || [])]
    });
  }

  addAction(): void {
    if (!this.transition) {
      return;
    }
    const action: WorkflowActionConfig = {
      type: 'MOVE_STOCK',
      trigger: 'ON_TRANSITION',
      requiresSuccess: true,
      params: {}
    };
    this.transition.actions = [...(this.transition.actions || []), action];
    this.emitTransition();
  }

  removeAction(index: number): void {
    if (!this.transition) {
      return;
    }
    const actions = [...(this.transition.actions || [])];
    actions.splice(index, 1);
    this.transition.actions = actions;
    this.emitTransition();
  }
}
