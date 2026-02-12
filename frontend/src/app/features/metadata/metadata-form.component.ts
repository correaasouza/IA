import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { MetadataService, TipoEntidade, TipoEntidadeCampoRegra } from './metadata.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-metadata-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSlideToggleModule,
    MatIconModule,
    InlineLoaderComponent
  ],
  templateUrl: './metadata-form.component.html',
})
export class MetadataFormComponent implements OnInit {
  tipoId: number | null = null;
  tipo: TipoEntidade | null = null;
  campos: TipoEntidadeCampoRegra[] = [];
  campoColumns = ['campo', 'label', 'habilitado', 'requerido', 'visivel', 'editavel'];
  loadingTipo = false;
  loadingCampos = false;
  savingTipo = false;
  savingCampos = false;

  form = this.fb.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: MetadataService,
    private route: ActivatedRoute,
    private router: Router,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      this.tipoId = id ? Number(id) : null;
      if (this.tipoId) {
        this.loadTipo(this.tipoId);
        this.loadCampos();
      } else {
        this.tipo = null;
        this.form.reset({ ativo: true });
        this.campos = [];
      }
    });
  }

  private loadTipo(id: number) {
    const tenantId = localStorage.getItem('tenantId') || '1';
    const stateTipo = history.state?.tipo as TipoEntidade | undefined;
    if (stateTipo && stateTipo.id === id) {
      this.tipo = stateTipo;
      this.form.reset({
        codigo: stateTipo.codigo,
        nome: stateTipo.nome,
        ativo: stateTipo.ativo
      });
      return;
    }
    this.loadingTipo = true;
    this.service.listTipos(tenantId).pipe(finalize(() => this.loadingTipo = false)).subscribe({
      next: data => {
        const found = (data.content || []).find((t: TipoEntidade) => t.id === id) || null;
        if (!found) {
          this.notify.error('Tipo não encontrado.');
          this.router.navigate(['/metadata']);
          return;
        }
        this.tipo = found;
        this.form.reset({
          codigo: found.codigo,
          nome: found.nome,
          ativo: found.ativo
        });
      },
      error: () => {
        this.notify.error('Não foi possível carregar o tipo.');
      }
    });
  }

  private loadCampos() {
    if (!this.tipoId) return;
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.loadingCampos = true;
    this.service.listCampos(tenantId, this.tipoId).pipe(finalize(() => this.loadingCampos = false)).subscribe({
      next: data => this.campos = data || [],
      error: () => {
        this.campos = [];
        this.notify.error('Não foi possível carregar os campos.');
      }
    });
  }

  saveTipo() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      codigo: (this.form.value.codigo || '').trim().toUpperCase(),
      nome: this.form.value.nome,
      ativo: this.form.value.ativo ?? true
    };

    this.savingTipo = true;
    if (this.tipoId) {
      this.service.updateTipo(this.tipoId, payload).pipe(finalize(() => this.savingTipo = false)).subscribe({
        next: data => {
          this.tipo = data;
          this.notify.success('Tipo atualizado.');
        },
        error: () => this.notify.error('Não foi possível salvar o tipo.')
      });
      return;
    }

    this.service.createTipo(payload).pipe(finalize(() => this.savingTipo = false)).subscribe({
      next: data => {
        this.notify.success('Tipo criado.');
        this.router.navigate(['/metadata', data.id, 'edit'], { state: { tipo: data } });
      },
      error: () => this.notify.error('Não foi possível criar o tipo.')
    });
  }

  saveCampos() {
    if (!this.tipoId) return;
    const payload = this.campos.map(c => ({
      campo: c.campo,
      habilitado: c.habilitado,
      requerido: c.requerido,
      visivel: c.visivel,
      editavel: c.editavel,
      label: c.label
    }));
    this.savingCampos = true;
    this.service.saveCampos(this.tipoId, payload).pipe(finalize(() => this.savingCampos = false)).subscribe({
      next: () => this.notify.success('Configurações salvas.'),
      error: () => this.notify.error('Não foi possível salvar as configurações.')
    });
  }

  cancel() {
    this.router.navigate(['/metadata']);
  }
}

