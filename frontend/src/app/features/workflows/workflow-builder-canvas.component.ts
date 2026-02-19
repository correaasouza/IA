import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { DragDropModule, CdkDragEnd } from '@angular/cdk/drag-drop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { WorkflowStateDefinition, WorkflowTransitionDefinition } from './models/workflow.models';

interface TransitionViewModel {
  key: string;
  name: string;
  path: string;
  labelX: number;
  labelY: number;
  selected: boolean;
}

@Component({
  selector: 'app-workflow-builder-canvas',
  standalone: true,
  imports: [CommonModule, DragDropModule, MatButtonModule, MatIconModule],
  templateUrl: './workflow-builder-canvas.component.html',
  styleUrls: ['./workflow-builder-canvas.component.css']
})
export class WorkflowBuilderCanvasComponent implements OnChanges {
  private readonly stateWidth = 180;
  private readonly stateHeight = 74;

  @ViewChild('canvasRoot') canvasRoot?: ElementRef<HTMLElement>;
  @Input() states: WorkflowStateDefinition[] = [];
  @Input() transitions: WorkflowTransitionDefinition[] = [];
  @Input() selectedStateKey: string | null = null;
  @Input() selectedTransitionKey: string | null = null;
  @Input() readOnly = false;
  @Output() addState = new EventEmitter<void>();
  @Output() selectState = new EventEmitter<string>();
  @Output() selectTransition = new EventEmitter<string>();
  @Output() moveState = new EventEmitter<{ key: string; x: number; y: number }>();
  @Output() createTransition = new EventEmitter<{ fromStateKey: string; toStateKey: string }>();

  transitionViews: TransitionViewModel[] = [];
  linkSourceStateKey: string | null = null;
  linkHoverStateKey: string | null = null;
  dragLinkActive = false;
  private pointerX = 0;
  private pointerY = 0;

  ngOnChanges(_changes: SimpleChanges): void {
    this.rebuildTransitionViews();
    if (this.linkSourceStateKey && !this.states.some(item => item.key === this.linkSourceStateKey)) {
      this.resetLinkMode();
    }
  }

  onDragEnd(state: WorkflowStateDefinition, event: CdkDragEnd): void {
    if (this.readOnly) {
      return;
    }
    const point = event.source.getFreeDragPosition();
    this.moveState.emit({ key: state.key, x: Math.round(point.x), y: Math.round(point.y) });
  }

  onStateClick(stateKey: string): void {
    if (this.dragLinkActive) {
      return;
    }
    this.selectState.emit(stateKey);
  }

  startLinkDrag(event: MouseEvent, stateKey: string): void {
    event.stopPropagation();
    event.preventDefault();
    if (this.readOnly) {
      return;
    }
    const pointer = this.toCanvasPoint(event.clientX, event.clientY);
    if (!pointer) {
      return;
    }
    this.linkSourceStateKey = stateKey;
    this.linkHoverStateKey = null;
    this.dragLinkActive = true;
    this.pointerX = pointer.x;
    this.pointerY = pointer.y;
  }

  onStateMouseEnter(stateKey: string): void {
    if (!this.dragLinkActive || !this.linkSourceStateKey || stateKey === this.linkSourceStateKey) {
      return;
    }
    this.linkHoverStateKey = stateKey;
  }

