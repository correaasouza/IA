import { CommonModule } from '@angular/common';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, ContentChild, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, finalize, of } from 'rxjs';
import { NotificationService } from '../../core/notifications/notification.service';
import { ConfigSectionShellComponent } from '../../shared/config-section-shell.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { CompanyService, EmpresaResponse } from '../companies/company.service';
import { AgrupadorEmpresa, AgrupadorEmpresaService } from './agrupador-empresa.service';

export interface AgrupadorAfterSaveContext {
  groupId: number;
  isCreate: boolean;
  nome: string;
  empresaIds: number[];
}

@Component({
  selector: 'app-agrupadores-empresa',
  standalone: true,
  imports: [
    CommonModule,
    DragDropModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    ConfigSectionShellComponent,
    FieldSearchComponent
  ],
  templateUrl: './agrupadores-empresa.component.html',
  styleUrl: './agrupadores-empresa.component.css'
})
export class AgrupadoresEmpresaComponent implements OnChanges {
  private static nextInstanceId = 1;
  @Input() configType = '';
  @Input() configId: number | null = null;
  @Input() configReferenceName = '';
  @Input() extraTabLabel = '';
  @Input() secondaryExtraTabLabel = '';
  @Input() pickerMode = false;
  @Input() pickerEmpresas: EmpresaResponse[] = [];
  @Input() pickerEmpresaIds: number[] = [];
  @Input() pickerDisabled = false;
  @Input() pickerDisabledEmpresaIds: number[] = [];
  @Input() pickerDisabledEmpresaLabel = 'Ja vinculada a outra configuracao.';
  @Input() pickerAvailableTitle = 'Empresas disponiveis';
  @Input() pickerSelectedTitle = 'Empresas selecionadas';
  @Input() pickerEmptyAvailableLabel = 'Nenhuma empresa disponivel.';
  @Input() pickerEmptySelectedLabel = 'Nenhuma empresa selecionada.';
  @Input() afterSave: ((context: AgrupadorAfterSaveContext) => Observable<unknown>) | null = null;
  @Output() changed = new EventEmitter<void>();
  @Output() groupEditStarted = new EventEmitter<AgrupadorEmpresa>();
  @Output() pickerEmpresaIdsChange = new EventEmitter<number[]>();
  @ViewChild('agrupadorFormDialog') formDialogTpl?: TemplateRef<unknown>;
  @ContentChild('agrupadorConfigTab', { read: TemplateRef }) configTabTpl?: TemplateRef<unknown>;
  @ContentChild('agrupadorExtraTab', { read: TemplateRef }) extraTabTpl?: TemplateRef<unknown>;
  @ContentChild('agrupadorSecondaryExtraTab', { read: TemplateRef }) secondaryExtraTabTpl?: TemplateRef<unknown>;

  loading = false;
  saving = false;
  loadingEmpresas = false;
  isMobile = false;
  mobileFiltersOpen = false;
  error = '';
  empresaSearch = '';
  empresaSearchOptions: FieldSearchOption[] = [{ key: 'razaoSocial', label: 'Nome' }];
  empresaSearchFields = ['razaoSocial'];
  editingGroupId: number | null = null;
  formNome = '';
  formEmpresaIds: number[] = [];
  availableEmpresasForFormList: EmpresaResponse[] = [];
  addedEmpresasForFormList: Array<{ id: number; label: string }> = [];
  selectedAvailableEmpresaIds: number[] = [];
  selectedAddedEmpresaIds: number[] = [];
  activeFormTab: 'FILIAIS' | 'CONFIGURACOES' | 'EXTRA' | 'EXTRA_SECONDARY' = 'FILIAIS';
  private dialogRef: MatDialogRef<unknown> | null = null;
  private originalNome = '';
  private originalEmpresaIds: number[] = [];
  private formEmpresaFallbackById: Record<number, string> = {};
  private pickerDisabledEmpresaIdSet = new Set<number>();
  private readonly instanceId = AgrupadoresEmpresaComponent.nextInstanceId++;
  readonly availableListId = `availableCompaniesList-${this.instanceId}`;
  readonly selectedListId = `selectedCompaniesList-${this.instanceId}`;

