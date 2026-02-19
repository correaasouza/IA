import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MovimentoEstoqueItemResponse } from '../movement-operation.service';

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
    MatTableModule,
    MatTooltipModule
  ],
  templateUrl: './movimento-itens-list.component.html',
  styleUrls: ['./movimento-itens-list.component.css']
})
export class MovimentoItensListComponent {
  @Input() items: MovimentoEstoqueItemResponse[] = [];
  @Input() inlineEditEnabled = false;
  @Input() actionButtonsEnabled = true;
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

  displayedColumns = ['tipo', 'catalogo', 'quantidade', 'valorUnitario', 'valorTotal', 'cobrar', 'acoes'];

  editingItemId: number | null = null;
  editingQuantidade = 0;
  editingValorUnitario = 0;

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
}
