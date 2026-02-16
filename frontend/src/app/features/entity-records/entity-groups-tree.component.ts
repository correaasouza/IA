import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { EntityGroupService, GrupoEntidadeNode } from './entity-group.service';

@Component({
  selector: 'app-entity-groups-tree',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './entity-groups-tree.component.html',
  styleUrls: ['./entity-groups-tree.component.css']
})
export class EntityGroupsTreeComponent implements OnChanges {
  @Input() tipoEntidadeId: number | null = null;
  @Input() canManage = false;
  @Input() allowEntityDrop = false;
  @Input() selectedGroupId: number | null = null;
  @Output() changed = new EventEmitter<void>();
  @Output() selectedGroupIdChange = new EventEmitter<number | null>();
  @Output() entityDroppedOnGroup = new EventEmitter<{ entityId: number; groupId: number | null }>();

  loading = false;
  nodes: GrupoEntidadeNode[] = [];
  private expandedMap: Record<number, boolean> = {};
  private nodeById: Record<number, GrupoEntidadeNode> = {};
  private parentById: Record<number, number | null> = {};
  private siblingsByParent: Record<string, number[]> = {};
  private dragOverGroupId: number | null = null;
  private dragOverAll = false;

  constructor(
      private service: EntityGroupService,
      private notify: NotificationService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tipoEntidadeId']) {
      this.load();
    }
  }

  load(): void {
    if (!this.tipoEntidadeId || !this.canManage) {
      this.nodes = [];
      return;
    }
    this.loading = true;
    this.service.tree(this.tipoEntidadeId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: data => {
          this.nodes = data || [];
          this.rebuildNodeIndex(this.nodes);
          this.seedExpanded();
        },
        error: err => {
          this.nodes = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar grupos de entidade.');
        }
      });
  }

  addRoot(): void {
    if (!this.tipoEntidadeId || !this.canManage) return;
    this.createGroupWithPrompt(null);
  }

  addChild(node: GrupoEntidadeNode): void {
    if (!this.tipoEntidadeId || !this.canManage) return;
    const nome = (prompt(`Novo subgrupo de "${node.nome}":`) || '').trim();
    if (!nome) return;
    this.service.create(this.tipoEntidadeId, nome, node.id).subscribe({
      next: () => {
        this.notify.success('Subgrupo criado.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar subgrupo.')
    });
  }

  rename(node: GrupoEntidadeNode): void {
    if (!this.tipoEntidadeId || !this.canManage) return;
    const nome = (prompt('Novo nome do grupo:', node.nome) || '').trim();
    if (!nome || nome === node.nome) return;
    this.service.update(this.tipoEntidadeId, node.id, { nome, parentId: node.parentId, ordem: node.ordem }).subscribe({
      next: () => {
        this.notify.success('Grupo atualizado.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar grupo.')
    });
  }

  remove(node: GrupoEntidadeNode): void {
    if (!this.tipoEntidadeId || !this.canManage) return;
    if (!confirm(`Excluir grupo "${node.nome}"?`)) return;
    this.service.delete(this.tipoEntidadeId, node.id).subscribe({
      next: () => {
        this.notify.success('Grupo removido.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover grupo.')
    });
  }

  selectGroup(groupId: number | null): void {
    this.selectedGroupId = groupId;
    this.selectedGroupIdChange.emit(groupId);
  }

  isSelected(node: GrupoEntidadeNode): boolean {
    return !!this.selectedGroupId && node.id === this.selectedGroupId;
  }

  hasChildren(node: GrupoEntidadeNode): boolean {
    return !!node.children && node.children.length > 0;
  }

  isExpanded(node: GrupoEntidadeNode): boolean {
    return this.expandedMap[node.id] !== false;
  }

  toggleExpand(node: GrupoEntidadeNode, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const nextValue = !this.isExpanded(node);
    this.expandedMap[node.id] = nextValue;
    if (nextValue) {
      this.collapseSiblings(node.id);
    }
  }

  onDragOverGroup(node: GrupoEntidadeNode, event: DragEvent): void {
    if (!this.allowEntityDrop) return;
    event.preventDefault();
    this.dragOverGroupId = node.id;
    this.dragOverAll = false;
  }

  onGroupDragStart(event: DragEvent, node: GrupoEntidadeNode): void {
    const payload = JSON.stringify({ groupId: node.id });
    event.dataTransfer?.setData('application/x-entity-group', payload);
    event.dataTransfer?.setData('text/plain', `group:${node.id}`);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDropGroup(node: GrupoEntidadeNode, event: DragEvent): void {
    if (!this.allowEntityDrop) return;
    event.preventDefault();
    const entityId = this.readEntityIdFromDrag(event);
    this.dragOverGroupId = null;
    this.dragOverAll = false;
    if (!entityId) return;
    this.entityDroppedOnGroup.emit({ entityId, groupId: node.id });
  }

  onDragLeaveGroup(node: GrupoEntidadeNode): void {
    if (this.dragOverGroupId === node.id) {
      this.dragOverGroupId = null;
    }
  }

  isGroupDropTarget(node: GrupoEntidadeNode): boolean {
    return this.dragOverGroupId === node.id;
  }

  onDragOverAll(event: DragEvent): void {
    if (!this.allowEntityDrop) return;
    event.preventDefault();
    this.dragOverAll = true;
    this.dragOverGroupId = null;
  }

  onDropAll(event: DragEvent): void {
    if (!this.allowEntityDrop) return;
    event.preventDefault();
    const entityId = this.readEntityIdFromDrag(event);
    this.dragOverAll = false;
    this.dragOverGroupId = null;
    if (!entityId) return;
    this.entityDroppedOnGroup.emit({ entityId, groupId: null });
  }

  onDragLeaveAll(): void {
    this.dragOverAll = false;
  }

  isAllDropTarget(): boolean {
    return this.dragOverAll;
  }

  private seedExpanded(): void {
    const keep: Record<number, boolean> = {};
    for (const id of Object.keys(this.nodeById)) {
      const nodeId = Number(id);
      const previous = this.expandedMap[nodeId];
      keep[nodeId] = previous === true;
    }
    this.expandedMap = keep;

    const rootSiblings = this.siblingsByParent['root'] || [];
    if (rootSiblings.length > 0 && !rootSiblings.some(id => this.expandedMap[id])) {
      const firstRoot = rootSiblings[0];
      if (firstRoot !== undefined) {
        this.expandedMap[firstRoot] = true;
      }
    }

    for (const key of Object.keys(this.siblingsByParent)) {
      const siblings = this.siblingsByParent[key] || [];
      let firstExpanded: number | null = null;
      for (const siblingId of siblings) {
        if (!this.expandedMap[siblingId]) continue;
        if (firstExpanded === null) {
          firstExpanded = siblingId;
        } else {
          this.expandedMap[siblingId] = false;
        }
      }
    }
  }

  private collapseSiblings(nodeId: number): void {
    const parentId = this.parentById[nodeId];
    const key = parentId === null || parentId === undefined ? 'root' : String(parentId);
    const siblings = this.siblingsByParent[key] || [];
    for (const siblingId of siblings) {
      if (siblingId !== nodeId) {
        this.expandedMap[siblingId] = false;
      }
    }
  }

  private rebuildNodeIndex(nodes: GrupoEntidadeNode[]): void {
    this.nodeById = {};
    this.parentById = {};
    this.siblingsByParent = {};
    this.indexNodes(nodes || [], null);
  }

  private indexNodes(nodes: GrupoEntidadeNode[], parentId: number | null): void {
    const key = parentId === null ? 'root' : String(parentId);
    if (!this.siblingsByParent[key]) {
      this.siblingsByParent[key] = [];
    }
    for (const node of nodes || []) {
      this.nodeById[node.id] = node;
      this.parentById[node.id] = parentId;
      this.siblingsByParent[key].push(node.id);
      this.indexNodes(node.children || [], node.id);
    }
  }

  private readEntityIdFromDrag(event: DragEvent): number | null {
    const raw = event.dataTransfer?.getData('application/x-entity-row')
      || event.dataTransfer?.getData('text/plain')
      || '';
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw);
      const id = Number(parsed?.entityId || 0);
      return Number.isFinite(id) && id > 0 ? id : null;
    } catch {
      const id = Number(raw);
      return Number.isFinite(id) && id > 0 ? id : null;
    }
  }

  private createGroupWithPrompt(parentId: number | null): void {
    if (!this.tipoEntidadeId || !this.canManage) return;
    const parentName = parentId ? this.nodeById[parentId]?.nome || '' : '';
    const promptText = parentId
      ? `Nome do novo subgrupo de "${parentName}":`
      : 'Nome do novo grupo raiz:';
    const nome = (prompt(promptText) || '').trim();
    if (!nome) return;
    this.service.create(this.tipoEntidadeId, nome, parentId).subscribe({
      next: () => {
        this.notify.success(parentId ? 'Subgrupo criado.' : 'Grupo criado.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || (parentId
        ? 'Nao foi possivel criar subgrupo.'
        : 'Nao foi possivel criar grupo.'))
    });
  }
}
