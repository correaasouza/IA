import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { OfficialUnit, OfficialUnitOrigin, UnitsService } from './units.service';

@Component({
  selector: 'app-official-unit-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './official-unit-form.component.html'
})
export class OfficialUnitFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  loading = false;
  saving = false;
  deleting = false;
  unit: OfficialUnit | null = null;
  title = 'Nova unidade oficial';

  form = this.fb.group({
    codigoOficial: ['', [Validators.required, Validators.maxLength(20)]],
    descricao: ['', [Validators.required, Validators.maxLength(160)]],
    ativo: [true, Validators.required],
    origem: ['MANUAL' as OfficialUnitOrigin]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly unitsService: UnitsService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');
    this.mode = !id ? 'new' : isEdit ? 'edit' : 'view';
    this.title = this.mode === 'new'
      ? 'Nova unidade oficial'
      : this.mode === 'edit'
        ? 'Editar unidade oficial'
        : 'Consultar unidade oficial';

    if (!id) {
      if (this.mode === 'view') {
        this.form.disable();
      }
      return;
    }

    this.loading = true;
    this.unitsService.getOfficial(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: unit => {
          this.unit = unit;
          this.form.patchValue({
            codigoOficial: unit.codigoOficial,
            descricao: unit.descricao,
            ativo: unit.ativo,
            origem: unit.origem
          });
          if (this.mode === 'view') {
            this.form.disable();
          } else {
            this.form.controls.codigoOficial.disable();
          }
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a unidade oficial.');
          this.back();
        }
      });
  }

  nomeAtual(): string {
    const codigo = (this.form.getRawValue().codigoOficial || '').trim();
    const descricao = (this.form.value.descricao || '').trim();
    if (!codigo && !descricao) {
      return '-';
    }
    return `${codigo}${descricao ? ' - ' + descricao : ''}`;
  }

  back(): void {
    this.router.navigate(['/global-settings/official-units']);
  }

  toEdit(): void {
    if (!this.unit?.id) return;
    this.router.navigate(['/global-settings/official-units', this.unit.id, 'edit']);
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      codigoOficial: (raw.codigoOficial || '').trim().toUpperCase(),
      descricao: (raw.descricao || '').trim(),
      ativo: !!raw.ativo,
      origem: raw.origem || 'MANUAL'
    };

    this.saving = true;
    if (this.mode === 'new') {
      this.unitsService.createOfficial(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.notify.success('Unidade oficial criada.');
            this.back();
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar a unidade oficial.')
        });
      return;
    }

    if (!this.unit?.id) {
      this.saving = false;
      return;
    }
    this.unitsService.updateOfficial(this.unit.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Unidade oficial atualizada.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a unidade oficial.')
      });
  }

  remove(): void {
    if (!this.unit?.id) return;
    if (!confirm(`Excluir a unidade oficial "${this.unit.codigoOficial}"?`)) {
      return;
    }
    this.deleting = true;
    this.unitsService.deleteOfficial(this.unit.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Unidade oficial excluida.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a unidade oficial.')
      });
  }
}

