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
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { TenantUnit, TenantUnitConversion, UnitsService } from './units.service';

@Component({
  selector: 'app-tenant-unit-conversion-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './tenant-unit-conversion-form.component.html'
})
export class TenantUnitConversionFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  loading = false;
  saving = false;
  deleting = false;
  conversion: TenantUnitConversion | null = null;
  unitOptions: TenantUnit[] = [];
  title = 'Nova conversao';

  form = this.fb.group({
    unidadeOrigemId: ['', [Validators.required]],
    unidadeDestinoId: ['', [Validators.required]],
    fator: [1, [Validators.required, Validators.min(0.000000000001)]]
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
      ? 'Nova conversao'
      : this.mode === 'edit'
        ? 'Editar conversao'
        : 'Consultar conversao';

    this.loadUnits(() => {
      if (!id) {
        if (this.mode === 'view') this.form.disable();
        return;
      }
      this.loadConversion(id);
    });
  }

  nomeAtual(): string {
    const origem = this.unitLabel(String(this.form.value.unidadeOrigemId || ''));
    const destino = this.unitLabel(String(this.form.value.unidadeDestinoId || ''));
    if (origem === '-' && destino === '-') return '-';
    return `${origem} -> ${destino}`;
  }

  back(): void {
    this.router.navigate(['/tenant-unit-conversions']);
  }

  toEdit(): void {
    if (!this.conversion?.id) return;
    this.router.navigate(['/tenant-unit-conversions', this.conversion.id, 'edit']);
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const unidadeOrigemId = String(this.form.value.unidadeOrigemId || '');
    const unidadeDestinoId = String(this.form.value.unidadeDestinoId || '');
    if (unidadeOrigemId === unidadeDestinoId) {
      this.notify.error('Origem e destino devem ser diferentes.');
      return;
    }
    const payload = {
      unidadeOrigemId,
      unidadeDestinoId,
      fator: Number(this.form.value.fator || 0)
    };

    this.saving = true;
    if (this.mode === 'new') {
      this.unitsService.createTenantUnitConversion(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.notify.success('Conversao criada.');
            this.back();
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar a conversao.')
        });
      return;
    }

    if (!this.conversion?.id) {
      this.saving = false;
      return;
    }
    this.unitsService.updateTenantUnitConversion(this.conversion.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Conversao atualizada.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a conversao.')
      });
  }

  remove(): void {
    if (!this.conversion?.id) return;
    if (!confirm(`Excluir a conversao ${this.conversion.unidadeOrigemSigla} -> ${this.conversion.unidadeDestinoSigla}?`)) {
      return;
    }
    this.deleting = true;
    this.unitsService.deleteTenantUnitConversion(this.conversion.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Conversao excluida.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a conversao.')
      });
  }

  unitLabel(id: string): string {
    const option = this.unitOptions.find(item => item.id === id);
    if (!option) return '-';
    return `${option.sigla} - ${option.nome}`;
  }

  private loadUnits(done: () => void): void {
    this.loading = true;
    this.unitsService.listTenantUnits()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.unitOptions = (rows || []).slice().sort((a, b) => a.sigla.localeCompare(b.sigla));
          done();
        },
        error: err => {
          this.unitOptions = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar unidades do locatario.');
          done();
        }
      });
  }

  private loadConversion(id: string): void {
    this.loading = true;
    this.unitsService.getTenantUnitConversion(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: conversion => {
          this.conversion = conversion;
          this.form.patchValue({
            unidadeOrigemId: conversion.unidadeOrigemId,
            unidadeDestinoId: conversion.unidadeDestinoId,
            fator: Number(conversion.fator || 0)
          });
          if (this.mode === 'view') {
            this.form.disable();
          }
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a conversao.');
          this.back();
        }
      });
  }
}

