import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { RhQualificacao, RhQualificacoesService } from './rh-qualificacoes.service';

@Component({
  selector: 'app-rh-qualificacao-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './rh-qualificacao-form.component.html'
})
export class RhQualificacaoFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  loading = false;
  saving = false;
  deleting = false;
  row: RhQualificacao | null = null;
  title = 'Nova qualificacao RH';

  form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['', [Validators.maxLength(1)]],
    completo: [false],
    ativo: [true]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: RhQualificacoesService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');
    this.mode = !id ? 'new' : isEdit ? 'edit' : 'view';
    this.title = this.mode === 'new'
      ? 'Nova qualificacao RH'
      : this.mode === 'edit'
        ? 'Editar qualificacao RH'
        : 'Consultar qualificacao RH';

    if (!id) {
      if (this.mode === 'view') this.form.disable();
      return;
    }

    this.loading = true;
    this.service.getById(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: row => {
          this.row = row;
          this.form.patchValue({
            nome: row.nome,
            tipo: row.tipo || '',
            completo: !!row.completo,
            ativo: !!row.ativo
          });
          if (this.mode === 'view') this.form.disable();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a qualificacao RH.');
          this.back();
        }
      });
  }

  nomeAtual(): string {
    return (this.form.value.nome || '').trim() || '-';
  }

  back(): void {
    this.router.navigate(['/configs/rh-qualificacoes']);
  }

  toEdit(): void {
    if (!this.row?.id) return;
    this.router.navigate(['/configs/rh-qualificacoes', this.row.id, 'edit']);
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = {
      nome: (raw.nome || '').trim(),
      tipo: (raw.tipo || '').trim().toUpperCase() || null,
      completo: !!raw.completo,
      ativo: !!raw.ativo
    };

    this.saving = true;
    if (this.mode === 'new') {
      this.service.create(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.notify.success('Qualificacao RH criada.');
            this.back();
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar a qualificacao RH.')
        });
      return;
    }

    if (!this.row?.id) {
      this.saving = false;
      return;
    }
    this.service.update(this.row.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Qualificacao RH atualizada.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a qualificacao RH.')
      });
  }

  remove(): void {
    if (!this.row?.id) return;
    if (!confirm(`Inativar a qualificacao "${this.row.nome}"?`)) return;
    this.deleting = true;
    this.service.delete(this.row.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Qualificacao RH inativada.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel inativar a qualificacao RH.')
      });
  }
}
