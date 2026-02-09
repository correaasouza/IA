import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { CdkDragDrop, moveItemInArray, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-atalho-ordenar-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, DragDropModule],
  template: `
    <div class="dialog-header">
      <div>
        <div class="title">Ordenar atalhos</div>
        <div class="subtitle">Arraste para reorganizar a ordem exibida no topo.</div>
      </div>
    </div>
    <div class="dialog-body" cdkDropList (cdkDropListDropped)="drop($event)">
      <div class="item" *ngFor="let i of items" cdkDrag>
        <span class="handle">⋮⋮</span>
        <span class="label">{{ i.label }}</span>
      </div>
    </div>
    <div class="actions">
      <button mat-stroked-button (click)="cancel()">Cancelar</button>
      <button mat-flat-button color="primary" (click)="save()">Salvar</button>
    </div>
  `,
  styles: [
    `
      .dialog-header { padding: 12px 16px 6px; }
      .title { font-size: 14px; font-weight: 600; }
      .subtitle { font-size: 12px; color: var(--muted); }
      .dialog-body {
        display: grid;
        gap: 6px;
        padding: 8px 16px 12px;
        max-height: 50vh;
        overflow: auto;
        border-top: 1px solid var(--border);
        border-bottom: 1px solid var(--border);
        background: #f7f9fc;
      }
      .item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 10px;
        background: var(--surface);
        border: 1px solid var(--border);
        border-radius: 8px;
        cursor: move;
        box-shadow: 0 1px 0 rgba(0, 0, 0, 0.03);
      }
      .handle { font-size: 14px; color: var(--muted); }
      .label { font-size: 12.5px; }
      .actions { display: flex; gap: 8px; justify-content: flex-end; padding: 10px 16px 14px; }
    `
  ]
})
export class AtalhoOrdenarDialogComponent {
  items: any[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dialogRef: MatDialogRef<AtalhoOrdenarDialogComponent>
  ) {
    this.items = [...(data?.items || [])];
  }

  drop(event: CdkDragDrop<string[]>) {
    moveItemInArray(this.items, event.previousIndex, event.currentIndex);
  }

  cancel() {
    this.dialogRef.close(null);
  }

  save() {
    this.dialogRef.close(this.items);
  }
}
