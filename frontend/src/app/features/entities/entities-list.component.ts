import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { EntidadeService, EntidadeDefinicao, EntidadeRegistro } from './entidade.service';
import { ConfigService } from '../configs/config.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-entities-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatSelectModule,
    MatPaginatorModule,
    FormsModule,
    MatIconModule,
    MatDialogModule,
    InlineLoaderComponent
  ],
  templateUrl: './entities-list.component.html',
  styleUrls: ['./entities-list.component.css']
})
export class EntitiesListComponent implements OnInit {
  definicoes: EntidadeDefinicao[] = [];
  registros: EntidadeRegistro[] = [];
  selectedDefId: number | null = null;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  loading = false;

  visible = { nome: true, apelido: true, cpfCnpj: true };
  labels = { nome: 'Nome', apelido: 'Apelido', cpfCnpj: 'CPF/CNPJ' };

  columns: string[] = ['nome', 'apelido', 'cpfCnpj', 'ativo', 'acoes'];

  filters = this.fb.group({
    nome: [''],
    cpfCnpj: [''],
    ativo: ['']
  });

  constructor(
    private fb: FormBuilder,
    private service: EntidadeService,
    private config: ConfigService,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadDef();
  }

  loadDef() {
    this.service.listDef(0, 50).subscribe({
      next: data => {
        this.definicoes = data.content || [];
        if (this.definicoes.length > 0 && this.definicoes[0]) {
          this.selectedDefId = this.definicoes[0].id;
          this.changeDef();
        }
      }
    });
  }

  changeDef() {
    if (!this.selectedDefId) return;
    this.loadConfig();
    this.loadReg();
  }

  loadReg() {
    if (!this.selectedDefId) return;
    this.loading = true;
    const ativo = this.filters.value.ativo || '';
    this.service.listReg(this.selectedDefId, this.pageIndex, this.pageSize, {
      nome: this.filters.value.nome || '',
      cpfCnpj: this.filters.value.cpfCnpj || '',
      ativo
    }).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.registros = data.content || [];
        this.totalElements = data.totalElements || 0;
      },
      error: () => {
        this.registros = [];
        this.totalElements = 0;
      }
    });
  }

  loadConfig() {
    const screenId = `entities-${this.selectedDefId}`;
    const rolesKeycloak = (JSON.parse(localStorage.getItem('roles') || '[]') as string[]);
    const rolesTenant = (JSON.parse(localStorage.getItem('tenantRoles') || '[]') as string[]);
    const roles = Array.from(new Set([...rolesKeycloak, ...rolesTenant])).join(',');
    const userId = localStorage.getItem('userId') || '';

    this.config.getColunas(screenId, userId, roles).subscribe({
      next: cfg => this.applyConfig(cfg?.configJson || '{}')
    });

    this.config.getForm(screenId, userId, roles).subscribe({
      next: cfg => this.applyFormConfig(cfg?.configJson || '{}')
    });
  }

  applyConfig(configJson: string) {
    try {
      const cfg = JSON.parse(configJson);
      const cols = cfg?.columns || ['nome', 'apelido', 'cpfCnpj', 'ativo', 'acoes'];
      this.columns = cols.filter((c: string) => ['nome','apelido','cpfCnpj','ativo','acoes'].includes(c));
    } catch {
    }
  }

  applyFormConfig(configJson: string) {
    try {
      const cfg = JSON.parse(configJson);
      this.visible.apelido = cfg?.fields?.apelido?.visible ?? true;
      this.visible.cpfCnpj = cfg?.fields?.cpfCnpj?.visible ?? true;
      this.labels.nome = cfg?.fields?.nome?.label ?? 'Nome';
      this.labels.apelido = cfg?.fields?.apelido?.label ?? 'Apelido';
      this.labels.cpfCnpj = cfg?.fields?.cpfCnpj?.label ?? 'CPF/CNPJ';
    } catch {
    }
  }

  applyFilters() {
    this.pageIndex = 0;
    this.loadReg();
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadReg();
  }

  newRegistro() {
    if (!this.selectedDefId) return;
    this.router.navigate(['/entities/new'], { queryParams: { defId: this.selectedDefId } });
  }

  view(row: EntidadeRegistro) {
    this.router.navigate(['/entities', row.id]);
  }

  edit(row: EntidadeRegistro) {
    this.router.navigate(['/entities', row.id, 'edit']);
  }

  remove(row: EntidadeRegistro) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir registro', message: `Deseja excluir o registro "${row.nome}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.deleteReg(row.id).subscribe({
        next: () => {
          this.notify.success('Registro removido.');
          this.loadReg();
        },
        error: () => this.notify.error('Não foi possível remover o registro.')
      });
    });
  }
}
