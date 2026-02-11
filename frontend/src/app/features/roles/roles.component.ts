import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatListModule, MatListOption } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { finalize } from 'rxjs/operators';

import { RolesService, Papel, PermissaoCatalog } from './roles.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatListModule,
    MatChipsModule,
    MatIconModule,
    MatDialogModule,
    MatSidenavModule,
    MatTooltipModule,
    MatMenuModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './roles.component.html',
  styleUrls: ['./roles.component.css']
})
export class RolesComponent implements OnInit {
  papeis: Papel[] = [];
  filteredPapeis: Papel[] = [];
  catalog: PermissaoCatalog[] = [];
  filteredCatalog: PermissaoCatalog[] = [];
  selected: Papel | null = null;
  selectedPerms = new Set<string>();
  displayedColumns = ['nome', 'descricao', 'ativo', 'acoes'];
  permColumns = ['codigo', 'label', 'acoes'];

  rolesLoading = false;
  catalogLoading = false;
  permsLoading = false;
  creatingRole = false;
  creatingPerm = false;
  savingPerms = false;
  togglingRoleId: number | null = null;

  roleSearchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' },
    { key: 'descricao', label: 'Descrição' }
  ];
  roleSearchTerm = '';
  roleSearchFields = ['nome', 'descricao'];

  permSearchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'label', label: 'Label' }
  ];
  permSearchTerm = '';
  permSearchFields = ['codigo', 'label'];

  editingPermId: number | null = null;
  editingPermLabelControl = new FormControl('', { nonNullable: true });
  savingPermId: number | null = null;

  editingRole: Papel | null = null;
  savingRole = false;

  form = this.fb.group({
    nome: ['', Validators.required],
    descricao: ['']
  });

  permForm = this.fb.group({
    codigo: ['', Validators.required],
    label: ['', Validators.required]
  });

  roleEditForm = this.fb.group({
    nome: ['', Validators.required],
    descricao: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: RolesService,
    private notify: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadCatalog();
  }

  load() {
    this.rolesLoading = true;
    this.service.list().pipe(finalize(() => this.rolesLoading = false)).subscribe({
      next: data => {
        this.papeis = data || [];
        this.applyRoleSearch();
      },
      error: () => {
        this.papeis = [];
        this.filteredPapeis = [];
        this.notify.error('Não foi possível carregar os papéis.');
      }
    });
  }

  loadCatalog() {
    this.catalogLoading = true;
    this.service.listCatalog().pipe(finalize(() => this.catalogLoading = false)).subscribe({
      next: data => {
        this.catalog = data || [];
        this.applyPermSearch();
      },
      error: () => {
        this.catalog = [];
        this.filteredCatalog = [];
        this.notify.error('Não foi possível carregar o catálogo de permissões.');
      }
    });
  }

  create() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.creatingRole = true;
    this.service.create({
      nome: this.form.value.nome!,
      descricao: this.form.value.descricao || undefined,
      ativo: true
    }).pipe(finalize(() => this.creatingRole = false)).subscribe({
      next: () => {
        this.form.reset();
        this.load();
        this.notify.success('Papel criado.');
      },
      error: () => this.notify.error('Não foi possível criar o papel.')
    });
  }

  openEditRole(row: Papel) {
    this.editingRole = row;
    this.roleEditForm.reset({
      nome: row.nome,
      descricao: row.descricao || ''
    });
  }

  closeEditRole() {
    this.editingRole = null;
    this.roleEditForm.reset();
  }

  saveRoleEdit() {
    if (!this.editingRole) return;
    if (this.roleEditForm.invalid) {
      this.roleEditForm.markAllAsTouched();
      return;
    }
    const payload = {
      nome: this.roleEditForm.value.nome!,
      descricao: this.roleEditForm.value.descricao || '',
      ativo: this.editingRole.ativo
    };
    this.savingRole = true;
    this.service.update(this.editingRole.id, payload).pipe(finalize(() => this.savingRole = false)).subscribe({
      next: () => {
        this.editingRole!.nome = payload.nome;
        this.editingRole!.descricao = payload.descricao;
        this.notify.success('Papel atualizado.');
        this.closeEditRole();
      },
      error: () => this.notify.error('Não foi possível atualizar o papel.')
    });
  }

  toggleStatus(row: Papel) {
    const nextStatus = !row.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: nextStatus ? 'Ativar papel' : 'Desativar papel',
        message: nextStatus
          ? `Deseja ativar o papel "${row.nome}"?`
          : `Deseja desativar o papel "${row.nome}"?`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.togglingRoleId = row.id;
      this.service.update(row.id, { nome: row.nome, descricao: row.descricao, ativo: nextStatus })
        .pipe(finalize(() => this.togglingRoleId = null))
        .subscribe({
          next: () => {
            row.ativo = nextStatus;
            this.notify.success(nextStatus ? 'Papel ativado.' : 'Papel desativado.');
          },
          error: () => this.notify.error('Não foi possível atualizar o status.')
        });
    });
  }

  select(row: Papel) {
    this.selected = row;
    this.selectedPerms = new Set<string>();
    this.permsLoading = true;
    this.service.listPermissoes(row.id).pipe(finalize(() => this.permsLoading = false)).subscribe({
      next: data => this.selectedPerms = new Set(data || []),
      error: () => this.notify.error('Não foi possível carregar as permissões do papel.')
    });
  }

  onPermChange(options: MatListOption[]) {
    this.selectedPerms = new Set(options.map(o => o.value as string));
  }

  savePerms() {
    if (!this.selected) return;
    this.savingPerms = true;
    this.service.setPermissoes(this.selected.id, Array.from(this.selectedPerms))
      .pipe(finalize(() => this.savingPerms = false))
      .subscribe({
        next: () => this.notify.success('Permissões atualizadas.'),
        error: () => this.notify.error('Não foi possível salvar as permissões.')
      });
  }

  createPerm() {
    if (this.permForm.invalid) {
      this.permForm.markAllAsTouched();
      return;
    }
    this.creatingPerm = true;
    this.service.createPerm({
      codigo: this.permForm.value.codigo!,
      label: this.permForm.value.label!,
      ativo: true
    }).pipe(finalize(() => this.creatingPerm = false)).subscribe({
      next: () => {
        this.permForm.reset();
        this.loadCatalog();
        this.notify.success('Permissão criada.');
      },
      error: () => this.notify.error('Não foi possível criar a permissão.')
    });
  }

  editPerm(row: PermissaoCatalog) {
    this.editingPermId = row.id;
    this.editingPermLabelControl.setValue(row.label);
  }

  cancelPermEdit() {
    this.editingPermId = null;
    this.editingPermLabelControl.setValue('');
  }

  savePerm(row: PermissaoCatalog) {
    const nextLabel = (this.editingPermLabelControl.value || '').trim();
    if (!nextLabel) {
      this.notify.error('Informe um label válido.');
      return;
    }
    this.savingPermId = row.id;
    this.service.updatePerm(row.id, row.codigo, nextLabel).pipe(finalize(() => this.savingPermId = null)).subscribe({
      next: () => {
        row.label = nextLabel;
        this.cancelPermEdit();
        this.notify.success('Permissão atualizada.');
      },
      error: () => this.notify.error('Não foi possível atualizar a permissão.')
    });
  }

  statusLabel(row: Papel) {
    return row.ativo ? 'Ativo' : 'Inativo';
  }

  onRoleSearchChange(value: FieldSearchValue) {
    this.roleSearchTerm = value.term;
    this.roleSearchFields = value.fields.length ? value.fields : this.roleSearchOptions.map(o => o.key);
    this.applyRoleSearch();
  }

  onPermSearchChange(value: FieldSearchValue) {
    this.permSearchTerm = value.term;
    this.permSearchFields = value.fields.length ? value.fields : this.permSearchOptions.map(o => o.key);
    this.applyPermSearch();
  }

  private applyRoleSearch() {
    const term = this.roleSearchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredPapeis = [...this.papeis];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredPapeis = this.papeis.filter(p => {
      const matchNome = this.roleSearchFields.includes('nome') && match(p.nome);
      const matchDescricao = this.roleSearchFields.includes('descricao') && match(p.descricao || '');
      return matchNome || matchDescricao;
    });
  }

  private applyPermSearch() {
    const term = this.permSearchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredCatalog = [...this.catalog];
      return;
    }
    const match = (val?: string) => (val || '').toLowerCase().includes(term);
    this.filteredCatalog = this.catalog.filter(p => {
      const matchCodigo = this.permSearchFields.includes('codigo') && match(p.codigo);
      const matchLabel = this.permSearchFields.includes('label') && match(p.label);
      return matchCodigo || matchLabel;
    });
  }
}


