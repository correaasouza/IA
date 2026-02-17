import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toDisplayDate, toIsoDate } from '../../shared/date-utils';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { MovementOperationService, MovimentoEstoqueResponse, MovimentoEstoqueTemplateResponse } from './movement-operation.service';

@Component({
  selector: 'app-movimento-estoque-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatDialogModule,
    DateMaskDirective,
    InlineLoaderComponent,
    AccessControlDirective
  ],
  templateUrl: './movimento-estoque-form.component.html',
  styleUrls: ['./movimento-estoque-form.component.css']
})
export class MovimentoEstoqueFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Novo movimento de estoque';
  returnTo = '/movimentos/estoque';
  loading = false;
  saving = false;
  deleting = false;
  errorMessage = '';
  movimento: MovimentoEstoqueResponse | null = null;
  templateData: MovimentoEstoqueTemplateResponse | null = null;

  form = this.fb.group({
    empresaId: [null as number | null, Validators.required],
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    dataMovimento: [''],
    version: [null as number | null]
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService,
    private service: MovementOperationService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id') || 0);
    const isEdit = this.route.snapshot.url.some(item => item.path === 'edit');
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo) {
      this.returnTo = returnTo;
    }
    this.mode = id > 0 ? (isEdit ? 'edit' : 'view') : 'new';
    this.title = this.mode === 'new'
      ? 'Novo movimento de estoque'
      : this.mode === 'edit'
        ? 'Editar movimento de estoque'
        : 'Consultar movimento de estoque';

    if (this.mode === 'new') {
      this.loadTemplate();
      return;
    }
    this.loadMovimento(id);
  }

  save(): void {
    if (this.mode === 'view') {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dataMovimentoInput = (this.form.value.dataMovimento || '').trim();
    if (dataMovimentoInput && !isValidDateInput(dataMovimentoInput)) {
      this.form.get('dataMovimento')?.setErrors({ invalidDate: true });
      this.form.get('dataMovimento')?.markAsTouched();
      return;
    }

    const empresaId = Number(this.form.value.empresaId || 0);
    const nome = (this.form.value.nome || '').trim();
    const dataMovimento = dataMovimentoInput ? toIsoDate(dataMovimentoInput) : null;
    this.saving = true;

    if (this.mode === 'new') {
      this.service.createEstoque({ empresaId, nome, dataMovimento })
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: created => {
            this.notify.success('Movimento de estoque criado.');
            this.router.navigate(['/movimentos/estoque', created.id], { queryParams: { returnTo: this.returnTo } });
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel criar o movimento de estoque.')
        });
      return;
    }

    if (!this.movimento?.id) {
      this.saving = false;
      return;
    }
    const version = Number(this.form.value.version ?? this.movimento.version);
    this.service.updateEstoque(this.movimento.id, { empresaId, nome, dataMovimento, version })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: updated => {
          this.notify.success('Movimento de estoque atualizado.');
          this.router.navigate(['/movimentos/estoque', updated.id], { queryParams: { returnTo: this.returnTo } });
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o movimento de estoque.')
      });
  }

  remove(): void {
    if (!this.movimento?.id) return;
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir movimento',
        message: `Deseja excluir o movimento "${this.movimento.nome}"?`,
        confirmText: 'Excluir',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.deleting = true;
      this.service.deleteEstoque(this.movimento!.id)
        .pipe(finalize(() => (this.deleting = false)))
        .subscribe({
          next: () => {
            this.notify.success('Movimento excluido.');
            this.router.navigateByUrl(this.returnTo);
          },
          error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir o movimento.')
        });
    });
  }

  toEdit(): void {
    if (!this.movimento?.id) return;
    this.router.navigate(['/movimentos/estoque', this.movimento.id, 'edit'], { queryParams: { returnTo: this.returnTo } });
  }

  back(): void {
    this.router.navigateByUrl(this.returnTo);
  }

  nomeAtual(): string {
    return (this.form.get('nome')?.value || this.movimento?.nome || '-').trim() || '-';
  }

  private loadTemplate(): void {
    const empresaId = Number(localStorage.getItem('empresaContextId') || 0);
    if (!empresaId) {
      this.errorMessage = 'Selecione uma empresa no topo do sistema para criar um movimento.';
      this.form.disable();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.service.buildTemplate('MOVIMENTO_ESTOQUE', empresaId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: template => {
          this.templateData = template;
          this.form.patchValue({
            empresaId: template.empresaId,
            nome: template.nome || '',
            dataMovimento: template.dataMovimento ? toDisplayDate(template.dataMovimento) : '',
            version: null
          });
          this.form.enable();
        },
        error: err => {
          this.errorMessage = err?.error?.detail || 'Nao foi possivel resolver a configuracao para criar o movimento.';
          this.form.disable();
        }
      });
  }

  private loadMovimento(id: number): void {
    if (!id) {
      this.errorMessage = 'Movimento invalido.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.service.getEstoque(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: movimento => {
          this.movimento = movimento;
          this.form.patchValue({
            empresaId: movimento.empresaId,
            nome: movimento.nome || '',
            dataMovimento: movimento.dataMovimento ? toDisplayDate(movimento.dataMovimento) : '',
            version: movimento.version
          });
          if (this.mode === 'view') {
            this.form.disable();
          } else {
            this.form.enable();
          }
        },
        error: err => {
          this.errorMessage = err?.error?.detail || 'Nao foi possivel carregar o movimento.';
          this.form.disable();
        }
      });
  }
}
