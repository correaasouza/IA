import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EntityTypeService, TipoEntidade } from '../entity-types/entity-type.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { EntityGroupService, GrupoEntidadeNode } from './entity-group.service';
import { EntityRecordService, RegistroEntidade, RegistroEntidadeContexto } from './entity-record.service';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { EntityTypeAccessService } from './entity-type-access.service';
import { EntityGroupsTreeComponent } from './entity-groups-tree.component';

@Component({
  selector: 'app-entity-records-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
    FieldSearchComponent,
    EntityGroupsTreeComponent
  ],
  templateUrl: './entity-records-list.component.html',
  styleUrls: ['./entity-records-list.component.css']
})
export class EntityRecordsListComponent implements OnInit {
  loading = false;
  loadingContext = false;
  tipos: TipoEntidade[] = [];
  selectedTipo: TipoEntidade | null = null;
  selectedGroupId: number | null = null;
  registros: RegistroEntidade[] = [];
  context: RegistroEntidadeContexto | null = null;
  contextWarning = '';
  displayedColumns = ['codigo', 'pessoa', 'registro', 'grupo', 'ativo', 'acoes'];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  gruposFiltro: Array<{ id: number; nome: string }> = [];
  dropTargetGroupId: number | null = null;
  dropZoneActive = false;
  dropZoneBusy = false;
  movementAreaOpen = false;
  isMobile = false;
  mobileFiltersOpen = false;
  groupTreeOpen = false;
  routeTipoEntidadeId = 0;
  routeTipoSeed = '';
  routeCustomOnly = false;

  searchOptions: FieldSearchOption[] = [
    { key: 'codigo', label: 'Codigo' },
    { key: 'pessoaNome', label: 'Pessoa' },
    { key: 'registroFederal', label: 'Registro federal' }
  ];
  searchTerm = '';
  searchFields = ['codigo', 'pessoaNome', 'registroFederal'];

  filters = this.fb.group({
    status: ['']
  });

  constructor(
      private fb: FormBuilder,
      private service: EntityRecordService,
      private groupsService: EntityGroupService,
      private typeService: EntityTypeService,
      private typeAccess: EntityTypeAccessService,
      private notify: NotificationService,
      private route: ActivatedRoute,
      private router: Router) {}

  ngOnInit(): void {
    this.updateViewportMode();
    this.route.queryParamMap.subscribe(params => {
      this.routeTipoEntidadeId = Number(params.get('tipoEntidadeId') || 0);
      this.routeTipoSeed = (params.get('tipoSeed') || '').trim().toUpperCase();
      const customOnlyValue = (params.get('customOnly') || '').trim().toLowerCase();
      this.routeCustomOnly = customOnlyValue === 'true' || customOnlyValue === '1';
      if (this.tipos.length) {
        this.applyRoutePresetIfPossible();
        this.pageIndex = 0;
        this.refreshContextAndData();
      }
    });
    this.loadTipos();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  @HostListener('window:empresa-context-updated')
  onEmpresaContextUpdated(): void {
    this.selectedGroupId = null;
    this.pageIndex = 0;
    this.refreshContextAndData();
  }

  loadTipos(): void {
    this.typeService.list({ page: 0, size: 200, ativo: true }).subscribe({
      next: data => {
        this.tipos = (data?.content || []).filter(item => item.ativo);
        this.applyRoutePresetIfPossible();
        this.refreshContextAndData();
      },
      error: () => this.notify.error('Nao foi possivel carregar tipos de entidade.')
    });
  }

  refreshContextAndData(): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (this.selectedTipo && !this.typeAccess.canAccessType(this.selectedTipo)) {
      this.context = null;
      this.contextWarning = 'Voce nao possui acesso ao tipo de entidade selecionado.';
      this.registros = [];
      this.totalElements = 0;
      this.gruposFiltro = [];
      return;
    }
    if (!tipoEntidadeId) {
      this.context = null;
      this.contextWarning = this.routeCustomOnly
        ? 'Nao existem tipos customizados ativos para este locatario.'
        : 'Nenhum tipo de entidade disponivel para o contexto atual.';
      this.registros = [];
      this.totalElements = 0;
      this.gruposFiltro = [];
      return;
    }
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      this.registros = [];
      this.totalElements = 0;
      this.gruposFiltro = [];
      return;
    }

