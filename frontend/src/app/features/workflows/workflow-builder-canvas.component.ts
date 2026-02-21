import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { DragDropModule, CdkDragEnd, CdkDragMove } from '@angular/cdk/drag-drop';
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
  private readonly stateHorizontalPadding = 80;
  private readonly stateVerticalPadding = 120;
  private readonly arrowGap = 8;
  private readonly minCanvasWidth = 1200;
  private readonly maxCanvasWidth = 8000;
  private readonly maxCanvasHeight = 8000;

  @ViewChild('canvasRoot') canvasRoot?: ElementRef<HTMLElement>;
  @Input() states: WorkflowStateDefinition[] = [];
  @Input() transitions: WorkflowTransitionDefinition[] = [];
  @Input() selectedStateKey: string | null = null;
  @Input() selectedTransitionKey: string | null = null;
  @Input() minHeightPx: number | null = null;
  @Input() readOnly = false;
  @Output() addState = new EventEmitter<void>();
  @Output() selectState = new EventEmitter<string>();
  @Output() selectTransition = new EventEmitter<string>();
  @Output() moveState = new EventEmitter<{ key: string; x: number; y: number }>();
  @Output() createTransition = new EventEmitter<{ fromStateKey: string; toStateKey: string }>();
  @Output() updateStateColor = new EventEmitter<{ key: string; color: string }>();

  transitionViews: TransitionViewModel[] = [];
  canvasWidthPx = this.minCanvasWidth;
  canvasHeightPx = 460;
  linkSourceStateKey: string | null = null;
  linkHoverStateKey: string | null = null;
  dragLinkActive = false;
  private dragStateKey: string | null = null;
  private dragStateX = 0;
  private dragStateY = 0;
  private pointerX = 0;
  private pointerY = 0;

  ngOnChanges(_changes: SimpleChanges): void {
    this.rebuildCanvasBounds();
    this.rebuildTransitionViews();
    if (this.linkSourceStateKey && !this.states.some(item => item.key === this.linkSourceStateKey)) {
      this.resetLinkMode();
    }
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.rebuildCanvasBounds();
  }

  onDragEnd(state: WorkflowStateDefinition, event: CdkDragEnd): void {
    if (this.readOnly) {
      return;
    }
    const point = event.source.getFreeDragPosition();
    const x = this.clampPosition(Math.round(point.x), this.maxCanvasWidth);
    const y = this.clampPosition(Math.round(point.y), this.maxCanvasHeight);
    event.source.setFreeDragPosition({ x, y });
    this.dragStateKey = null;
    this.rebuildCanvasBounds(state.key, x, y);
    this.moveState.emit({ key: state.key, x, y });
  }

  onDragMove(state: WorkflowStateDefinition, event: CdkDragMove): void {
    const point = event.source.getFreeDragPosition();
    const x = this.clampPosition(Math.round(point.x), this.maxCanvasWidth);
    const y = this.clampPosition(Math.round(point.y), this.maxCanvasHeight);
    this.dragStateKey = state.key;
    this.dragStateX = x;
    this.dragStateY = y;
    this.rebuildCanvasBounds(state.key, x, y);
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

  stateColorValue(state: WorkflowStateDefinition): string {
    const color = (state.color || '').trim();
    return /^#[\da-fA-F]{6}$/.test(color) ? color : '#3b82f6';
  }

  onColorPickerPointer(event: Event): void {
    event.stopPropagation();
  }

  onStateColorInput(event: Event, stateKey: string): void {
    event.stopPropagation();
    const color = ((event.target as HTMLInputElement | null)?.value || '').trim();
    if (!color) {
      return;
    }
    this.updateStateColor.emit({ key: stateKey, color });
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
    const sourceAnchor = this.resolveAnchorForPoint(sourceX, sourceY, this.pointerX, this.pointerY);
    const dx = this.pointerX - sourceAnchor.x;
    const dy = this.pointerY - sourceAnchor.y;
    const len = Math.max(1, Math.hypot(dx, dy));
    const startX = sourceAnchor.x + (dx / len) * this.arrowGap;
    const startY = sourceAnchor.y + (dy / len) * this.arrowGap;
    const endX = this.pointerX;
    const endY = this.pointerY;
    return this.buildPath(startX, startY, endX, endY, 0, 0).path;
  }

  private rebuildTransitionViews(): void {
    const stateByKey = new Map(this.states.map(item => [item.key, item]));
    const laneOffsetByTransitionKey = this.resolveLaneOffsets();
    const labelOffsetByTransitionKey = this.resolveLabelOffsets();
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
        const anchors = this.resolveAnchors(fromX, fromY, toX, toY);
        const laneOffset = laneOffsetByTransitionKey.get(item.key) || 0;
        const labelOffset = labelOffsetByTransitionKey.get(item.key) ?? laneOffset;
        const geometry = this.buildPath(
          anchors.startX,
          anchors.startY,
          anchors.endX,
          anchors.endY,
          laneOffset,
          labelOffset
        );

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

  private resolveLabelOffsets(): Map<string, number> {
    const pairMap = new Map<string, WorkflowTransitionDefinition[]>();
    for (const transition of this.transitions) {
      const keys = [transition.fromStateKey, transition.toStateKey].sort();
      const pairKey = `${keys[0]}|${keys[1]}`;
      const list = pairMap.get(pairKey) || [];
      list.push(transition);
      pairMap.set(pairKey, list);
    }

    const offsets = new Map<string, number>();
    for (const [pairKey, items] of pairMap) {
      const [leftKey, rightKey] = pairKey.split('|');
      if (items.length === 2) {
        for (const item of items) {
          const forward = item.fromStateKey === leftKey && item.toStateKey === rightKey;
          offsets.set(item.key, forward ? -28 : 28);
        }
        continue;
      }
      const sorted = [...items].sort((a, b) => a.key.localeCompare(b.key));
      const center = (sorted.length - 1) / 2;
      sorted.forEach((item, index) => {
        offsets.set(item.key, (index - center) * 20);
      });
    }
    return offsets;
  }

  private buildPath(
    startX: number,
    startY: number,
    endX: number,
    endY: number,
    laneOffset: number,
    labelOffset: number
  ): {
    path: string;
    labelX: number;
    labelY: number;
  } {
    const dx = endX - startX;
    const dy = endY - startY;
    const length = Math.max(1, Math.hypot(dx, dy));
    let canonicalDx = dx;
    let canonicalDy = dy;
    if (canonicalDx < 0 || (canonicalDx === 0 && canonicalDy < 0)) {
      canonicalDx *= -1;
      canonicalDy *= -1;
    }
    const canonicalLen = Math.max(1, Math.hypot(canonicalDx, canonicalDy));
    const lanePerpX = -canonicalDy / canonicalLen;
    const lanePerpY = canonicalDx / canonicalLen;
    const shiftedStartX = startX + lanePerpX * laneOffset;
    const shiftedStartY = startY + lanePerpY * laneOffset;
    const shiftedEndX = endX + lanePerpX * laneOffset;
    const shiftedEndY = endY + lanePerpY * laneOffset;
    const isHorizontal = Math.abs(dx) >= Math.abs(dy);
    const curvatureBase = Math.max(40, Math.min(140, (isHorizontal ? Math.abs(dx) : Math.abs(dy)) * 0.35));
    const curvature = curvatureBase + Math.min(24, Math.abs(laneOffset) * 0.5);
    let control1X = shiftedStartX;
    let control1Y = shiftedStartY;
    let control2X = shiftedEndX;
    let control2Y = shiftedEndY;
    if (isHorizontal) {
      const direction = dx >= 0 ? 1 : -1;
      control1X = shiftedStartX + direction * curvature;
      control2X = shiftedEndX - direction * curvature;
    } else {
      const direction = dy >= 0 ? 1 : -1;
      control1Y = shiftedStartY + direction * curvature;
      control2Y = shiftedEndY - direction * curvature;
    }
    const path = `M ${shiftedStartX} ${shiftedStartY} C ${control1X} ${control1Y}, ${control2X} ${control2Y}, ${shiftedEndX} ${shiftedEndY}`;

    const labelX = (startX + endX) / 2 + lanePerpX * labelOffset;
    const labelY = (startY + endY) / 2 + lanePerpY * labelOffset - 8;
    return { path, labelX, labelY };
  }

  private toCanvasPoint(clientX: number, clientY: number): { x: number; y: number } | null {
    const root = this.canvasRoot?.nativeElement;
    const rect = root?.getBoundingClientRect();
    if (!rect) {
      return null;
    }
    return {
      x: (root?.scrollLeft || 0) + clientX - rect.left,
      y: (root?.scrollTop || 0) + clientY - rect.top
    };
  }

  private resetLinkMode(): void {
    this.dragLinkActive = false;
    this.linkSourceStateKey = null;
    this.linkHoverStateKey = null;
  }

  private rebuildCanvasBounds(overrideStateKey?: string, overrideX?: number, overrideY?: number): void {
    const root = this.canvasRoot?.nativeElement;
    const baseWidth = Math.max(this.minCanvasWidth, root?.clientWidth || 0);
    const baseHeight = Math.max(360, Number(this.minHeightPx || 0));
    let maxWidth = baseWidth;
    let maxHeight = baseHeight;
    for (const state of this.states) {
      const hasExplicitOverride = !!overrideStateKey && state?.key === overrideStateKey;
      const hasDragOverride = !hasExplicitOverride && !!this.dragStateKey && state?.key === this.dragStateKey;
      const x = hasExplicitOverride
        ? Number(overrideX || 0)
        : hasDragOverride
          ? Number(this.dragStateX || 0)
          : Number(state?.uiX || 0);
      const y = hasExplicitOverride
        ? Number(overrideY || 0)
        : hasDragOverride
          ? Number(this.dragStateY || 0)
          : Number(state?.uiY || 0);
      maxWidth = Math.max(maxWidth, x + this.stateWidth + this.stateHorizontalPadding);
      maxHeight = Math.max(maxHeight, y + this.stateHeight + this.stateVerticalPadding);
    }
    this.canvasWidthPx = Math.min(this.maxCanvasWidth, Math.max(baseWidth, Math.ceil(maxWidth)));
    this.canvasHeightPx = Math.min(this.maxCanvasHeight, Math.max(baseHeight, Math.ceil(maxHeight)));
  }

  private clampPosition(value: number, limit: number): number {
    if (!Number.isFinite(value)) {
      return 16;
    }
    return Math.min(limit, Math.max(16, value));
  }

  private resolveAnchors(fromX: number, fromY: number, toX: number, toY: number): {
    startX: number;
    startY: number;
    endX: number;
    endY: number;
  } {
    const toCenterX = toX + this.stateWidth / 2;
    const toCenterY = toY + this.stateHeight / 2;
    const fromCenterX = fromX + this.stateWidth / 2;
    const fromCenterY = fromY + this.stateHeight / 2;
    const fromAnchor = this.resolveAnchorForPoint(fromX, fromY, toCenterX, toCenterY);
    const toAnchor = this.resolveAnchorForPoint(toX, toY, fromCenterX, fromCenterY);
    const dx = toAnchor.x - fromAnchor.x;
    const dy = toAnchor.y - fromAnchor.y;
    const len = Math.max(1, Math.hypot(dx, dy));
    const ux = dx / len;
    const uy = dy / len;
    return {
      startX: fromAnchor.x + ux * this.arrowGap,
      startY: fromAnchor.y + uy * this.arrowGap,
      endX: toAnchor.x - ux * this.arrowGap,
      endY: toAnchor.y - uy * this.arrowGap
    };
  }

  private resolveAnchorForPoint(
    stateX: number,
    stateY: number,
    targetX: number,
    targetY: number
  ): { x: number; y: number } {
    const centerX = stateX + this.stateWidth / 2;
    const centerY = stateY + this.stateHeight / 2;
    const dx = targetX - centerX;
    const dy = targetY - centerY;
    if (Math.abs(dx) >= Math.abs(dy)) {
      return {
        x: dx >= 0 ? stateX + this.stateWidth : stateX,
        y: centerY
      };
    }
    return {
      x: centerX,
      y: dy >= 0 ? stateY + this.stateHeight : stateY
    };
  }
}
