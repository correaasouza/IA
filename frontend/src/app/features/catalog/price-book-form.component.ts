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
  selector: 'app-price-book-form',
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
  templateUrl: './price-book-form.component.html'
})
export class PriceBookFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  bookId: number | null = null;
  loading = false;
  saving = false;
  deleting = false;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    active: [true, Validators.required],
    defaultBook: [false]
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
      this.bookId = Number(idParam);
      this.mode = isEdit ? 'edit' : 'view';
      this.load(this.bookId);
      return;
    }
    this.mode = 'new';
    this.applyModeState();
  }

  title(): string {
    if (this.mode === 'new') return 'Nova Tabela de Preco';
    if (this.mode === 'edit') return 'Editar Tabela de Preco';
    return 'Consultar Tabela de Preco';
  }

  save(): void {
    if (this.mode === 'view') return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      name: (this.form.value.name || '').trim(),
      active: !!this.form.value.active,
      defaultBook: !!this.form.value.defaultBook
    };

    this.saving = true;
    const request$ = this.mode === 'new'
      ? this.pricingService.createBook(payload)
      : this.pricingService.updateBook(this.bookId!, payload);

    request$.pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => {
        this.notify.success(this.mode === 'new' ? 'Tabela de preco criada.' : 'Tabela de preco atualizada.');
        this.back();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar tabela de preco.')
    });
  }

  remove(): void {
    if (!this.bookId || this.mode === 'new') return;
    if (!confirm('Excluir esta tabela de preco?')) return;

    this.deleting = true;
    this.pricingService.deleteBook(this.bookId)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success('Tabela de preco excluida.');
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir tabela de preco.')
      });
  }

  toEdit(): void {
    if (!this.bookId) return;
    this.router.navigate(['/catalog/pricing/books', this.bookId, 'edit']);
  }

  back(): void {
    this.router.navigate(['/catalog/configuration'], {
      queryParams: { subTab: 'PRICE_BOOKS' }
    });
  }

  private load(id: number): void {
    this.loading = true;
    this.pricingService.getBook(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: data => {
          this.form.patchValue({
            name: data.name || '',
            active: !!data.active,
            defaultBook: !!data.defaultBook
          });
          this.applyModeState();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar tabela de preco.');
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
