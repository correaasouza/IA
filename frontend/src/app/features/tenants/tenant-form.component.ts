import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { DateMaskDirective } from '../../shared/date-mask.directive';
import { TenantService, LocatarioResponse } from './tenant.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    DateMaskDirective,
    MatIconModule,
    MatDialogModule,
    InlineLoaderComponent
  ],
  templateUrl: './tenant-form.component.html',
})
export class TenantFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  locatario: LocatarioResponse | null = null;
  title = 'Novo locatário';
  loading = false;
  saving = false;
  deleting = false;
  renewing = false;
  toggling = false;

  form = this.fb.group({
    nome: ['', Validators.required],
    dataLimiteAcesso: ['', Validators.required],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: TenantService,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (id) {
      this.mode = isEdit ? 'edit' : 'view';
      this.load(Number(id));
    } else {
      this.mode = 'new';
      this.updateTitle();
    }
  }

  private updateTitle() {
    this.title = this.mode === 'new' ? 'Novo locatário' : this.mode === 'edit' ? 'Editar locatário' : 'Consultar locatário';
  }

  private load(id: number) {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({
          nome: data.nome,
          dataLimiteAcesso: data.dataLimiteAcesso,
          ativo: data.ativo
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
        }
        this.updateTitle();
      },
      error: () => this.notify.error('Não foi possível carregar o locatário.')
    });
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      nome: this.form.value.nome!,
      dataLimiteAcesso: this.form.value.dataLimiteAcesso!,
      ativo: !!this.form.value.ativo
    };
    this.saving = true;
    if (this.mode === 'new') {
      this.service.create(payload).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.notify.success('Locatário criado.');
          this.router.navigateByUrl('/tenants');
        },
        error: () => this.notify.error('Não foi possível criar o locatário.')
      });
      return;
    }
    if (!this.locatario) {
      this.saving = false;
      return;
    }
    this.service.update(this.locatario.id, payload).pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.notify.success('Locatário atualizado.');
        this.router.navigate(['/tenants', this.locatario!.id]);
      },
      error: () => this.notify.error('Não foi possível atualizar o locatário.')
    });
  }

  renew() {
    if (!this.locatario) return;
    const date = new Date(this.locatario.dataLimiteAcesso);
    date.setDate(date.getDate() + 30);
    const iso = date.toISOString().slice(0, 10);
    this.renewing = true;
    this.service.updateAccessLimit(this.locatario.id, iso).pipe(finalize(() => this.renewing = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ dataLimiteAcesso: data.dataLimiteAcesso });
        this.notify.success('Acesso renovado por 30 dias.');
      },
      error: () => this.notify.error('Não foi possível renovar o acesso.')
    });
  }

  toggleStatus() {
    if (!this.locatario) return;
    const ativo = !this.locatario.ativo;
    this.toggling = true;
    this.service.updateStatus(this.locatario.id, ativo).pipe(finalize(() => this.toggling = false)).subscribe({
      next: data => {
        this.locatario = data;
        this.form.patchValue({ ativo: data.ativo });
        this.notify.success(ativo ? 'Locatário ativado.' : 'Locatário desativado.');
      },
      error: () => this.notify.error('Não foi possível atualizar o status.')
    });
  }

  remove() {
    if (!this.locatario) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir locatário', message: `Deseja excluir o locatário "${this.locatario.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.deleting = true;
      this.service.delete(this.locatario!.id).pipe(finalize(() => this.deleting = false)).subscribe({
        next: () => {
          this.notify.success('Locatário removido.');
          this.router.navigateByUrl('/tenants');
        },
        error: () => this.notify.error('Não foi possível remover o locatário.')
      });
    });
  }

  toEdit() {
    if (!this.locatario) return;
    this.router.navigate(['/tenants', this.locatario.id, 'edit']);
  }

  back() {
    this.router.navigateByUrl('/tenants');
  }

  statusLabel() {
    if (!this.locatario) return '';
    if (!this.locatario.ativo) return 'Inativo';
    if (this.locatario.bloqueado) return 'Bloqueado';
    return 'Ativo';
  }
}

