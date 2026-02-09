import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatListModule, MatListOption } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';

import { RolesService, Papel } from '../roles/roles.service';
import { UsuarioService } from './usuario.service';

@Component({
  selector: 'app-usuario-papeis-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatListModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Papéis do usuário</h2>
    <mat-dialog-content>
      <div class="subtitle">{{ data.username }}</div>
      <mat-selection-list #papelList (selectionChange)="onChange(papelList.selectedOptions.selected)">
        <mat-list-option *ngFor="let p of papeis" [value]="p.id" [selected]="selected.has(p.id)">
          {{ p.nome }}
        </mat-list-option>
      </mat-selection-list>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" (click)="save()">Salvar</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .subtitle { color: var(--muted); font-size: 12px; margin-bottom: 6px; }
      mat-dialog-content { min-width: 320px; }
    `
  ]
})
export class UsuarioPapeisDialogComponent implements OnInit {
  papeis: Papel[] = [];
  selected = new Set<number>();

  constructor(
    private rolesService: RolesService,
    private usuarioService: UsuarioService,
    private dialogRef: MatDialogRef<UsuarioPapeisDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { userId: number; username: string }
  ) {}

  ngOnInit(): void {
    this.rolesService.list().subscribe({ next: data => this.papeis = data || [] });
    this.usuarioService.getPapeis(this.data.userId).subscribe({
      next: data => {
        const ids = data?.papelIds || [];
        this.selected = new Set<number>(ids);
      }
    });
  }

  onChange(options: MatListOption[]) {
    this.selected = new Set(options.map(o => o.value as number));
  }

  save() {
    const ids = Array.from(this.selected);
    this.usuarioService.setPapeis(this.data.userId, ids).subscribe({
      next: () => this.dialogRef.close(true)
    });
  }

  close() {
    this.dialogRef.close(false);
  }
}
