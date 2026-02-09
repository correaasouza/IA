import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { ContatoTipoService, ContatoTipo } from './contato-tipo.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';

@Component({
  selector: 'app-contato-tipos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatTableModule, MatSlideToggleModule, MatIconModule, InlineLoaderComponent],
  templateUrl: './contato-tipos.component.html',
  styleUrls: ['./contato-tipos.component.css']
})
export class ContatoTiposComponent implements OnInit {
  tipos: ContatoTipo[] = [];
  columns = ['codigo', 'nome', 'mascara', 'regex', 'ativo', 'obrigatorio', 'principalUnico', 'acoes'];
  loading = false;
  savingIds = new Set<number>();
  savedIds = new Set<number>();
  errorIds = new Set<number>();

  form = this.fb.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    mascara: [''],
    regexValidacao: [''],
    ativo: [true],
    obrigatorio: [false],
    principalUnico: [true]
  });

  constructor(private fb: FormBuilder, private service: ContatoTipoService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.service.list().pipe(finalize(() => this.loading = false)).subscribe({
      next: data => this.tipos = data,
      error: () => this.tipos = []
    });
  }

  create() {
    if (this.form.invalid) return;
    this.service.create(this.form.value).subscribe({
      next: () => {
        this.form.reset({ ativo: true, obrigatorio: false, principalUnico: true, mascara: '', regexValidacao: '' });
        this.load();
      }
    });
  }

  save(row: ContatoTipo) {
    this.savingIds.add(row.id);
    this.savedIds.delete(row.id);
    this.errorIds.delete(row.id);
    const payload = {
      codigo: row.codigo,
      nome: row.nome,
      ativo: row.ativo,
      obrigatorio: row.obrigatorio,
      principalUnico: row.principalUnico,
      mascara: row.mascara || null,
      regexValidacao: row.regexValidacao || null
    };
    this.service.update(row.id, payload).pipe(finalize(() => this.savingIds.delete(row.id))).subscribe({
      next: () => this.savedIds.add(row.id),
      error: () => this.errorIds.add(row.id)
    });
  }
}
