import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MovimentoEstoqueItemResponse } from '../movement-operation.service';

interface ItemWorkflowTransition {
  key: string;
  name: string;
  toStateKey: string;
  toStateName?: string | null;
}

export interface MovimentoItemInlineSaveEvent {
  item: MovimentoEstoqueItemResponse;
  quantidade: number;
  valorUnitario: number;
}

@Component({
  selector: 'app-movimento-itens-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatTableModule,
    MatTooltipModule
  ],
  templateUrl: './movimento-itens-list.component.html',
  styleUrls: ['./movimento-itens-list.component.css']
})
export class MovimentoItensListComponent implements OnInit {
  @Input() items: MovimentoEstoqueItemResponse[] = [];
  @Input() inlineEditEnabled = false;
  @Input() actionButtonsEnabled = true;
  @Input() showWorkflowTransitions = false;
  @Input() transitionsByItem: Record<number, ItemWorkflowTransition[]> = {};
  @Input() stateNamesByItemId: Record<number, string> = {};
  @Input() stateKeysByItemId: Record<number, string> = {};
  @Input() stateColorsByStateKey: Record<string, string> = {};
  @Input() itemSaving = false;
  @Input() emptyMessage = 'Movimento sem itens.';
  @Input() updateControlKey = 'movimentos.estoque.update';
  @Input() deleteControlKey = 'movimentos.estoque.update';
  @Input() onConsult: ((item: MovimentoEstoqueItemResponse) => void) | null = null;
  @Input() onEdit: ((item: MovimentoEstoqueItemResponse) => void) | null = null;
  @Input() onSaveInline: ((event: MovimentoItemInlineSaveEvent) => void) | null = null;
  @Input() onDelete: ((item: MovimentoEstoqueItemResponse) => void) | null = null;

  @Output() consultItem = new EventEmitter<MovimentoEstoqueItemResponse>();
  @Output() editItem = new EventEmitter<MovimentoEstoqueItemResponse>();
  @Output() saveInlineItem = new EventEmitter<MovimentoItemInlineSaveEvent>();
  @Output() deleteItem = new EventEmitter<MovimentoEstoqueItemResponse>();
  @Output() transitionItem = new EventEmitter<{
    item: MovimentoEstoqueItemResponse;
    transitionKey: string;
    expectedCurrentStateKey?: string | null;
  }>();

  displayedColumns = ['tipo', 'catalogo', 'status', 'quantidade', 'valorUnitario', 'valorTotal', 'cobrar', 'acoes'];
  private readonly desktopColumns = ['tipo', 'catalogo', 'status', 'quantidade', 'valorUnitario', 'valorTotal', 'cobrar', 'acoes'];
  isMobile = false;

  editingItemId: number | null = null;
  editingQuantidade = 0;
  editingValorUnitario = 0;

  ngOnInit(): void {
    this.updateViewportColumns();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportColumns();
  }

  isEditingItem(item: MovimentoEstoqueItemResponse): boolean {
    return this.editingItemId === item.id;
  }

  startEdit(item: MovimentoEstoqueItemResponse): void {
    this.editingItemId = item.id;
    this.editingQuantidade = Number(item.quantidade || 0);
    this.editingValorUnitario = Number(item.valorUnitario || 0);
  }

  cancelEdit(): void {
    this.editingItemId = null;
    this.editingQuantidade = 0;
    this.editingValorUnitario = 0;
  }

  onConsultClick(event: Event, item: MovimentoEstoqueItemResponse): void {
    event.preventDefault();
    event.stopPropagation();
    this.dispatch(this.onConsult, this.consultItem, item);
  }

  onEditClick(event: Event, item: MovimentoEstoqueItemResponse): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.inlineEditEnabled) {
      this.startEdit(item);
      return;
    }
    this.dispatch(this.onEdit, this.editItem, item);
  }

  onSaveClick(event: Event, item: MovimentoEstoqueItemResponse): void {
    event.preventDefault();
    event.stopPropagation();
    const payload: MovimentoItemInlineSaveEvent = {
      item,
      quantidade: Number(this.editingQuantidade || 0),
      valorUnitario: Number(this.editingValorUnitario || 0)
    };
    this.dispatch(this.onSaveInline, this.saveInlineItem, payload);
  }

  onCancelClick(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.cancelEdit();
  }

  onDeleteClick(event: Event, item: MovimentoEstoqueItemResponse): void {
    event.preventDefault();
    event.stopPropagation();
    this.dispatch(this.onDelete, this.deleteItem, item);
  }

  itemTransitions(item: MovimentoEstoqueItemResponse): ItemWorkflowTransition[] {
    const itemId = Number(item?.id || 0);
    if (!itemId || !this.transitionsByItem) {
      return [];
    }
    return this.transitionsByItem[itemId] || [];
  }

  displayStatus(item: MovimentoEstoqueItemResponse): string {
    const itemId = Number(item?.id || 0);
    const mapped = itemId > 0 ? (this.stateNamesByItemId[itemId] || '').trim() : '';
    if (mapped) {
      return mapped;
    }
    const raw = (item?.status || '').trim();
    if (!raw) {
      return '-';
    }
    return this.looksLikeUuid(raw) ? '-' : raw;
  }

  statusColor(item: MovimentoEstoqueItemResponse): string | null {
    const stateKey = this.resolveStateKey(item);
    if (!stateKey) {
      return null;
    }
    const color = (this.stateColorsByStateKey[stateKey] || '').trim();
    return /^#[\da-fA-F]{6}$/.test(color) ? color : null;
  }

  onTransitionSelect(event: Event, item: MovimentoEstoqueItemResponse, transitionKey: string): void {
    event.preventDefault();
    event.stopPropagation();
    if (!transitionKey) {
      return;
    }
    this.transitionItem.emit({
      item,
      transitionKey,
      expectedCurrentStateKey: this.resolveExpectedCurrentStateKey(item)
    });
  }

  private dispatch<T>(
    callback: ((payload: T) => void) | null | undefined,
    emitter: EventEmitter<T>,
    payload: T
  ): void {
    if (callback) {
      callback(payload);
      return;
    }
    emitter.emit(payload);
  }

  private looksLikeUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
  }

  private resolveExpectedCurrentStateKey(item: MovimentoEstoqueItemResponse): string | null {
    const itemId = Number(item?.id || 0);
    if (itemId > 0) {
      const runtimeStateKey = (this.stateKeysByItemId[itemId] || '').trim();
      if (runtimeStateKey) {
        return runtimeStateKey;
      }
    }
    const raw = (item?.status || '').trim();
    if (!raw) {
      return null;
    }
    return this.looksLikeUuid(raw) ? raw : null;
  }

  private resolveStateKey(item: MovimentoEstoqueItemResponse): string | null {
    const itemId = Number(item?.id || 0);
    if (itemId > 0) {
      const runtimeStateKey = (this.stateKeysByItemId[itemId] || '').trim().toUpperCase();
      if (runtimeStateKey) {
        return runtimeStateKey;
      }
    }
    const raw = (item?.status || '').trim().toUpperCase();
    if (!raw || this.looksLikeUuid(raw)) {
      return null;
    }
    return raw;
  }

  private updateViewportColumns(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    this.displayedColumns = this.desktopColumns;
  }
}
