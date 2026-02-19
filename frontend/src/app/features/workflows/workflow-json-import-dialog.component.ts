import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface WorkflowJsonImportDialogData {
  json?: string;
}

@Component({
  selector: 'app-workflow-json-import-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './workflow-json-import-dialog.component.html',
  styleUrls: ['./workflow-json-import-dialog.component.css']
})
export class WorkflowJsonImportDialogComponent {
  jsonText = '';

  constructor(
    private dialogRef: MatDialogRef<WorkflowJsonImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: WorkflowJsonImportDialogData
  ) {
    this.jsonText = (data?.json || '').trim();
  }

  close(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    this.dialogRef.close(this.jsonText);
  }
}
