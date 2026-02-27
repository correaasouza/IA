import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatListModule, MatListOption } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { RolesService, Papel } from '../roles/roles.service';
import { UsuarioService } from './usuario.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';

@Component({
  selector: 'app-usuario-papeis-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatListModule, MatButtonModule, InlineLoaderComponent],
  templateUrl: './usuario-papeis-dialog.component.html'
})
export class UsuarioPapeisDialogComponent implements OnInit {
  papeis: Papel[] = [];
  selected = new Set<number>();
  loading = false;
  saving = false;

  constructor(
    private rolesService: RolesService,
    private usuarioService: UsuarioService,
    private dialogRef: MatDialogRef<UsuarioPapeisDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { userId: number; username: string }
  ) {}

  ngOnInit(): void {
    this.loading = true;
    forkJoin({
      papeis: this.rolesService.list(),
      user: this.usuarioService.getPapeis(this.data.userId)
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: ({ papeis, user }) => {
        this.papeis = papeis || [];
        const ids = user?.papelIds || [];
        this.selected = new Set<number>(ids);
        this.enforceMasterRoleConstraint();
      }
    });
  }

  onChange(options: MatListOption[]) {
    this.selected = new Set(options.map(o => o.value as number));
    this.enforceMasterRoleConstraint();
  }

  save() {
    const ids = Array.from(this.selected);
    this.saving = true;
    this.usuarioService.setPapeis(this.data.userId, ids).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => this.dialogRef.close(true)
    });
  }

  close() {
    this.dialogRef.close(false);
  }

  isMasterRoleDisabled(papel: Papel): boolean {
    return this.isMasterRole(papel.nome) && !this.isMasterUsername();
  }

  private enforceMasterRoleConstraint(): void {
    if (this.isMasterUsername()) {
      return;
    }
    const forbiddenIds = new Set(
      this.papeis
        .filter(papel => this.isMasterRole(papel?.nome || ''))
        .map(papel => papel.id)
    );
    if (!forbiddenIds.size) {
      return;
    }
    this.selected = new Set(Array.from(this.selected).filter(id => !forbiddenIds.has(id)));
  }

  private isMasterRole(roleName: string): boolean {
    return (roleName || '').trim().toUpperCase() === 'MASTER';
  }

  private isMasterUsername(): boolean {
    return (this.data?.username || '').trim().toLowerCase() === 'master';
  }
}

