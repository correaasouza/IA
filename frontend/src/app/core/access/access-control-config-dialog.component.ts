import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { RolesService } from '../../features/roles/roles.service';

export interface AccessControlConfigDialogData {
  title: string;
  controlKey: string;
  description?: string;
  selectedRoles: string[];
  fallbackRoles?: string[];
}

@Component({
  selector: 'app-access-control-config-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatSelectModule],
  templateUrl: './access-control-config-dialog.component.html'
})
export class AccessControlConfigDialogComponent implements OnInit {
  selectedRoles: string[] = [];
  roleOptions: string[] = ['MASTER', 'USER'];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: AccessControlConfigDialogData,
    private dialogRef: MatDialogRef<AccessControlConfigDialogComponent, string[]>,
    private rolesService: RolesService,
    private auth: AuthService
  ) {
    this.selectedRoles = this.normalize([...(data?.selectedRoles || [])]);
  }

  ngOnInit(): void {
    this.rolesService.list().subscribe({
      next: (items) => {
        const fromApi = (items || [])
          .filter(r => r?.ativo !== false)
          .map(r => (r?.nome || '').trim().toUpperCase())
          .filter(r => r.length > 0);
        this.roleOptions = this.mergeRoles(fromApi, this.data?.fallbackRoles || [], this.selectedRoles, ['MASTER', 'USER']);
      },
      error: () => {
        const fromToken = (this.auth.getUserRoles() || [])
          .map(r => (r || '').trim().toUpperCase())
          .filter(r => r.length > 0);
        this.roleOptions = this.mergeRoles(fromToken, this.data?.fallbackRoles || [], this.selectedRoles, ['MASTER', 'USER']);
      }
    });
  }

  isMasterRole(role: string): boolean {
    return (role || '').toUpperCase() === 'MASTER';
  }

  onRolesChange(): void {
    this.selectedRoles = this.normalize(this.selectedRoles);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.dialogRef.close(this.normalize(this.selectedRoles));
  }

  private mergeRoles(...groups: string[][]): string[] {
    return this.normalize(groups.flat());
  }

  private normalize(values: string[]): string[] {
    return Array.from(new Set((values || [])
      .map(v => this.normalizeRole(v || ''))
      .filter(v => v.length > 0)
      .concat(['MASTER'])))
      .sort((a, b) => a.localeCompare(b));
  }

  private normalizeRole(value: string): string {
    let role = (value || '').trim().toUpperCase();
    if (!role) {
      return '';
    }
    const sepIdx = role.lastIndexOf(':');
    if (sepIdx >= 0 && sepIdx < role.length - 1) {
      role = role.substring(sepIdx + 1);
    }
    if (role.startsWith('ROLE_')) {
      role = role.substring(5);
    }
    return role;
  }
}