    this.loadingContext = true;
    this.contextWarning = '';
    this.service.contextoEmpresa(tipoEntidadeId)
      .pipe(finalize(() => (this.loadingContext = false)))
      .subscribe({
        next: context => {
          this.context = context;
          if (!context.vinculado) {
            this.contextWarning = context.mensagem || 'Empresa sem grupo para o tipo selecionado.';
            this.registros = [];
            this.totalElements = 0;
            this.gruposFiltro = [];
            return;
          }
          this.contextWarning = '';
          this.loadGruposFiltro(tipoEntidadeId);
          this.loadRegistros();
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
          this.registros = [];
          this.totalElements = 0;
          this.gruposFiltro = [];
        }
      });
  }

  loadRegistros(): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId || !this.context?.vinculado) {
      this.registros = [];
      this.totalElements = 0;
      return;
    }
    this.loading = true;
    const status = this.filters.value.status || '';
    const term = this.searchTerm.trim();
    const searchCodigo = this.searchFields.includes('codigo') ? this.parseNumber(term) : null;
    const searchPessoa = this.searchFields.includes('pessoaNome') ? term : '';
    const searchRegistro = this.searchFields.includes('registroFederal') ? term : '';
    this.service.list(tipoEntidadeId, {
      page: this.pageIndex,
      size: this.pageSize,
      codigo: searchCodigo,
      pessoaNome: searchPessoa,
      registroFederal: searchRegistro,
      grupoId: this.selectedGroupId,
      ativo: status === '' ? '' : status === 'ativo'
    }).pipe(finalize(() => (this.loading = false))).subscribe({
      next: data => {
        this.registros = data.content || [];
        this.totalElements = this.extractTotalElements(data);
      },
      error: err => {
        this.registros = [];
        this.totalElements = 0;
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar entidades.');
      }
    });
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.loadRegistros();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchFields = ['codigo', 'pessoaNome', 'registroFederal'];
    this.filters.patchValue({ status: '' }, { emitEvent: false });
    this.applyFilters();
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(o => o.key);
    this.applyFilters();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
    if (this.isMobile && !this.mobileFiltersOpen) {
      this.groupTreeOpen = false;
    }
  }

  pageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRegistros();
  }

  goNew(): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId || !this.context?.vinculado) return;
    this.router.navigate(['/entities/new'], {
      queryParams: {
        tipoEntidadeId,
        grupoId: this.selectedGroupId || null
      }
    });
  }

  view(row: RegistroEntidade): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    this.router.navigate(['/entities', row.id], { queryParams: { tipoEntidadeId } });
  }

  edit(row: RegistroEntidade): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    this.router.navigate(['/entities', row.id, 'edit'], { queryParams: { tipoEntidadeId } });
  }

  onMoveGroup(row: RegistroEntidade, rawGroupId: string): void {
    const nextGroupId = this.parseNumber(rawGroupId);
    this.moveEntityToGroup(row.id, nextGroupId);
  }

  onStatusToggle(row: RegistroEntidade, nextAtivo: boolean): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId || !this.context?.vinculado) return;
    const previous = !!row.ativo;
    if (previous === nextAtivo) return;

    this.service.update(tipoEntidadeId, row.id, this.buildUpdatePayload(row, row.grupoEntidadeId || null, nextAtivo)).subscribe({
      next: updated => {
        row.ativo = !!updated.ativo;
        this.notify.success(`Entidade ${row.codigo} ${row.ativo ? 'ativada' : 'inativada'}.`);
        this.loadRegistros();
      },
      error: err => {
        row.ativo = previous;
        this.notify.error(err?.error?.detail || 'Nao foi possivel atualizar o status da entidade.');
      }
    });
  }

  onGroupDrop(event: { entityId: number; groupId: number | null }): void {
    this.moveEntityToGroup(event.entityId, event.groupId);
  }

  onEntityRowDragStart(event: DragEvent, row: RegistroEntidade): void {
    const payload = JSON.stringify({ entityId: row.id });
    event.dataTransfer?.setData('application/x-entity-row', payload);
    event.dataTransfer?.setData('text/plain', String(row.id));
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  remove(row: RegistroEntidade): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId) return;
    if (!confirm(`Excluir entidade codigo ${row.codigo}?`)) return;
    this.service.delete(tipoEntidadeId, row.id).subscribe({
      next: () => {
        this.notify.success('Entidade excluida.');
        this.loadRegistros();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir entidade.')
    });
  }

  onGroupSelected(groupId: number | null): void {
    this.selectedGroupId = groupId;
    this.pageIndex = 0;
    this.loadRegistros();
    if (this.isMobile) {
      this.groupTreeOpen = false;
      this.mobileFiltersOpen = false;
    }
  }

  toggleGroupTree(): void {
    this.groupTreeOpen = !this.groupTreeOpen;
  }

  showAllGroups(): void {
    this.onGroupSelected(null);
  }

  toggleMovementArea(): void {
    this.movementAreaOpen = !this.movementAreaOpen;
  }

  onDropTargetGroupChange(rawGroupId: string): void {
    this.dropTargetGroupId = this.parseNumber(rawGroupId);
  }

  dropTargetGroupLabel(): string {
    if (!this.dropTargetGroupId) return 'Nenhum';
    const target = this.gruposFiltro.find(item => item.id === this.dropTargetGroupId);
    return target?.nome || 'Nao encontrado';
  }

  onDestinationDragOver(event: DragEvent): void {
    if (this.isMobile || this.dropZoneBusy) return;
    event.preventDefault();
    this.dropZoneActive = true;
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDestinationDragLeave(): void {
    this.dropZoneActive = false;
  }

  onDestinationDrop(event: DragEvent): void {
    if (this.isMobile || this.dropZoneBusy) return;
    event.preventDefault();
    this.dropZoneActive = false;
    if (!this.context?.vinculado) return;
    const targetGroupId = this.dropTargetGroupId;
    if (!targetGroupId) {
      this.notify.info('Selecione um grupo de destino antes de soltar.');
      return;
    }

    const entityId = this.readEntityIdFromDrag(event);
    if (entityId) {
      this.moveEntityToGroup(entityId, targetGroupId);
      return;
    }

    const sourceGroupId = this.readGroupIdFromDrag(event);
    if (sourceGroupId) {
      if (sourceGroupId === targetGroupId) {
        this.notify.info('O grupo de origem ja e o grupo de destino.');
        return;
      }
      this.moveAllFromGroupToGroup(sourceGroupId, targetGroupId);
      return;
    }

    this.notify.info('Arraste uma entidade da lista ou um grupo da arvore.');
  }

  private moveEntityToGroup(entityId: number, groupId: number | null): void {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId || !this.context?.vinculado) return;
    const row = this.registros.find(item => item.id === entityId);
    if (!row) return;
    const currentGroupId = row.grupoEntidadeId || null;
    if ((groupId || null) === currentGroupId) return;

    const payload = this.buildUpdatePayload(row, groupId);

    this.service.update(tipoEntidadeId, row.id, payload).subscribe({
      next: updated => {
        row.grupoEntidadeId = updated.grupoEntidadeId || null;
        row.grupoEntidadeNome = updated.grupoEntidadeNome || null;
        this.refreshContextAndData();
        this.notify.success(`Entidade ${row.codigo} movida de grupo.`);
      },
      error: err => {
        this.notify.error(err?.error?.detail || 'Nao foi possivel mover a entidade de grupo.');
      }
    });
  }

  private async moveAllFromGroupToGroup(sourceGroupId: number, targetGroupId: number): Promise<void> {
    const tipoEntidadeId = this.selectedTipoEntidadeId();
    if (!tipoEntidadeId || !this.context?.vinculado) return;
    this.dropZoneBusy = true;
    try {
      const entities = await this.fetchAllEntitiesByGroup(tipoEntidadeId, sourceGroupId);
      if (entities.length === 0) {
        this.notify.info('O grupo de origem nao possui entidades para mover.');
        return;
      }

      let moved = 0;
      for (const entity of entities) {
        const currentGroupId = entity.grupoEntidadeId || null;
        if (currentGroupId === targetGroupId) continue;
        await firstValueFrom(this.service.update(tipoEntidadeId, entity.id, this.buildUpdatePayload(entity, targetGroupId)));
        moved++;
      }

      if (moved === 0) {
        this.notify.info('Nenhuma entidade precisou ser movida.');
        return;
      }
      this.notify.success(`${moved} entidade(s) movida(s) para ${this.dropTargetGroupLabel()}.`);
      this.refreshContextAndData();
    } catch (err: any) {
      this.notify.error(err?.error?.detail || 'Nao foi possivel mover entidades do grupo selecionado.');
    } finally {
      this.dropZoneBusy = false;
    }
  }

  private loadGruposFiltro(tipoEntidadeId: number): void {
    this.groupsService.tree(tipoEntidadeId).subscribe({
      next: tree => this.gruposFiltro = this.flatten(tree),
      error: () => this.gruposFiltro = []
    });
  }

  private flatten(nodes: GrupoEntidadeNode[], acc: Array<{ id: number; nome: string }> = [], prefix = ''): Array<{ id: number; nome: string }> {
    for (const node of nodes || []) {
      const label = prefix ? `${prefix} / ${node.nome}` : node.nome;
      acc.push({ id: node.id, nome: label });
      this.flatten(node.children || [], acc, label);
    }
    return acc;
  }

  private parseNumber(value: unknown): number | null {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }

  private buildUpdatePayload(entity: RegistroEntidade, groupId: number | null, ativo: boolean = entity.ativo): {
    grupoEntidadeId: number | null;
    ativo: boolean;
    pessoa: {
      nome: string;
      apelido?: string | undefined;
      tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
      registroFederal: string;
    };
  } {
    return {
      grupoEntidadeId: groupId,
      ativo: !!ativo,
      pessoa: {
        nome: entity.pessoa.nome,
        apelido: entity.pessoa.apelido || undefined,
        tipoRegistro: entity.pessoa.tipoRegistro,
        registroFederal: entity.pessoa.registroFederal
      }
    };
  }

  private async fetchAllEntitiesByGroup(tipoEntidadeId: number, groupId: number): Promise<RegistroEntidade[]> {
    const size = 200;
    let page = 0;
    const all: RegistroEntidade[] = [];
    while (true) {
      const response = await firstValueFrom(this.service.list(tipoEntidadeId, {
        page,
        size,
        grupoId: groupId,
        ativo: ''
      }));
      const items = response?.content || [];
      all.push(...items);
      if (items.length < size) break;
      page++;
    }
    return all;
  }

  private readEntityIdFromDrag(event: DragEvent): number | null {
    const rawEntity = event.dataTransfer?.getData('application/x-entity-row') || '';
    if (rawEntity) {
      try {
        const parsed = JSON.parse(rawEntity);
        const id = Number(parsed?.entityId || 0);
        return Number.isFinite(id) && id > 0 ? id : null;
      } catch {
        return null;
      }
    }
    const rawText = event.dataTransfer?.getData('text/plain') || '';
    if (rawText.startsWith('group:')) return null;
    try {
      const parsed = JSON.parse(rawText);
      const id = Number(parsed?.entityId || 0);
      if (Number.isFinite(id) && id > 0) return id;
    } catch {
      const id = Number(rawText);
      if (Number.isFinite(id) && id > 0) return id;
    }
    return null;
  }

  private readGroupIdFromDrag(event: DragEvent): number | null {
    const rawGroup = event.dataTransfer?.getData('application/x-entity-group') || '';
    if (rawGroup) {
      try {
        const parsed = JSON.parse(rawGroup);
        const id = Number(parsed?.groupId || 0);
        return Number.isFinite(id) && id > 0 ? id : null;
      } catch {
        return null;
      }
    }
    const rawText = event.dataTransfer?.getData('text/plain') || '';
    if (!rawText.startsWith('group:')) return null;
    const id = Number(rawText.replace('group:', '').trim());
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  selectedTipoEntidadeId(): number {
    return Number(this.selectedTipo?.id || 0);
  }

  selectedTipoNome(): string {
    return this.formatTipoNome(this.selectedTipo?.nome || '');
  }

  headerTitle(): string {
    const nome = this.selectedTipoNome();
    return nome ? `Cadastro ${nome}` : 'Cadastro de Entidades';
  }

  newEntityButtonLabel(): string {
    const nome = this.selectedTipoNome().trim();
    if (!nome) return 'Nova entidade';
    const nomeLower = nome.toLowerCase();
    const artigo = this.isFeminineTypeName(nomeLower) ? 'Nova' : 'Novo';
    return `${artigo} ${nomeLower}`;
  }

  selectedGroupHeader(): string {
    if (!this.selectedGroupId) return 'Todos';
    const selected = this.gruposFiltro.find(item => item.id === this.selectedGroupId);
    return selected?.nome || 'Todos';
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) count++;
    if ((this.filters.value.status || '').toString().trim()) count++;
    return count;
  }

  private extractTotalElements(payload: any): number {
    if (typeof payload?.totalElements === 'number') return payload.totalElements;
    if (typeof payload?.page?.totalElements === 'number') return payload.page.totalElements;
    return this.registros.length;
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }

  tipoOptions(): TipoEntidade[] {
    const active = (this.tipos || []).filter(item => item.ativo);
    const scoped = this.routeCustomOnly ? active.filter(item => !item.tipoPadrao) : active;
    return scoped.filter(item => this.typeAccess.canAccessType(item));
  }

  private applyRoutePresetIfPossible(): void {
    if (!this.tipos.length) return;
    const options = this.tipoOptions();
    if (options.length === 0) {
      this.selectedTipo = null;
      this.context = null;
      this.contextWarning = this.routeCustomOnly
        ? 'Nao existem tipos de entidade customizados ativos para este locatario.'
        : 'Nenhum tipo de entidade disponivel.';
      this.registros = [];
      this.totalElements = 0;
      return;
    }

    if (this.routeCustomOnly && !this.routeTipoEntidadeId && !this.routeTipoSeed) {
      this.selectedTipo = null;
      this.context = null;
      this.contextWarning = 'Selecione um tipo em "Outras entidades" no menu.';
      this.registros = [];
      this.totalElements = 0;
      return;
    }

    if (this.routeTipoEntidadeId > 0) {
      const byId = (this.tipos || []).find(item => item.id === this.routeTipoEntidadeId);
      if (byId && this.typeAccess.canAccessType(byId)) {
        this.selectedTipo = byId;
        return;
      }
      if (byId && !this.typeAccess.canAccessType(byId)) {
        this.selectedTipo = null;
        this.contextWarning = 'Voce nao possui acesso ao tipo de entidade selecionado.';
        return;
      }
    }

    if (this.routeTipoSeed) {
      const bySeed = (this.tipos || []).find(item => (item.codigoSeed || '').trim().toUpperCase() === this.routeTipoSeed);
      if (bySeed) {
        if (!this.typeAccess.canAccessType(bySeed)) {
          this.selectedTipo = null;
          this.contextWarning = 'Voce nao possui acesso ao tipo de entidade selecionado.';
          return;
        }
        this.selectedTipo = bySeed;
        return;
      }
    }

    const current = Number(this.selectedTipo?.id || 0);
    const currentExists = options.some(item => item.id === current);
    if (currentExists) return;
    this.selectedTipo = options[0] || null;
  }

  private formatTipoNome(value: string): string {
    const raw = (value || '').trim();
    if (!raw) return '';
    if (raw === raw.toUpperCase()) {
      return raw
        .toLowerCase()
        .split(' ')
        .filter(part => !!part)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
    }
    return raw;
  }

  private isFeminineTypeName(value: string): boolean {
    const normalized = (value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
    if (!normalized) return false;
    const token = normalized.split(' ').filter(part => !!part).pop() || normalized;
    return token.endsWith('a')
      || token.endsWith('cao')
      || token.endsWith('sao')
      || token.endsWith('dade')
      || token.endsWith('gem');
  }
}
