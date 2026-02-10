import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { MetadataService, TipoEntidade, TipoEntidadeCampoRegra } from './metadata.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';

@Component({
  selector: 'app-metadata',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSlideToggleModule,
    MatIconModule,
    FormsModule,
    InlineLoaderComponent
  ],
  templateUrl: './metadata.component.html',
  styleUrls: ['./metadata.component.css']
})
export class MetadataComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  campos: TipoEntidadeCampoRegra[] = [];
  tipoSelecionado: TipoEntidade | null = null;
  tipoColumns = ['codigo', 'nome', 'ativo', 'acoes'];
  campoColumns = ['campo', 'label', 'habilitado', 'requerido', 'visivel', 'editavel'];
  loadingCampos = false;
  savingCampos = false;

  tipoForm = this.fb.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    ativo: [true]
  });

  constructor(
    private fb: FormBuilder,
    private service: MetadataService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadTipos();
  }

  loadTipos() {
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.service.listTipos(tenantId).subscribe({
      next: data => this.tipos = data.content || [],
      error: () => this.tipos = []
    });
  }

  selectTipo(tipo: TipoEntidade) {
    this.tipoSelecionado = tipo;
    this.loadCampos();
  }

  loadCampos() {
    if (!this.tipoSelecionado) {
      this.campos = [];
      return;
    }
    const tenantId = localStorage.getItem('tenantId') || '1';
    this.loadingCampos = true;
    this.service.listCampos(tenantId, this.tipoSelecionado.id).pipe(finalize(() => this.loadingCampos = false)).subscribe({
      next: data => this.campos = data || [],
      error: () => this.campos = []
    });
  }

  createTipo() {
    if (this.tipoForm.invalid) {
      return;
    }
    const payload = {
      codigo: this.tipoForm.value.codigo?.trim().toUpperCase(),
      nome: this.tipoForm.value.nome,
      ativo: this.tipoForm.value.ativo ?? true
    };
    this.service.createTipo(payload).subscribe({
      next: () => {
        this.tipoForm.reset({ ativo: true });
        this.loadTipos();
      }
    });
  }

  saveCampos() {
    if (!this.tipoSelecionado) return;
    const payload = this.campos.map(c => ({
      campo: c.campo,
      habilitado: c.habilitado,
      requerido: c.requerido,
      visivel: c.visivel,
      editavel: c.editavel,
      label: c.label
    }));
    this.savingCampos = true;
    this.service.saveCampos(this.tipoSelecionado.id, payload).pipe(finalize(() => this.savingCampos = false)).subscribe({
      next: () => this.notify.success('Configurações salvas.'),
      error: () => this.notify.error('Não foi possível salvar as configurações.')
    });
  }
}
