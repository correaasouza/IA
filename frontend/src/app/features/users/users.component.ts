import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { UsuarioService, UsuarioResponse } from './usuario.service';
import { UsuarioPapeisDialogComponent } from './usuario-papeis-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatDialogModule,
    MatIconModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  usuarios: UsuarioResponse[] = [];
  filteredUsuarios: UsuarioResponse[] = [];
  displayedColumns = ['username', 'email', 'ativo', 'papeis', 'acoes'];
  loading = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'username', label: 'Username' },
    { key: 'email', label: 'Email' },
    { key: 'papeis', label: 'Papéis' }
  ];
  searchTerm = '';
  searchFields = ['username', 'email'];

  form = this.fb.group({
    username: ['', Validators.required],
    email: [''],
    password: ['', Validators.required],
    roles: ['USER'],
    ativo: [true]
  });

  constructor(private fb: FormBuilder, private service: UsuarioService, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.service.list(0, 50).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.usuarios = data.content || [];
        this.applySearch();
      },
      error: () => {
        this.usuarios = [];
        this.filteredUsuarios = [];
      }
    });
  }

  create() {
    if (this.form.invalid) {
      return;
    }
    const roles = (this.form.value.roles || '')
      .split(',')
      .map(r => r.trim())
      .filter(r => r.length > 0);
    this.service.create({
      username: this.form.value.username!,
      email: this.form.value.email || undefined,
      password: this.form.value.password!,
      ativo: true,
      roles
    }).subscribe({
      next: () => {
        this.form.reset({ roles: 'USER', ativo: true });
        this.load();
      }
    });
  }

  disable(row: UsuarioResponse) {
    this.service.disable(row.id).subscribe({
      next: () => this.load()
    });
  }

  editPapeis(row: UsuarioResponse) {
    const ref = this.dialog.open(UsuarioPapeisDialogComponent, {
      data: { userId: row.id, username: row.username }
    });
    ref.afterClosed().subscribe();
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredUsuarios = [...this.usuarios];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredUsuarios = this.usuarios.filter(u => {
      const matchUsername = this.searchFields.includes('username') && match(u.username);
      const matchEmail = this.searchFields.includes('email') && match(u.email || '');
      const matchPapeis = this.searchFields.includes('papeis') && match((u.papeis || []).join(' '));
      return matchUsername || matchEmail || matchPapeis;
    });
  }
}


