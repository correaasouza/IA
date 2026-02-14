import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { RolesService, Papel } from './roles.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-role-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    InlineLoaderComponent
  ],
  templateUrl: './role-form.component.html'
})
export class RoleFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo papel';
  papel: Papel | null = null;
  loading = false;
  saving = false;
  deleting = false;
  toggling = false;

  form = this.fb.group({
    nome: ['', Validators.required],
    descricao: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: RolesService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (idParam) {
      const id = Number(idParam);
      this.mode = isEdit ? 'edit' : 'view';
      this.title = this.mode === 'edit' ? 'Editar papel' : 'Consultar papel';
      this.load(id);
      return;
    }
    this.mode = 'new';
    this.title = 'Novo papel';
    this.form.enable();
  }

  private load(id: number) {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => (this.loading = false))).subscribe({
      next: data => {
        this.papel = data;
        this.form.patchValue({
          nome: data.nome,
          descricao: data.descricao || ''
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
        }
      },
      error: () => this.notify.error('Não foi possível carregar o papel.')
    });
  }

  toEdit() {
    if (!this.papel) return;
    this.router.navigate(['/roles', this.papel.id, 'edit']);
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.mode === 'new') {
      this.saving = true;
      this.service.create({
        nome: this.form.value.nome!,
        descricao: this.form.value.descricao || undefined,
        ativo: true
      }).pipe(finalize(() => (this.saving = false))).subscribe({
        next: created => {
          this.notify.success('Papel criado.');
          this.router.navigate(['/roles', created.id]);
        },
        error: () => this.notify.error('Não foi possível criar o papel.')
      });
      return;
    }
    if (!this.papel) return;
    this.saving = true;
    this.service.update(this.papel.id, {
      nome: this.form.value.nome!,
      descricao: this.form.value.descricao || undefined,
      ativo: this.papel.ativo
    }).pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => {
        this.notify.success('Papel atualizado.');
        this.router.navigate(['/roles', this.papel!.id]);
      },
      error: () => this.notify.error('Não foi possível atualizar o papel.')
    });
  }

  remove() {
    if (!this.papel || this.mode === 'new') return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir papel', message: `Deseja excluir o papel "${this.papel.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deleting = true;
      this.service.delete(this.papel!.id).pipe(finalize(() => (this.deleting = false))).subscribe({
        next: () => {
          this.notify.success('Papel excluído.');
          this.router.navigateByUrl('/roles');
        },
        error: () => this.notify.error('Não foi possível excluir o papel.')
      });
    });
  }

  toggleStatus() {
    if (!this.papel || this.mode === 'new') return;
    const nextStatus = !this.papel.ativo;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: nextStatus ? 'Ativar papel' : 'Desativar papel',
        message: nextStatus
          ? `Deseja ativar o papel "${this.papel.nome}"?`
          : `Deseja desativar o papel "${this.papel.nome}"?`,
        confirmText: nextStatus ? 'Ativar' : 'Desativar',
        confirmColor: nextStatus ? 'primary' : 'warn',
        confirmAriaLabel: `${nextStatus ? 'Ativar' : 'Desativar'} papel`
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result || !this.papel) return;
      this.toggling = true;
      this.service.update(this.papel.id, {
        nome: this.form.value.nome!,
        descricao: this.form.value.descricao || undefined,
        ativo: nextStatus
      }).pipe(finalize(() => (this.toggling = false))).subscribe({
        next: updated => {
          this.papel = updated;
          this.notify.success(nextStatus ? 'Papel ativado.' : 'Papel desativado.');
        },
        error: () => this.notify.error('Não foi possível atualizar o status do papel.')
      });
    });
  }

  back() {
    this.router.navigateByUrl('/roles');
  }
}