  agrupadores: AgrupadorEmpresa[] = [];
  empresas: EmpresaResponse[] = [];
  empresaPageSize = 100;
  empresaIdToGroup: Record<number, { groupId: number; groupName: string }> = {};

  constructor(
    private agrupadorService: AgrupadorEmpresaService,
    private companyService: CompanyService,
    private notify: NotificationService,
    private dialog: MatDialog) {
    this.updateViewportMode();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.pickerMode) {
      this.applyPickerInputs();
      return;
    }
    if (changes['configType'] || changes['configId']) {
      this.tryLoad();
    }
  }

  canRender(): boolean {
    return !!this.normalizedType() && !!this.configId && this.configId > 0;
  }

  hasConfigTab(): boolean {
    return !!this.configTabTpl;
  }

  hasExtraTab(): boolean {
    return !!this.extraTabTpl && !!(this.extraTabLabel || '').trim();
  }

  hasSecondaryExtraTab(): boolean {
    return !!this.secondaryExtraTabTpl && !!(this.secondaryExtraTabLabel || '').trim();
  }

  setFormTab(tab: 'FILIAIS' | 'CONFIGURACOES' | 'EXTRA' | 'EXTRA_SECONDARY'): void {
    if (tab === 'CONFIGURACOES' && !this.hasConfigTab()) return;
    if (tab === 'EXTRA' && !this.hasExtraTab()) return;
    if (tab === 'EXTRA_SECONDARY' && !this.hasSecondaryExtraTab()) return;
    this.activeFormTab = tab;
  }

  configTabContext(): Record<string, unknown> {
    return {
      group: this.currentGroupContext(),
      saving: this.saving,
      loading: this.loading
    };
  }

  extraTabContext(): Record<string, unknown> {
    return this.configTabContext();
  }

  startCreateForm(): void {
    this.resetFormState();
    this.activeFormTab = 'FILIAIS';
    this.openFormDialog();
  }

  startEditForm(group: AgrupadorEmpresa): void {
    this.applyGroupToForm(group, true);
    this.activeFormTab = 'FILIAIS';
    this.openFormDialog();
  }

  cancelForm(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
      this.dialogRef = null;
    }
    this.resetFormState();
  }

  private applyGroupToForm(group: AgrupadorEmpresa, emit = false): void {
    this.editingGroupId = group.id;
    this.formNome = group.nome;
    this.formEmpresaIds = (group.empresas || []).map(item => item.empresaId);
    this.formEmpresaFallbackById = {};
    for (const item of group.empresas || []) {
      this.formEmpresaFallbackById[item.empresaId] = item.nome;
    }
    this.selectedAvailableEmpresaIds = [];
    this.selectedAddedEmpresaIds = [];
    this.originalNome = group.nome;
    this.originalEmpresaIds = [...this.formEmpresaIds];
    this.rebuildFormEmpresaLists();
    if (emit) {
      this.groupEditStarted.emit({
        id: group.id,
        nome: group.nome,
        ativo: group.ativo,
        empresas: [...(group.empresas || [])]
      });
    }
  }

  private resetFormState(): void {
    this.editingGroupId = null;
    this.formNome = '';
    this.formEmpresaIds = [];
    this.formEmpresaFallbackById = {};
    this.selectedAvailableEmpresaIds = [];
    this.selectedAddedEmpresaIds = [];
    this.originalNome = '';
    this.originalEmpresaIds = [];
    this.availableEmpresasForFormList = [];
    this.addedEmpresasForFormList = [];
    this.activeFormTab = 'FILIAIS';
  }

  saveForm(): void {
    const nome = (this.formNome || '').trim();
    if (!nome || !this.canRender()) return;

    const selectedEmpresaIds = [...new Set((this.formEmpresaIds || []).map(id => Number(id)).filter(id => !!id))];
    this.saving = true;

    if (!this.editingGroupId) {
      this.agrupadorService.create(this.normalizedType(), this.configId!, nome).subscribe({
        next: created => {
          this.syncEmpresas(created.id, [], selectedEmpresaIds, () => {
            this.runAfterSaveHook({
              groupId: created.id,
              isCreate: true,
              nome,
              empresaIds: selectedEmpresaIds
            }, () => {
              this.notify.success('Configuracao criada.');
              this.saving = false;
              this.editingGroupId = created.id;
              this.originalNome = nome;
              this.originalEmpresaIds = [...selectedEmpresaIds];
              this.activeFormTab = this.hasConfigTab()
                ? 'CONFIGURACOES'
                : (this.hasExtraTab()
                  ? 'EXTRA'
                  : (this.hasSecondaryExtraTab() ? 'EXTRA_SECONDARY' : 'FILIAIS'));
              this.groupEditStarted.emit({
                id: created.id,
                nome,
                ativo: created?.ativo ?? true,
                empresas: this.addedEmpresasForForm().map(item => ({ empresaId: item.id, nome: item.label }))
              });
              this.loadAgrupadores();
              this.changed.emit();
            });
          });
        },
        error: err => {
          this.saving = false;
          this.notify.error(this.errorMessage(err, 'Nao foi possivel criar a configuracao.'));
        }
      });
      return;
    }

    const groupId = this.editingGroupId;
    const afterRename = () => {
      this.syncEmpresas(groupId, this.originalEmpresaIds, selectedEmpresaIds, () => {
        this.runAfterSaveHook({
          groupId,
          isCreate: false,
          nome,
          empresaIds: selectedEmpresaIds
        }, () => {
          this.notify.success('Configuracao atualizada.');
          this.saving = false;
          this.cancelForm();
          this.loadAgrupadores();
          this.changed.emit();
        });
      });
    };

    if (nome !== this.originalNome) {
      this.agrupadorService.rename(this.normalizedType(), this.configId!, groupId, nome).subscribe({
        next: () => afterRename(),
        error: err => {
          this.saving = false;
          this.notify.error(this.errorMessage(err, 'Nao foi possivel salvar a configuracao.'));
        }
      });
      return;
    }

    afterRename();
  }

  remove(group: AgrupadorEmpresa): void {
    if (!this.canRender()) return;
    this.saving = true;
    this.agrupadorService.remove(this.normalizedType(), this.configId!, group.id)
      .pipe(finalize(() => this.saving = false))
      .subscribe({
        next: () => {
          this.notify.success('Configuracao removida.');
          if (this.editingGroupId === group.id) {
            this.cancelForm();
          }
          this.loadAgrupadores();
          this.changed.emit();
        },
        error: err => this.notify.error(this.errorMessage(err, 'Nao foi possivel remover a configuracao.'))
      });
  }

  onEmpresaSearchChange(value: FieldSearchValue): void {
    this.empresaSearch = (value.term || '').trim();
    this.empresaSearchFields = value.fields.length ? value.fields : this.empresaSearchOptions.map(o => o.key);
    this.loadEmpresas();
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    return (this.empresaSearch || '').trim() ? 1 : 0;
  }

  isEmpresaDisabledForForm(empresaId: number): boolean {
    if (this.pickerMode) {
      return this.pickerDisabledEmpresaIdSet.has(Number(empresaId));
    }
    const target = this.empresaIdToGroup[empresaId];
    if (!target || target.groupId === this.editingGroupId) return false;
    return this.normalizedType() !== 'CATALOGO';
  }

  occupiedLabelForForm(empresaId: number): string {
    if (this.pickerMode) {
      return this.pickerDisabledEmpresaIdSet.has(Number(empresaId))
        ? this.pickerDisabledEmpresaLabel
        : '';
    }
    const target = this.empresaIdToGroup[empresaId];
    if (!target || target.groupId === this.editingGroupId) return '';
    if (this.normalizedType() === 'CATALOGO') {
      return `Atualmente em: ${target.groupName}. Ao salvar, sera movida.`;
    }
    return `Ja vinculada em: ${target.groupName}`;
  }

  availableEmpresasForForm(): EmpresaResponse[] {
    return this.availableEmpresasForFormList;
  }

  addedEmpresasForForm(): Array<{ id: number; label: string }> {
    return this.addedEmpresasForFormList;
  }

  empresaLabel(empresa: EmpresaResponse): string {
    const tipo = empresa.tipo === 'MATRIZ' ? 'Matriz' : 'Filial';
    return `${tipo} - ${empresa.razaoSocial}`;
  }

  isAvailableSelected(empresaId: number): boolean {
    return this.selectedAvailableEmpresaIds.includes(Number(empresaId));
  }

  isAddedSelected(empresaId: number): boolean {
    return this.selectedAddedEmpresaIds.includes(Number(empresaId));
  }

  toggleAvailableSelection(empresaId: number): void {
    const id = Number(empresaId);
    if (!id || this.isEmpresaDisabledForForm(id) || this.isSelectionLocked()) return;
    if (this.isAvailableSelected(id)) {
      this.selectedAvailableEmpresaIds = this.selectedAvailableEmpresaIds.filter(item => item !== id);
      return;
    }
    this.selectedAvailableEmpresaIds = [...this.selectedAvailableEmpresaIds, id];
  }

  toggleAddedSelection(empresaId: number): void {
    const id = Number(empresaId);
    if (!id || this.isSelectionLocked()) return;
    if (this.isAddedSelected(id)) {
      this.selectedAddedEmpresaIds = this.selectedAddedEmpresaIds.filter(item => item !== id);
      return;
    }
    this.selectedAddedEmpresaIds = [...this.selectedAddedEmpresaIds, id];
  }

  assignSelected(): void {
    if (this.isSelectionLocked()) return;
    const toAdd = this.selectedAvailableEmpresaIds
      .map(id => Number(id))
      .filter(id => id > 0)
      .filter(id => !this.isEmpresaDisabledForForm(id))
      .filter(id => !(this.formEmpresaIds || []).includes(id));
    if (!toAdd.length) return;
    this.formEmpresaIds = [...(this.formEmpresaIds || []), ...toAdd];
    this.selectedAvailableEmpresaIds = [];
    this.rebuildFormEmpresaLists();
    this.normalizeSelections();
    this.emitPickerEmpresaIdsIfNeeded();
  }

  unassignSelected(): void {
    if (this.isSelectionLocked()) return;
    const toRemove = new Set(this.selectedAddedEmpresaIds.map(id => Number(id)));
    if (!toRemove.size) return;
    this.formEmpresaIds = (this.formEmpresaIds || []).filter(id => !toRemove.has(Number(id)));
    this.selectedAddedEmpresaIds = [];
    this.rebuildFormEmpresaLists();
    this.normalizeSelections();
    this.emitPickerEmpresaIdsIfNeeded();
  }

  onDropToSelected(event: CdkDragDrop<Array<{ id: number; label: string }>>): void {
    if (this.isSelectionLocked()) return;
    if (event.previousContainer === event.container) return;
    const id = this.resolveDroppedEmpresaId(event);
    if (!id || this.isEmpresaDisabledForForm(id)) return;
    if (!(this.formEmpresaIds || []).includes(id)) {
      this.formEmpresaIds = [...(this.formEmpresaIds || []), id];
      this.rebuildFormEmpresaLists();
      this.normalizeSelections();
      this.emitPickerEmpresaIdsIfNeeded();
    }
  }

  onDropToAvailable(event: CdkDragDrop<EmpresaResponse[]>): void {
    if (this.isSelectionLocked()) return;
    if (event.previousContainer === event.container) return;
    const id = this.resolveDroppedEmpresaId(event);
    if (!id) return;
    this.formEmpresaIds = (this.formEmpresaIds || []).filter(item => Number(item) !== id);
    this.rebuildFormEmpresaLists();
    this.normalizeSelections();
    this.emitPickerEmpresaIdsIfNeeded();
  }

  refresh(): void {
    if (this.pickerMode) {
      this.applyPickerInputs();
      return;
    }
    this.tryLoad();
  }

  empresasCount(group: AgrupadorEmpresa): number {
    return (group.empresas || []).length;
  }

  editingModeLabel(): string {
    if (!this.editingGroupId) {
      return this.normalizedType() === 'CATALOGO'
        ? `Nova Configuracao por Empresa - ${this.catalogLabel()}`
        : 'Nova Configuracao';
    }
    if (this.normalizedType() === 'CATALOGO') {
      return `Configuracao do Catalogo por Empresa - ${this.catalogCompaniesTitleSuffix()}`;
    }
    return 'Ficha da configuracao';
  }

  showCreateFlowWarning(): boolean {
    return this.normalizedType() === 'CATALOGO' && !this.editingGroupId;
  }

  createFlowWarningMessage(): string {
    return `Fluxo recomendado (${this.catalogLabel()}): 1) informe o nome e salve a configuracao; 2) atribua empresas e ajuste as demais configuracoes.`;
  }

  isGroupBeingEdited(groupId: number): boolean {
    return this.editingGroupId === groupId;
  }

  configContextLabel(): string {
    const normalized = this.normalizedType();
    if (normalized === 'TIPO_ENTIDADE') return 'Tipo de entidade';
    if (normalized === 'FORMULARIO') return 'Configuracao de formulario';
    if (normalized === 'COLUNA') return 'Configuracao de coluna';
    return normalized || 'Configuracao';
  }

  configContextReference(): string {
    const id = this.configId || '-';
    const name = (this.configReferenceName || '').trim();
    if (name) {
      return `${this.configContextLabel()} #${id} - ${name}`;
    }
    return `${this.configContextLabel()} #${id}`;
  }

  private currentGroupContext(): AgrupadorEmpresa | null {
    if (!this.editingGroupId) return null;
    return {
      id: this.editingGroupId,
      nome: (this.formNome || '').trim(),
      ativo: true,
      empresas: this.addedEmpresasForForm().map(item => ({ empresaId: item.id, nome: item.label }))
    };
  }

  private tryLoad(): void {
    if (this.pickerMode) {
      this.applyPickerInputs();
      return;
    }
    if (!this.canRender()) {
      this.agrupadores = [];
      this.empresas = [];
      this.empresaIdToGroup = {};
      this.error = '';
      this.cancelForm();
      return;
    }
    this.loadEmpresas();
    this.loadAgrupadores();
  }

  private loadEmpresas(): void {
    if (this.loadingEmpresas) return;
    const page = 0;
    const nome = (this.empresaSearch || '').trim();
    this.loadingEmpresas = true;
    this.companyService.list({ page, size: this.empresaPageSize, nome: nome || undefined }).pipe(
      finalize(() => this.loadingEmpresas = false)
    ).subscribe({
      next: data => {
        const items = ((data?.content || []) as EmpresaResponse[]).slice();
        const dedup = new Map<number, EmpresaResponse>();
        for (const empresa of items) {
          dedup.set(empresa.id, empresa);
        }
        this.empresas = [...dedup.values()].sort((a, b) => {
          const an = `${a.tipo === 'MATRIZ' ? 'Matriz' : 'Filial'} - ${a.razaoSocial || ''}`.toLowerCase();
          const bn = `${b.tipo === 'MATRIZ' ? 'Matriz' : 'Filial'} - ${b.razaoSocial || ''}`.toLowerCase();
          return an.localeCompare(bn);
        });
        this.rebuildFormEmpresaLists();
      },
      error: () => this.notify.error('Nao foi possivel carregar as empresas.')
    });
  }

  private loadAgrupadores(): void {
    this.loading = true;
    this.error = '';
    this.agrupadorService.list(this.normalizedType(), this.configId!)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: data => {
          this.agrupadores = (data || []).map(item => ({ ...item, empresas: item.empresas || [] }));
          if (this.dialogRef && this.editingGroupId) {
            const edited = this.agrupadores.find(item => item.id === this.editingGroupId);
            if (edited) {
              this.applyGroupToForm(edited, true);
            } else {
              this.cancelForm();
            }
          }
          this.rebuildEmpresaLocks();
        },
        error: err => {
          this.agrupadores = [];
          this.empresaIdToGroup = {};
          this.error = this.errorMessage(err, 'Nao foi possivel carregar as configuracoes por empresa.');
        }
      });
  }

  private syncEmpresas(groupId: number, beforeIds: number[], afterIds: number[], onDone: () => void): void {
    const beforeSet = new Set(beforeIds.map(id => Number(id)));
    const afterSet = new Set(afterIds.map(id => Number(id)));
    const toAdd = [...afterSet].filter(id => !beforeSet.has(id));
    const toRemove = [...beforeSet].filter(id => !afterSet.has(id));

    const actions: Array<{ kind: 'add' | 'remove'; empresaId: number }> = [
      ...toRemove.map(empresaId => ({ kind: 'remove' as const, empresaId })),
      ...toAdd.map(empresaId => ({ kind: 'add' as const, empresaId }))
    ];

    const run = (index: number) => {
      if (index >= actions.length) {
        onDone();
        return;
      }
      const action = actions[index];
      if (!action) {
        onDone();
        return;
      }
      const request$ = action.kind === 'add'
        ? this.agrupadorService.addEmpresa(this.normalizedType(), this.configId!, groupId, action.empresaId)
        : this.agrupadorService.removeEmpresa(this.normalizedType(), this.configId!, groupId, action.empresaId);

      request$.subscribe({
        next: () => run(index + 1),
        error: err => {
          this.saving = false;
          this.notify.error(this.errorMessage(err, 'Nao foi possivel atualizar empresas do agrupador.'));
          this.loadAgrupadores();
        }
      });
    };

    run(0);
  }

  private rebuildEmpresaLocks(): void {
    const next: Record<number, { groupId: number; groupName: string }> = {};
    for (const group of this.agrupadores) {
      for (const empresa of group.empresas || []) {
        next[empresa.empresaId] = { groupId: group.id, groupName: group.nome };
      }
    }
    this.empresaIdToGroup = next;
  }

  private normalizedType(): string {
    return (this.configType || '').trim().toUpperCase();
  }

  private errorMessage(err: any, fallback: string): string {
    return err?.error?.detail || err?.error?.message || fallback;
  }

  private openFormDialog(): void {
    if (!this.formDialogTpl) return;
    if (this.dialogRef) {
      this.dialogRef.updateSize('960px');
      return;
    }
    const topOffset = '104px';
    this.dialogRef = this.dialog.open(this.formDialogTpl, {
      width: '960px',
      maxWidth: '95vw',
      maxHeight: 'calc(100dvh - 124px)',
      position: { top: topOffset },
      panelClass: 'agrupador-dialog-panel',
      autoFocus: false,
      restoreFocus: true
    });
    this.dialogRef.afterClosed().subscribe(() => {
      this.dialogRef = null;
      this.resetFormState();
    });
  }

  private normalizeSelections(): void {
    const availableIds = new Set(this.availableEmpresasForForm().map(item => Number(item.id)));
    const addedIds = new Set((this.formEmpresaIds || []).map(id => Number(id)));
    this.selectedAvailableEmpresaIds = this.selectedAvailableEmpresaIds.filter(id => availableIds.has(Number(id)));
    this.selectedAddedEmpresaIds = this.selectedAddedEmpresaIds.filter(id => addedIds.has(Number(id)));
  }

  private rebuildFormEmpresaLists(): void {
    const selected = new Set((this.formEmpresaIds || []).map(id => Number(id)));
    this.availableEmpresasForFormList = (this.empresas || []).filter(empresa => !selected.has(Number(empresa.id)));

    const empresaById = new Map<number, EmpresaResponse>((this.empresas || []).map(item => [Number(item.id), item]));
    this.addedEmpresasForFormList = (this.formEmpresaIds || []).map(id => {
      const empresaId = Number(id);
      const empresa = empresaById.get(empresaId);
      if (empresa) {
        return { id: empresaId, label: this.empresaLabel(empresa) };
      }
      const fallback = this.formEmpresaFallbackById[empresaId] || `Empresa #${empresaId}`;
      return { id: empresaId, label: fallback };
    });
  }

  private resolveDroppedEmpresaId(event: CdkDragDrop<any[]>): number {
    const direct = this.toEmpresaId(event.item?.data);
    if (direct) return direct;
    const sourceData = event.previousContainer?.data;
    if (Array.isArray(sourceData) && event.previousIndex >= 0 && event.previousIndex < sourceData.length) {
      return this.toEmpresaId(sourceData[event.previousIndex]);
    }
    return 0;
  }

  private toEmpresaId(raw: any): number {
    if (raw === null || raw === undefined) return 0;
    if (typeof raw === 'number') return Number.isFinite(raw) ? Number(raw) : 0;
    if (typeof raw === 'string') {
      const parsed = Number(raw);
      return Number.isFinite(parsed) ? parsed : 0;
    }
    if (typeof raw === 'object') {
      if ('id' in raw) {
        const parsed = Number((raw as { id: unknown }).id);
        if (Number.isFinite(parsed)) return parsed;
      }
      if ('empresaId' in raw) {
        const parsed = Number((raw as { empresaId: unknown }).empresaId);
        if (Number.isFinite(parsed)) return parsed;
      }
    }
    return 0;
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }

  private isSelectionLocked(): boolean {
    if (this.pickerMode) {
      return this.pickerDisabled;
    }
    return this.saving || this.loading;
  }

  private applyPickerInputs(): void {
    this.loading = false;
    this.loadingEmpresas = false;
    this.error = '';
    this.agrupadores = [];
    this.empresaIdToGroup = {};
    this.pickerDisabledEmpresaIdSet = new Set((this.pickerDisabledEmpresaIds || [])
      .map(id => Number(id))
      .filter(id => id > 0));
    const normalizedIds = [...new Set((this.pickerEmpresaIds || [])
      .map(id => Number(id))
      .filter(id => id > 0))];
    this.formEmpresaIds = normalizedIds;
    this.empresas = [...(this.pickerEmpresas || [])].sort((a, b) =>
      this.empresaLabel(a).toLowerCase().localeCompare(this.empresaLabel(b).toLowerCase()));
    this.rebuildFormEmpresaLists();
    this.normalizeSelections();
  }

  private emitPickerEmpresaIdsIfNeeded(): void {
    if (!this.pickerMode) {
      return;
    }
    const normalizedIds = [...new Set((this.formEmpresaIds || [])
      .map(id => Number(id))
      .filter(id => id > 0))];
    this.formEmpresaIds = normalizedIds;
    this.pickerEmpresaIdsChange.emit(normalizedIds);
  }

  private catalogCompaniesTitleSuffix(): string {
    const companies = this.addedEmpresasForForm()
      .map(item => this.companyNameOnly(item.label))
      .filter(label => !!label);
    if (!companies.length) {
      return '-';
    }
    return companies.join(', ');
  }

  private catalogLabel(): string {
    const ref = (this.configReferenceName || '').toLowerCase();
    if (ref.includes('servic')) return 'Servicos';
    return 'Produtos';
  }

  private companyNameOnly(label: string): string {
    const normalized = (label || '').trim();
    if (!normalized) {
      return '';
    }
    const marker = ' - ';
    const idx = normalized.indexOf(marker);
    if (idx < 0) {
      return normalized;
    }
    return normalized.substring(idx + marker.length).trim() || normalized;
  }

  private runAfterSaveHook(context: AgrupadorAfterSaveContext, onDone: () => void): void {
    const hook = this.afterSave;
    if (!hook) {
      onDone();
      return;
    }

    let request$ = of(null as unknown);
    try {
      request$ = hook(context) || of(null as unknown);
    } catch (err: any) {
      this.saving = false;
      this.notify.error(this.errorMessage(err, 'Nao foi possivel salvar configuracoes adicionais do agrupador.'));
      return;
    }

    request$.subscribe({
      next: () => onDone(),
      error: err => {
        this.saving = false;
        this.notify.error(this.errorMessage(err, 'Nao foi possivel salvar configuracoes adicionais do agrupador.'));
      }
    });
  }
}
