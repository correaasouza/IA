import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="dialog" role="dialog" aria-labelledby="confirm-title" aria-describedby="confirm-message">
      <div class="icon">
        <mat-icon>warning</mat-icon>
      </div>
      <div class="content">
        <div class="title" id="confirm-title">{{ data?.title || 'Confirmar' }}</div>
        <div class="message" id="confirm-message">{{ data?.message || 'Tem certeza?' }}</div>
      </div>
    </div>
    <div class="actions">
      <button mat-stroked-button (click)="close(false)" aria-label="Cancelar ação">Cancelar</button>
      <button mat-flat-button color="warn" (click)="close(true)" aria-label="Confirmar exclusão">Excluir</button>
    </div>
  `,
  styles: [
    `
      .dialog { display: grid; grid-template-columns: 36px 1fr; gap: 10px; padding: 14px 16px 6px; align-items: center; }
      .icon { width: 32px; height: 32px; border-radius: 8px; display: grid; place-items: center; background: #fef3c7; color: #b45309; }
      .title { font-size: 14px; font-weight: 600; }
      .message { font-size: 12px; color: var(--muted); }
      .actions { display: flex; justify-content: flex-end; gap: 8px; padding: 8px 16px 14px; }
    `
  ]
})
export class ConfirmDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { title?: string; message?: string },
    private dialogRef: MatDialogRef<ConfirmDialogComponent>
  ) {}

  close(result: boolean) {
    this.dialogRef.close(result);
  }
}
