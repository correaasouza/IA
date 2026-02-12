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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { TipoEntidadeService, TipoEntidade } from './tipo-entidade.service';
import { EntidadePapelService, EntidadePapel } from './entidade-papel.service';
import { PessoaService, Pessoa } from '../people/pessoa.service';
import { ConfigService } from '../configs/config.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { NotificationService } from '../../core/notifications/notification.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';

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
    MatTooltipModule,
    MatMenuModule,
    InlineLoaderComponent,
    FieldSearchComponent
  ],
  templateUrl: './entities-list.component.html'
})
export class EntitiesListComponent implements OnInit {
  tipos: TipoEntidade[] = [];
  registros: EntidadePapel[] = [];
  filtered: EntidadePapel[] = [];
  pessoas = new Map<number, Pessoa>();
  selectedTipoId: number | null = null;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  loading = false;

  columns: string[] = ['pessoa', 'documento', 'alerta', 'ativo', 'acoes'];

  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' },
    { key: 'documento', label: 'Documento' },
    { key: 'alerta', label: 'Alerta' }
  ];
  searchTerm = '';
  searchFields = ['nome', 'documento'];

  filters = this.fb.group({
    ativo: [''],
    comAlerta: [false]
  });

  constructor(
    private fb: FormBuilder,
    private tipoService: TipoEntidadeService,
    private service: EntidadePapelService,
    private pessoaService: PessoaService,
    private config: ConfigService,
    private router: Router,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadTipos();
  }

  loadTipos() {
    this.tipoService.list(0, 100).subscribe({
      next: data => {
        this.tipos = data.content || [];
        if (this.tipos.length > 0 && this.tipos[0]) {
          this.selectedTipoId = this.tipos[0].id;
          this.changeTipo();
        }
      }
    });
  }

  changeTipo() {
    if (!this.selectedTipoId) return;
    this.loadConfig();
    this.loadReg();
  }

  loadReg() {
    this.loading = true;
    this.service.list(this.selectedTipoId, this.pageIndex, this.pageSize).pipe(finalize(() => this.loading = false)).subscribe({
      next: data => {
        this.registros = data.content || [];
        this.totalElements = data.totalElements || 0;
        this.loadPessoas();
      },
      error: () => {
        this.registros = [];
        this.filtered = [];
        this.totalElements = 0;
      }
    });
  }

  loadPessoas() {
    const ids = Array.from(new Set(this.registros.map(r => r.pessoaId))).filter(id => !this.pessoas.has(id));
    if (ids.length === 0) {
      this.applySearch();
      return;
    }
    const calls = ids.map(id => this.pessoaService.get(id));
    forkJoin(calls).subscribe({
      next: pessoas => {
        pessoas.forEach(p => this.pessoas.set(p.id, p));
        this.applySearch();
      },
      error: () => this.applySearch()
    });
  }

  loadConfig() {
    const screenId = `entities-${this.selectedTipoId}`;
    const rolesKeycloak = (JSON.parse(localStorage.getItem('roles') || '[]') as string[]);
    const rolesTenant = (JSON.parse(localStorage.getItem('tenantRoles') || '[]') as string[]);
    const roles = Array.from(new Set([...rolesKeycloak, ...rolesTenant])).join(',');
    const userId = localStorage.getItem('userId') || '';

    this.config.getColunas(screenId, userId, roles).subscribe({
      next: cfg => this.applyConfig(cfg?.configJson || '{}')
    });
  }

  applyConfig(configJson: string) {
    try {
      const cfg = JSON.parse(configJson);
      const cols = cfg?.columns || ['pessoa', 'documento', 'alerta', 'ativo', 'acoes'];
      this.columns = cols.filter((c: string) => ['pessoa', 'documento', 'alerta', 'ativo', 'acoes'].includes(c));
    } catch {
    }
  }

  applyFilters() {
    this.pageIndex = 0;
    this.loadReg();
  }

  onSearchChange(value: FieldSearchValue) {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applySearch();
  }

  private applySearch() {
    const term = this.searchTerm.trim().toLowerCase();
    const status = this.filters.value.ativo || '';
    const comAlerta = !!this.filters.value.comAlerta;
    this.filtered = this.registros.filter(r => {
      if (status && (r.ativo ? 'ATIVO' : 'INATIVO') !== status) return false;
      if (comAlerta && !r.alerta) return false;
      if (!term) return true;
      const pessoa = this.pessoas.get(r.pessoaId);
      const nome = pessoa?.nome || '';
      const doc = pessoa?.cpf || pessoa?.cnpj || pessoa?.idEstrangeiro || '';
      const alerta = (r.alerta || '').toLowerCase();
      const matchNome = this.searchFields.includes('nome') && nome.toLowerCase().includes(term);
      const matchDoc = this.searchFields.includes('documento') && doc.toLowerCase().includes(term);
      const matchAlerta = this.searchFields.includes('alerta') && alerta.includes(term);
      return matchNome || matchDoc || matchAlerta;
    });
  }

  pageChange(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadReg();
  }

  personName(row: EntidadePapel): string {
    return this.pessoas.get(row.pessoaId)?.nome || '—';
  }

  personDocument(row: EntidadePapel): string {
    const pessoa = this.pessoas.get(row.pessoaId);
    return pessoa?.cpf || pessoa?.cnpj || pessoa?.idEstrangeiro || '—';
  }

  alertaPreview(row: EntidadePapel): string {
    if (!row.alerta) return '—';
    return row.alerta.length > 48 ? `${row.alerta.slice(0, 48)}…` : row.alerta;
  }

  newRegistro() {
    this.router.navigate(['/entities/new'], { queryParams: { tipoId: this.selectedTipoId } });
  }

  view(row: EntidadePapel) {
    this.router.navigate(['/entities', row.id]);
  }

  edit(row: EntidadePapel) {
    this.router.navigate(['/entities', row.id, 'edit']);
  }

  remove(row: EntidadePapel) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Excluir registro', message: `Deseja excluir o registro da pessoa "${this.personName(row)}"?` }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.service.delete(row.id).subscribe({
        next: () => {
          this.notify.success('Registro removido.');
          this.loadReg();
        },
        error: () => this.notify.error('Não foi possível remover o registro.')
      });
    });
  }
}


