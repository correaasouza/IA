import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { TipoEntidadeService, TipoEntidade } from './tipo-entidade.service';
import { ContatoTiposComponent } from './contato-tipos.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';

@Component({
  selector: 'app-entities-config',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    FormsModule,
    MatIconModule,
    ContatoTiposComponent,
    InlineLoaderComponent
  ],
  templateUrl: './entities-config.component.html'
})
export class EntitiesConfigComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  defColumns: string[] = ['codigo', 'nome', 'ativo', 'save'];
  loading = false;
  savingIds = new Set<number>();
  savedIds = new Set<number>();
  errorIds = new Set<number>();

  constructor(private service: TipoEntidadeService) {}

  ngOnInit(): void {
    this.loading = true;
    this.service.list(0, 100).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.tipos = data.content || [];
      }
    });
  }

  saveDef(row: TipoEntidade) {
    this.savingIds.add(row.id);
    this.savedIds.delete(row.id);
    this.errorIds.delete(row.id);
    const payload = {
      codigo: row.codigo,
      nome: row.nome,
      ativo: row.ativo
    };
    this.service.update(row.id, payload).pipe(finalize(() => this.savingIds.delete(row.id))).subscribe({
      next: () => this.savedIds.add(row.id),
      error: () => this.errorIds.add(row.id)
    });
  }
}

