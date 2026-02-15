import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { CompanyService, EmpresaResponse } from './company.service';
import { CompanyContextService } from './company-context.service';

@Component({
  selector: 'app-company-form',
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
    RouterLink
  ],
  templateUrl: './company-form.component.html'
})
export class CompanyFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Nova empresa';
  saving = false;
  loading = false;
  empresa: EmpresaResponse | null = null;
  matrizesAtivas: EmpresaResponse[] = [];
  returnTo = '/companies';
  defaultEmpresaId = 0;

  form = this.fb.group({
    tipo: ['MATRIZ', Validators.required],
    matrizId: [null as number | null],
    razaoSocial: ['', Validators.required],
    nomeFantasia: [''],
    cnpj: ['', [Validators.required, Validators.pattern(/^\d{14}$/)]],
    ativo: [true, Validators.required],
    empresaPadrao: [false]
  });

  constructor(
    private fb: FormBuilder,
    private service: CompanyService,
    private companyContextService: CompanyContextService,
    private route: ActivatedRoute,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    this.mode = id ? (isEdit ? 'edit' : 'view') : 'new';
    this.title = this.mode === 'new' ? 'Nova empresa' : this.mode === 'edit' ? 'Editar empresa' : 'Consultar empresa';
    const tipo = (this.route.snapshot.queryParamMap.get('tipo') || '').toUpperCase();
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    if (returnTo) {
      this.returnTo = returnTo;
    }
    if (this.mode === 'new' && (tipo === 'FILIAL' || tipo === 'MATRIZ')) {
      this.form.patchValue({ tipo: tipo as 'FILIAL' | 'MATRIZ' });
    }
    this.loadDefaultEmpresa();
    this.loadMatrizesAtivas();
    if (id) this.load(Number(id));
    this.form.get('tipo')?.valueChanges.subscribe(tipo => {
      const matrizCtrl = this.form.get('matrizId');
      if (tipo === 'FILIAL') {
        matrizCtrl?.addValidators([Validators.required]);
      } else {
        matrizCtrl?.clearValidators();
        matrizCtrl?.setValue(null);
      }
      matrizCtrl?.updateValueAndValidity();
    });
    this.form.get('tipo')?.updateValueAndValidity({ emitEvent: true });
  }

  private loadMatrizesAtivas() {
    this.service.list({ page: 0, size: 200, tipo: 'MATRIZ', ativo: true }).subscribe({
      next: data => (this.matrizesAtivas = data.content || [])
    });
  }

  private load(id: number) {
    this.loading = true;
    this.service.get(id).pipe(finalize(() => (this.loading = false))).subscribe({
      next: e => {
        this.empresa = e;
        this.form.patchValue({
          tipo: e.tipo,
          matrizId: e.matrizId || null,
          razaoSocial: e.razaoSocial,
          nomeFantasia: e.nomeFantasia || '',
          cnpj: e.cnpj,
          ativo: e.ativo,
          empresaPadrao: this.isEmpresaPadraoEmpresaId(e.id)
        });
        if (this.mode === 'view') {
          this.form.disable();
        } else {
          this.form.enable();
        }
      },
      error: () => this.notify.error('Não foi possível carregar a empresa.')
    });
  }

  save() {
    if (this.mode === 'view') {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const tipo = this.form.value.tipo as 'MATRIZ' | 'FILIAL';
    const body = {
      razaoSocial: this.form.value.razaoSocial!,
      nomeFantasia: this.form.value.nomeFantasia || undefined,
      cnpj: this.form.value.cnpj!,
      ativo: !!this.form.value.ativo
    };

    this.saving = true;
    if (this.mode === 'new') {
      const request = tipo === 'FILIAL'
        ? this.service.createFilial({ ...body, matrizId: this.form.value.matrizId! })
        : this.service.createMatriz(body);
      request.pipe(finalize(() => (this.saving = false))).subscribe({
        next: (created) => {
          this.applyEmpresaPadraoSelection(created.id).subscribe({
            next: () => {
              this.notify.success('Empresa criada.');
              this.router.navigateByUrl(this.returnTo);
            },
            error: () => {
              this.notify.error('Empresa criada, mas não foi possível atualizar a empresa padrão.');
              this.router.navigateByUrl(this.returnTo);
            }
          });
        },
        error: () => this.notify.error('Não foi possível salvar a empresa.')
      });
      return;
    }

    if (!this.empresa) {
      this.saving = false;
      return;
    }
    this.service.update(this.empresa.id, body).pipe(finalize(() => (this.saving = false))).subscribe({
      next: (updated) => {
        this.applyEmpresaPadraoSelection(updated.id || this.empresa!.id).subscribe({
          next: () => {
            this.notify.success('Empresa atualizada.');
            this.router.navigateByUrl(this.returnTo);
          },
          error: () => {
            this.notify.error('Empresa atualizada, mas não foi possível atualizar a empresa padrão.');
            this.router.navigateByUrl(this.returnTo);
          }
        });
      },
      error: () => this.notify.error('Não foi possível atualizar a empresa.')
    });
  }

  back() {
    this.router.navigateByUrl(this.returnTo);
  }

  isEmpresaPadrao(): boolean {
    if (!this.empresa?.id) return false;
    return this.isEmpresaPadraoEmpresaId(this.empresa.id);
  }

  private loadDefaultEmpresa(): void {
    this.companyContextService.getDefault().subscribe({
      next: pref => {
        this.defaultEmpresaId = Number(pref?.empresaId || 0);
        if (this.empresa?.id) {
          this.form.patchValue({ empresaPadrao: this.isEmpresaPadraoEmpresaId(this.empresa.id) });
        }
      },
      error: () => this.defaultEmpresaId = 0
    });
  }

  private isEmpresaPadraoEmpresaId(empresaId: number): boolean {
    return !!this.defaultEmpresaId && this.defaultEmpresaId === empresaId;
  }

  private applyEmpresaPadraoSelection(empresaId: number): Observable<void> {
    if (!empresaId) return of(void 0);
    const shouldBeDefault = !!this.form.value.empresaPadrao;
    const currentDefaultId = this.defaultEmpresaId;

    if (shouldBeDefault) {
      return new Observable<void>(subscriber => {
        this.companyContextService.setDefault(empresaId).subscribe({
          next: () => {
            this.defaultEmpresaId = empresaId;
            localStorage.setItem('empresaContextId', String(empresaId));
            this.notifyEmpresaContextUpdated();
            subscriber.next();
            subscriber.complete();
          },
          error: err => subscriber.error(err)
        });
      });
    }
    if (currentDefaultId === empresaId) {
      return new Observable<void>(subscriber => {
        this.companyContextService.clearDefault().subscribe({
          next: () => {
            this.defaultEmpresaId = 0;
            this.notifyEmpresaContextUpdated();
            subscriber.next();
            subscriber.complete();
          },
          error: err => subscriber.error(err)
        });
      });
    }
    return of(void 0);
  }

  private notifyEmpresaContextUpdated(): void {
    if (typeof window === 'undefined') return;
    window.dispatchEvent(new CustomEvent('empresa-context-updated'));
  }
}