  onStateMouseLeave(stateKey: string): void {
    if (!this.dragLinkActive || this.linkHoverStateKey !== stateKey) {
      return;
    }
    this.linkHoverStateKey = null;
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.dragLinkActive) {
      return;
    }
    const pointer = this.toCanvasPoint(event.clientX, event.clientY);
    if (!pointer) {
      return;
    }
    this.pointerX = pointer.x;
    this.pointerY = pointer.y;
  }

  @HostListener('document:mouseup', ['$event'])
  onDocumentMouseUp(event: MouseEvent): void {
    if (!this.dragLinkActive) {
      return;
    }
    event.stopPropagation();
    const source = this.linkSourceStateKey;
    const target = this.linkHoverStateKey;
    if (source && target && source !== target) {
      this.createTransition.emit({ fromStateKey: source, toStateKey: target });
    }
    this.resetLinkMode();
  }

  cancelLink(event?: MouseEvent): void {
    event?.stopPropagation();
    this.resetLinkMode();
  }

  onSelectTransition(event: MouseEvent, transitionKey: string): void {
    event.stopPropagation();
    this.selectTransition.emit(transitionKey);
  }

  trackByState(_index: number, item: WorkflowStateDefinition): string {
    return item.key;
  }

  trackByTransition(_index: number, item: TransitionViewModel): string {
    return item.key;
  }

  isLinkSource(stateKey: string): boolean {
    return this.linkSourceStateKey === stateKey;
  }

  isLinkTarget(stateKey: string): boolean {
    return this.dragLinkActive && this.linkHoverStateKey === stateKey;
  }

  sourceStateName(): string {
    if (!this.linkSourceStateKey) {
      return '';
    }
    return this.states.find(item => item.key === this.linkSourceStateKey)?.name || 'Estado';
  }

  linkPreviewPath(): string | null {
    if (!this.dragLinkActive || !this.linkSourceStateKey) {
      return null;
    }
    const source = this.states.find(item => item.key === this.linkSourceStateKey);
    if (!source) {
      return null;
    }
    const sourceX = Number(source.uiX || 0);
    const sourceY = Number(source.uiY || 0);
    const sourceCenterX = sourceX + this.stateWidth / 2;
    const sourceCenterY = sourceY + this.stateHeight / 2;
    const startX = this.pointerX >= sourceCenterX ? sourceX + this.stateWidth : sourceX;
    const startY = sourceCenterY;
    const endX = this.pointerX;
    const endY = this.pointerY;
    return this.buildPath(startX, startY, endX, endY, 0).path;
  }

  private rebuildTransitionViews(): void {
    const stateByKey = new Map(this.states.map(item => [item.key, item]));
    const laneOffsetByTransitionKey = this.resolveLaneOffsets();
    this.transitionViews = this.transitions
      .map(item => {
        const fromState = stateByKey.get(item.fromStateKey);
        const toState = stateByKey.get(item.toStateKey);
        if (!fromState || !toState) {
          return null;
        }
        const fromX = Number(fromState.uiX || 0);
        const fromY = Number(fromState.uiY || 0);
        const toX = Number(toState.uiX || 0);
        const toY = Number(toState.uiY || 0);

        const leftToRight = toX >= fromX;
        const startX = leftToRight ? fromX + this.stateWidth : fromX;
        const endX = leftToRight ? toX : toX + this.stateWidth;
        const startY = fromY + this.stateHeight / 2;
        const endY = toY + this.stateHeight / 2;
        const laneOffset = laneOffsetByTransitionKey.get(item.key) || 0;
        const geometry = this.buildPath(startX, startY, endX, endY, laneOffset);

        return {
          key: item.key,
          name: (item.name || '').trim() || 'Transicao',
          path: geometry.path,
          labelX: geometry.labelX,
          labelY: geometry.labelY,
          selected: item.key === this.selectedTransitionKey
        } as TransitionViewModel;
      })
      .filter((item): item is TransitionViewModel => !!item);
  }

  private resolveLaneOffsets(): Map<string, number> {
    const pairMap = new Map<string, WorkflowTransitionDefinition[]>();
    for (const transition of this.transitions) {
      const keys = [transition.fromStateKey, transition.toStateKey].sort();
      const pairKey = `${keys[0]}|${keys[1]}`;
      const list = pairMap.get(pairKey) || [];
      list.push(transition);
      pairMap.set(pairKey, list);
    }

    const offsets = new Map<string, number>();
    for (const [, items] of pairMap) {
      const sorted = [...items].sort((a, b) => a.key.localeCompare(b.key));
      const first = sorted[0];
      const second = sorted[1];
      if (
        sorted.length === 2
        && first
        && second
        && first.fromStateKey === second.toStateKey
        && first.toStateKey === second.fromStateKey
      ) {
        offsets.set(first.key, -18);
        offsets.set(second.key, 18);
        continue;
      }
      const center = (sorted.length - 1) / 2;
      sorted.forEach((item, index) => {
        offsets.set(item.key, (index - center) * 14);
      });
    }
    return offsets;
  }

  private buildPath(startX: number, startY: number, endX: number, endY: number, laneOffset: number): {
    path: string;
    labelX: number;
    labelY: number;
  } {
    const dx = endX - startX;
    const dy = endY - startY;
    const length = Math.max(1, Math.hypot(dx, dy));
    const perpX = -dy / length;
    const perpY = dx / length;
    const shiftedStartX = startX + perpX * laneOffset;
    const shiftedStartY = startY + perpY * laneOffset;
    const shiftedEndX = endX + perpX * laneOffset;
    const shiftedEndY = endY + perpY * laneOffset;
    const curvatureBase = Math.max(40, Math.min(140, Math.abs(dx) * 0.35));
    const curvature = curvatureBase + Math.min(24, Math.abs(laneOffset) * 0.5);
    const direction = dx >= 0 ? 1 : -1;
    const control1X = shiftedStartX + direction * curvature;
    const control1Y = shiftedStartY;
    const control2X = shiftedEndX - direction * curvature;
    const control2Y = shiftedEndY;
    const path = `M ${shiftedStartX} ${shiftedStartY} C ${control1X} ${control1Y}, ${control2X} ${control2Y}, ${shiftedEndX} ${shiftedEndY}`;
    const labelX = (shiftedStartX + shiftedEndX) / 2;
    const labelY = (shiftedStartY + shiftedEndY) / 2 - 8;
    return { path, labelX, labelY };
  }

  private toCanvasPoint(clientX: number, clientY: number): { x: number; y: number } | null {
    const rect = this.canvasRoot?.nativeElement.getBoundingClientRect();
    if (!rect) {
      return null;
    }
    return {
      x: clientX - rect.left,
      y: clientY - rect.top
    };
  }

  private resetLinkMode(): void {
    this.dragLinkActive = false;
    this.linkSourceStateKey = null;
    this.linkHoverStateKey = null;
  }
}
