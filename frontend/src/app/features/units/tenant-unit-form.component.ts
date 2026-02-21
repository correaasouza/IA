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
import { TenantUnit, UnitsService } from './units.service';

interface OfficialOption {
  id: string;
  codigo: string;
  descricao: string;
  ativo: boolean;
}

@Component({
  selector: 'app-tenant-unit-form',
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
  templateUrl: './tenant-unit-form.component.html'
})
export class TenantUnitFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  loading = false;
  saving = false;
  deleting = false;
  unit: TenantUnit | null = null;
  officialOptions: OfficialOption[] = [];
  title = 'Nova unidade';

  form = this.fb.group({
    unidadeOficialId: ['', [Validators.required]],
    sigla: ['', [Validators.required, Validators.maxLength(20)]],
    nome: ['', [Validators.required, Validators.maxLength(160)]],
    fatorParaOficial: [1, [Validators.required, Validators.min(0)]]
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
      ? 'Nova unidade'
      : this.mode === 'edit'
        ? 'Editar unidade'
        : 'Consultar unidade';

    this.loadOfficialOptions(() => {
      if (!id) {
        if (this.mode === 'view') {
          this.form.disable();
        }
        return;
      }
      this.loadUnit(id);
    });
  }

  nomeAtual(): string {
    const sigla = (this.form.value.sigla || '').trim();
    const nome = (this.form.value.nome || '').trim();
    if (!sigla && !nome) return '-';
    return `${sigla}${nome ? ' - ' + nome : ''}`;
  }

  back(): void {
    this.router.navigate(['/tenant-units']);
  }

  toEdit(): void {
    if (!this.unit?.id) return;
    this.router.navigate(['/tenant-units', this.unit.id, 'edit']);
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      unidadeOficialId: String(this.form.value.unidadeOficialId || ''),
      sigla: (this.form.value.sigla || '').trim().toUpperCase(),
      nome: (this.form.value.nome || '').trim(),
      fatorParaOficial: Number(this.form.value.fatorParaOficial || 0)
    };

    this.saving = true;
    if (this.mode === 'new') {
      this.unitsService.createTenantUnit(payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.notify.success('Unidade criada.');
            this.back();
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar a unidade.')
        });
      return;
    }

    if (!this.unit?.id) {
      this.saving = false;
      return;
    }
    this.unitsService.updateTenantUnit(this.unit.id, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Unidade atualizada.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar a unidade.')
      });
  }

  remove(): void {
    if (!this.unit?.id) return;
    if (!confirm(`Excluir a unidade "${this.unit.sigla}"?`)) return;
    this.deleting = true;
    this.unitsService.deleteTenantUnit(this.unit.id)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Unidade excluida.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir a unidade.')
      });
  }

  officialLabel(id: string): string {
    const option = this.officialOptions.find(item => item.id === id);
    if (!option) return '-';
    return `${option.codigo} - ${option.descricao}`;
  }

  private loadOfficialOptions(onLoaded: () => void): void {
    this.loading = true;
    this.unitsService.listTenantUnits()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          const byId = new Map<string, OfficialOption>();
          for (const row of rows || []) {
            if (!row.unidadeOficialId) continue;
            byId.set(row.unidadeOficialId, {
              id: row.unidadeOficialId,
              codigo: row.unidadeOficialCodigo,
              descricao: row.unidadeOficialDescricao,
              ativo: row.unidadeOficialAtiva
            });
          }
          this.officialOptions = [...byId.values()].sort((a, b) => a.codigo.localeCompare(b.codigo));
          onLoaded();
        },
        error: err => {
          this.officialOptions = [];
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar unidades oficiais do locatario.');
          onLoaded();
        }
      });
  }

  private loadUnit(id: string): void {
    this.loading = true;
    this.unitsService.getTenantUnit(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: unit => {
          this.unit = unit;
          if (!this.officialOptions.some(item => item.id === unit.unidadeOficialId)) {
            this.officialOptions = [
              ...this.officialOptions,
              {
                id: unit.unidadeOficialId,
                codigo: unit.unidadeOficialCodigo,
                descricao: unit.unidadeOficialDescricao,
                ativo: unit.unidadeOficialAtiva
              }
            ].sort((a, b) => a.codigo.localeCompare(b.codigo));
          }
          this.form.patchValue({
            unidadeOficialId: unit.unidadeOficialId,
            sigla: unit.sigla,
            nome: unit.nome,
            fatorParaOficial: Number(unit.fatorParaOficial || 0)
          });
          if (this.mode === 'view') {
            this.form.disable();
          }
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar a unidade.');
          this.back();
        }
      });
  }
}

