import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { WorkflowStateDefinition } from './models/workflow.models';

@Component({
  selector: 'app-workflow-state-properties-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule
  ],
  templateUrl: './workflow-state-properties-panel.component.html',
  styleUrls: ['./workflow-state-properties-panel.component.css']
})
export class WorkflowStatePropertiesPanelComponent {
  @Input() state: WorkflowStateDefinition | null = null;
  @Input() readOnly = false;
  @Output() stateChange = new EventEmitter<WorkflowStateDefinition>();
  @Output() deleteState = new EventEmitter<string>();

  emitState(): void {
    if (!this.state) {
      return;
    }
    this.stateChange.emit({ ...this.state });
  }
}
