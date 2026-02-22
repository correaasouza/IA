import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { CatalogPricingService } from './catalog-pricing.service';

@Component({
  selector: 'app-price-variant-form',
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
  templateUrl: './price-variant-form.component.html'
})
export class PriceVariantFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  variantId: number | null = null;
  loading = false;
  saving = false;
  deleting = false;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    active: [true, Validators.required]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pricingService: CatalogPricingService,
    private readonly notify: NotificationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');
    if (idParam) {
      this.variantId = Number(idParam);
      this.mode = isEdit ? 'edit' : 'view';
      this.load(this.variantId);
      return;
    }
    this.mode = 'new';
    this.applyModeState();
  }

  title(): string {
    if (this.mode === 'new') return 'Nova Variacao de Preco';
    if (this.mode === 'edit') return 'Editar Variacao de Preco';
    return 'Consultar Variacao de Preco';
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      name: (this.form.value.name || '').trim(),
      active: !!this.form.value.active
    };

    this.saving = true;
    const request$ = this.mode === 'new'
      ? this.pricingService.createVariant(payload)
      : this.pricingService.updateVariant(this.variantId!, payload);

    request$.pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => {
        this.notify.success(this.mode === 'new' ? 'Variacao de preco criada.' : 'Variacao de preco atualizada.');
        this.back();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar variacao de preco.')
    });
  }

  remove(): void {
    if (!this.variantId || this.mode === 'new') return;
    if (!confirm('Excluir esta variacao de preco?')) return;

    this.deleting = true;
    this.pricingService.deleteVariant(this.variantId)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Variacao de preco excluida.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir variacao de preco.')
      });
  }

  toEdit(): void {
    if (!this.variantId) return;
    this.router.navigate(['/catalog/pricing/variants', this.variantId, 'edit']);
  }

  back(): void {
    this.router.navigate(['/catalog/configuration'], {
      queryParams: { subTab: 'PRICE_VARIANTS' }
    });
  }

  private load(id: number): void {
    this.loading = true;
    this.pricingService.getVariant(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: data => {
          this.form.patchValue({
            name: data.name || '',
            active: !!data.active
          });
          this.applyModeState();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar variacao de preco.');
          this.back();
        }
      });
  }

  private applyModeState(): void {
    if (this.mode === 'view') {
      this.form.disable();
      return;
    }
    this.form.enable();
  }
}
