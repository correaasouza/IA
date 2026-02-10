import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { CdkDragDrop, moveItemInArray, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-atalho-ordenar-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, DragDropModule],
  templateUrl: './atalho-ordenar-dialog.component.html',
  styleUrls: ['./atalho-ordenar-dialog.component.css']
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

