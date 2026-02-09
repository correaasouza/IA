import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { EntidadeService, EntidadeDefinicao } from './entidade.service';
import { ContatoTiposComponent } from './contato-tipos.component';
import { ContatoTiposPorEntidadeComponent } from './contato-tipos-por-entidade.component';
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
    MatSelectModule,
    FormsModule,
    MatIconModule,
    ContatoTiposComponent,
    ContatoTiposPorEntidadeComponent,
    InlineLoaderComponent
  ],
  templateUrl: './entities-config.component.html',
  styleUrls: ['./entities-config.component.css']
})
export class EntitiesConfigComponent implements OnInit {
  definicoes: EntidadeDefinicao[] = [];
  defColumns: string[] = ['nome', 'role', 'ativo', 'save'];
  selectedDefId: number | null = null;
  loading = false;
  savingIds = new Set<number>();
  savedIds = new Set<number>();
  errorIds = new Set<number>();

  constructor(private service: EntidadeService) {}

  ngOnInit(): void {
    this.loading = true;
    this.service.listDef(0, 50).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.definicoes = data.content || [];
        if (this.definicoes.length > 0 && this.definicoes[0]) {
          this.selectedDefId = this.definicoes[0].id;
        }
      }
    });
  }

  saveDef(row: EntidadeDefinicao) {
    this.savingIds.add(row.id);
    this.savedIds.delete(row.id);
    this.errorIds.delete(row.id);
    const payload = {
      codigo: row.codigo,
      nome: row.nome,
      ativo: row.ativo,
      roleRequired: row.roleRequired || null
    };
    this.service.updateDef(row.id, payload).pipe(finalize(() => this.savingIds.delete(row.id))).subscribe({
      next: () => this.savedIds.add(row.id),
      error: () => this.errorIds.add(row.id)
    });
  }
}
