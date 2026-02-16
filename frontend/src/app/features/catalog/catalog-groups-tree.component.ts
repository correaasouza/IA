import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { CatalogCrudType } from './catalog-item.service';
import { CatalogGroupNode, CatalogGroupService } from './catalog-group.service';

@Component({
  selector: 'app-catalog-groups-tree',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './catalog-groups-tree.component.html',
  styleUrls: ['./catalog-groups-tree.component.css']
})
export class CatalogGroupsTreeComponent implements OnChanges {
  @Input({ required: true }) type: CatalogCrudType = 'PRODUCTS';
  @Input() canManage = true;
  @Input() allowGroupDrag = false;
  @Input() selectedGroupId: number | null = null;
  @Output() selectedGroupIdChange = new EventEmitter<number | null>();
  @Output() changed = new EventEmitter<void>();

  loading = false;
  nodes: CatalogGroupNode[] = [];
  private expandedMap: Record<number, boolean> = {};
  private nodeById: Record<number, CatalogGroupNode> = {};

  constructor(
    private service: CatalogGroupService,
    private notify: NotificationService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['type']) {
      this.load();
    }
  }

  load(): void {
    this.loading = true;
    this.service.tree(this.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: data => {
          this.nodes = data || [];
          this.rebuildNodeIndex(this.nodes);
          this.seedExpanded(this.nodes);
        },
        error: err => {
          this.nodes = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar grupos do catalogo.');
        }
      });
  }

  selectGroup(groupId: number | null): void {
    this.selectedGroupId = groupId;
    this.selectedGroupIdChange.emit(groupId);
  }

  addRoot(): void {
    if (!this.canManage) return;
    this.createWithPrompt(null);
  }

  addChild(node: CatalogGroupNode): void {
    if (!this.canManage) return;
    this.createWithPrompt(node.id);
  }

  rename(node: CatalogGroupNode): void {
    if (!this.canManage) return;
    const nome = (prompt('Novo nome do grupo:', node.nome) || '').trim();
    if (!nome || nome === node.nome) return;

    this.service.update(this.type, node.id, {
      nome,
      parentId: node.parentId ?? null,
      ordem: node.ordem
    }).subscribe({
      next: () => {
        this.notify.success('Grupo atualizado.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar grupo.')
    });
  }

  remove(node: CatalogGroupNode): void {
    if (!this.canManage) return;
    if (!confirm(`Excluir grupo "${node.nome}"?`)) return;

    this.service.delete(this.type, node.id).subscribe({
      next: () => {
        this.notify.success('Grupo removido.');
        if (this.selectedGroupId === node.id) {
          this.selectGroup(null);
        }
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover grupo.')
    });
  }

  isSelected(node: CatalogGroupNode): boolean {
    return !!this.selectedGroupId && this.selectedGroupId === node.id;
  }

  hasChildren(node: CatalogGroupNode): boolean {
    return !!node.children?.length;
  }

  isExpanded(node: CatalogGroupNode): boolean {
    return this.expandedMap[node.id] !== false;
  }

  toggleExpand(node: CatalogGroupNode, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.expandedMap[node.id] = !this.isExpanded(node);
  }

  onGroupDragStart(event: DragEvent, node: CatalogGroupNode): void {
    if (!this.allowGroupDrag) return;
    const payload = JSON.stringify({ groupId: node.id });
    event.dataTransfer?.setData('application/x-catalog-group', payload);
    event.dataTransfer?.setData('text/plain', `group:${node.id}`);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  private createWithPrompt(parentId: number | null): void {
    const parentName = parentId ? this.nodeById[parentId]?.nome || '' : '';
    const question = parentId
      ? `Nome do novo subgrupo de "${parentName}":`
      : 'Nome do novo grupo raiz:';
    const nome = (prompt(question) || '').trim();
    if (!nome) return;

    this.service.create(this.type, { nome, parentId }).subscribe({
      next: () => {
        this.notify.success(parentId ? 'Subgrupo criado.' : 'Grupo criado.');
        this.load();
        this.changed.emit();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar grupo.')
    });
  }

  private rebuildNodeIndex(nodes: CatalogGroupNode[]): void {
    this.nodeById = {};
    this.indexNodes(nodes || []);
  }

  private indexNodes(nodes: CatalogGroupNode[]): void {
    for (const node of nodes || []) {
      this.nodeById[node.id] = node;
      this.indexNodes(node.children || []);
    }
  }

  private seedExpanded(nodes: CatalogGroupNode[]): void {
    const next: Record<number, boolean> = {};
    for (const node of nodes || []) {
      next[node.id] = this.expandedMap[node.id] !== false;
    }
    this.expandedMap = { ...this.expandedMap, ...next };
  }
}
