import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';
import { FeatureFlagService } from '../../core/features/feature-flag.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { FieldSearchComponent, FieldSearchOption, FieldSearchValue } from '../../shared/field-search/field-search.component';
import { CompanyService, EmpresaResponse } from '../companies/company.service';
import { AgrupadoresEmpresaComponent } from '../configs/agrupadores-empresa.component';
import { EntityTypeService, TipoEntidade } from '../entity-types/entity-type.service';
import {
  MovimentoConfig,
  MovimentoConfigCoverageWarning,
  MovimentoConfigRequest,
  MovimentoTipoOption,
  MovementConfigService
} from './movement-config.service';

@Component({
  selector: 'app-movement-configs-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
    FieldSearchComponent,
    AgrupadoresEmpresaComponent,
    AccessControlDirective
  ],
  templateUrl: './movement-configs-page.component.html',
  styleUrl: './movement-configs-page.component.css'
})
export class MovementConfigsPageComponent implements OnInit {
  @ViewChild('configFormDialog') configFormDialog?: TemplateRef<unknown>;

  movementTypes: MovimentoTipoOption[] = [];
  activeType = '';
  loadingTypes = false;
  loadingList = false;
  saving = false;
  loadingCompanies = false;
  loadingEntityTypes = false;
  loadingCoverageWarnings = false;
  errorMessage = '';
  featureEnabled = true;
  coverageWarnings: MovimentoConfigCoverageWarning[] = [];

  configs: MovimentoConfig[] = [];
  filteredConfigs: MovimentoConfig[] = [];
  selectedConfigId: number | null = null;
  displayedColumns = ['nome', 'ativo', 'empresas', 'tipos', 'acoes'];

  searchTerm = '';
  searchOptions: FieldSearchOption[] = [
    { key: 'nome', label: 'Nome' },
    { key: 'empresas', label: 'Empresas' },
    { key: 'destinatario', label: 'Destinatario do Movimento' }
  ];
  searchFields = ['nome', 'empresas', 'destinatario'];
  statusFilter: 'ALL' | 'ATIVO' | 'INATIVO' = 'ALL';
  isMobile = false;
  mobileFiltersOpen = false;

  companies: EmpresaResponse[] = [];
  companyById = new Map<number, EmpresaResponse>();

  entityTypes: TipoEntidade[] = [];
  entityTypeById = new Map<number, TipoEntidade>();

  readOnlyMode = false;
  editingConfigId: number | null = null;
  activeFormTab: 'CONFIGURACAO' | 'FILIAIS' = 'FILIAIS';
  private dialogRef: MatDialogRef<unknown> | null = null;

  readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    ativo: [true],
    empresaIds: [<number[]>[], [arrayRequiredValidator]],
    tiposEntidadePermitidos: [<number[]>[]],
    tipoEntidadePadraoId: [null as number | null]
  });

  constructor(
    private fb: FormBuilder,
    private movementConfigService: MovementConfigService,
    private companyService: CompanyService,
    private entityTypeService: EntityTypeService,
    private featureFlagService: FeatureFlagService,
    private dialog: MatDialog,
    private notify: NotificationService
  ) {
    this.updateViewportMode();
  }

  ngOnInit(): void {
    this.featureEnabled = this.featureFlagService.isEnabled('movementConfigEnabled', true);
    if (!this.featureEnabled) {
      this.errorMessage = 'Modulo de Configuracoes de Movimentos desabilitado para este ambiente.';
      return;
    }
    this.bindFormRules();
    this.loadCompanies();
    this.loadEntityTypes();
    this.loadTypesAndConfigs();
    this.loadCoverageWarnings();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.updateViewportMode();
  }

  setActiveType(tipo: string): void {
    if (!tipo || this.activeType === tipo) {
      return;
    }
    this.activeType = tipo;
    this.selectedConfigId = null;
    this.loadConfigs();
  }

  isTypeActive(tipo: string): boolean {
    return this.activeType === tipo;
  }

  toggleMobileFilters(): void {
    this.mobileFiltersOpen = !this.mobileFiltersOpen;
  }

  activeFiltersCount(): number {
    let count = 0;
    if ((this.searchTerm || '').trim()) {
      count += 1;
    }
    if (this.statusFilter !== 'ALL') {
      count += 1;
    }
    return count;
  }

  applyFilters(): void {
    const term = (this.searchTerm || '').trim().toLowerCase();
    const status = this.statusFilter;
    this.filteredConfigs = (this.configs || []).filter(config => {
      const statusMatch = status === 'ALL'
        || (status === 'ATIVO' && config.ativo)
        || (status === 'INATIVO' && !config.ativo);
      if (!statusMatch) {
        return false;
      }
      if (!term) {
        return true;
      }
      const byField: Record<string, string> = {
        nome: config.nome || '',
        empresas: this.empresaNames(config).join(' '),
        destinatario: this.tipoEntidadeNames(config).join(' ')
      };
      const fields = this.searchFields.length ? this.searchFields : this.searchOptions.map(item => item.key);
      return fields.some(field => (byField[field] || '').toLowerCase().includes(term));
    });
  }

  onSearchChange(value: FieldSearchValue): void {
    this.searchTerm = value.term;
    this.searchFields = value.fields.length ? value.fields : this.searchOptions.map(option => option.key);
    this.applyFilters();
  }

  openCreate(): void {
    if (!this.activeType) {
      return;
    }
    this.readOnlyMode = false;
    this.editingConfigId = null;
    this.activeFormTab = 'FILIAIS';
    this.form.reset({
      nome: '',
      ativo: true,
      empresaIds: [],
      tiposEntidadePermitidos: [],
      tipoEntidadePadraoId: null
    });
    this.form.enable({ emitEvent: false });
    this.openDialog();
  }

  openView(config: MovimentoConfig): void {
    this.openEdit(config, true);
  }

  openEdit(config: MovimentoConfig, readOnly = false): void {
    this.readOnlyMode = readOnly;
    this.editingConfigId = config.id;
    this.selectedConfigId = config.id;
    this.activeFormTab = 'FILIAIS';
    this.form.reset({
      nome: config.nome,
      ativo: config.ativo,
      empresaIds: [...(config.empresaIds || [])],
      tiposEntidadePermitidos: [...(config.tiposEntidadePermitidos || [])],
      tipoEntidadePadraoId: config.tipoEntidadePadraoId
    });
    if (this.readOnlyMode) {
      this.form.disable({ emitEvent: false });
    } else {
      this.form.enable({ emitEvent: false });
    }
    this.openDialog();
  }

  save(): void {
    if (this.readOnlyMode || !this.activeType) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notify.error('Revise os campos obrigatorios antes de salvar.');
      return;
    }

    const payload = this.buildPayload();
    if (!payload) {
      return;
    }

    this.saving = true;
    const request$ = this.editingConfigId
      ? this.movementConfigService.update(this.editingConfigId, payload)
      : this.movementConfigService.create(payload);

      request$.pipe(finalize(() => this.saving = false)).subscribe({
      next: () => {
        this.notify.success(this.editingConfigId ? 'Configuracao atualizada.' : 'Configuracao criada.');
        this.closeDialog();
        this.loadConfigs();
        this.loadCoverageWarnings();
      },
      error: (err) => this.notify.error(this.errorFrom(err, 'Nao foi possivel salvar a configuracao.'))
    });
  }

  duplicate(config: MovimentoConfig): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Duplicar configuracao',
        message: `Deseja duplicar a configuracao "${config.nome}"?`,
        confirmText: 'Duplicar'
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) {
        return;
      }
      this.saving = true;
      this.movementConfigService.duplicate(config.id, {
        nome: `${config.nome} - Copia`,
        ativo: false
      }).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.notify.success('Configuracao duplicada como inativa.');
          this.loadConfigs();
        },
        error: (err) => this.notify.error(this.errorFrom(err, 'Nao foi possivel duplicar a configuracao.'))
      });
    });
  }

  delete(config: MovimentoConfig): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Inativar configuracao',
        message: `Deseja inativar a configuracao "${config.nome}"?`,
        confirmText: 'Inativar',
        confirmColor: 'warn'
      }
    });
    ref.afterClosed().subscribe(result => {
      if (!result) {
        return;
      }
      this.saving = true;
      this.movementConfigService.delete(config.id).pipe(finalize(() => this.saving = false)).subscribe({
        next: () => {
          this.notify.success('Configuracao inativada.');
          if (this.selectedConfigId === config.id) {
            this.selectedConfigId = null;
          }
          this.loadConfigs();
          this.loadCoverageWarnings();
        },
        error: (err) => this.notify.error(this.errorFrom(err, 'Nao foi possivel inativar a configuracao.'))
      });
    });
  }

  closeDialog(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
    this.dialogRef = null;
    this.readOnlyMode = false;
    this.editingConfigId = null;
    this.activeFormTab = 'FILIAIS';
    this.form.enable({ emitEvent: false });
  }

  setFormTab(tab: 'CONFIGURACAO' | 'FILIAIS'): void {
    this.activeFormTab = tab;
  }

  onPickerEmpresaIdsChange(ids: number[]): void {
    const blocked = new Set(this.blockedEmpresaIdsForForm());
    const next = uniquePositiveIds(ids || []).filter(id => !blocked.has(id));
    this.form.patchValue({ empresaIds: next });
  }

  assignedEmpresaIds(): number[] {
    const value = this.form.get('empresaIds')?.value;
    if (!Array.isArray(value)) {
      return [];
    }
    return [...new Set(value.map(item => Number(item)).filter(item => item > 0))];
  }

  empresaLabel(empresa: EmpresaResponse): string {
    const tipo = empresa.tipo === 'MATRIZ' ? 'Matriz' : 'Filial';
    return `${tipo} - ${empresa.razaoSocial}`;
  }

  tipoEntidadeLabel(id: number): string {
    const tipo = this.entityTypeById.get(id);
    return tipo ? tipo.nome : `Tipo #${id}`;
  }

  empresaNames(config: MovimentoConfig): string[] {
    return (config.empresaIds || []).map(id => this.empresaLabelFromId(id));
  }

  tipoEntidadeNames(config: MovimentoConfig): string[] {
    return (config.tiposEntidadePermitidos || []).map(id => this.tipoEntidadeLabel(id));
  }

  selectedCard(config: MovimentoConfig): boolean {
    return this.selectedConfigId === config.id;
  }

  flowSummary(): string {
    return 'Fluxo: 1) selecione o tipo de movimento; 2) crie/edite a configuracao; 3) use a aba de empresas para atribuir quem usa essa regra. Sempre que faltar configuracao aplicavel, o sistema avisa e bloqueia o uso do movimento.';
  }

  coverageWarningsForActiveType(): MovimentoConfigCoverageWarning[] {
    if (!this.activeType) {
      return this.coverageWarnings;
    }
    return this.coverageWarnings.filter(item => item.tipoMovimento === this.activeType);
  }

  blockedEmpresaIdsForForm(): number[] {
    const blocked = new Set<number>();
    for (const config of this.configs || []) {
      if (!config?.ativo) {
        continue;
      }
      if (this.editingConfigId && config.id === this.editingConfigId) {
        continue;
      }
      for (const empresaId of (config.empresaIds || [])) {
        const normalizedId = Number(empresaId);
        if (normalizedId > 0) {
          blocked.add(normalizedId);
        }
      }
    }
    return [...blocked];
  }

  blockedEmpresasCountForForm(): number {
    return this.blockedEmpresaIdsForForm().length;
  }

  private bindFormRules(): void {
    this.form.get('tiposEntidadePermitidos')?.valueChanges.subscribe(value => {
      const allowed = new Set((value || []).map(item => Number(item)));
      const currentDefault = Number(this.form.get('tipoEntidadePadraoId')?.value || 0);
      if (currentDefault > 0 && !allowed.has(currentDefault)) {
        this.form.patchValue({ tipoEntidadePadraoId: null });
      }
    });
  }

  private loadTypesAndConfigs(): void {
    if (!this.featureEnabled) {
      return;
    }
    this.loadingTypes = true;
    this.movementConfigService.listTipos().pipe(finalize(() => this.loadingTypes = false)).subscribe({
      next: data => {
        this.movementTypes = (data || []).slice();
        if (!this.activeType && this.movementTypes.length) {
          this.activeType = this.movementTypes[0]?.codigo || '';
        }
        this.loadConfigs();
      },
      error: (err) => {
        this.movementTypes = [];
        this.activeType = '';
        this.errorMessage = this.errorFrom(err, 'Nao foi possivel carregar os tipos de movimento.');
      }
    });
  }

  private loadCoverageWarnings(): void {
    if (!this.featureEnabled) {
      this.coverageWarnings = [];
      return;
    }
    this.loadingCoverageWarnings = true;
    this.movementConfigService.listCoverageWarnings()
      .pipe(finalize(() => this.loadingCoverageWarnings = false))
      .subscribe({
        next: data => {
          this.coverageWarnings = (data || []).slice();
        },
        error: () => {
          this.coverageWarnings = [];
        }
      });
  }

  private loadConfigs(): void {
    if (!this.featureEnabled) {
      this.configs = [];
      this.filteredConfigs = [];
      return;
    }
    if (!this.activeType) {
      this.configs = [];
      this.filteredConfigs = [];
      return;
    }
    this.loadingList = true;
    this.errorMessage = '';
    this.movementConfigService.listByTipo(this.activeType, 0, 500)
      .pipe(finalize(() => this.loadingList = false))
      .subscribe({
        next: page => {
          this.configs = (page?.content || []).slice();
          if (this.selectedConfigId && !this.configs.some(item => item.id === this.selectedConfigId)) {
            this.selectedConfigId = null;
          }
          this.applyFilters();
        },
        error: (err) => {
          this.configs = [];
          this.filteredConfigs = [];
          this.errorMessage = this.errorFrom(err, 'Nao foi possivel carregar as configuracoes.');
        }
      });
  }

  private loadCompanies(): void {
    this.loadingCompanies = true;
    const size = 200;
    const all: EmpresaResponse[] = [];
    const fetchPage = (page: number) => {
      this.companyService.list({ page, size, ativo: true, sort: 'razaoSocial,asc' }).subscribe({
        next: response => {
          const content = Array.isArray(response?.content) ? (response.content as EmpresaResponse[]) : [];
          all.push(...content);
          const totalPages = Number(response?.totalPages || 1);
          if (page + 1 < totalPages) {
            fetchPage(page + 1);
            return;
          }
          this.loadingCompanies = false;
          const dedup = new Map<number, EmpresaResponse>();
          for (const empresa of all) {
            dedup.set(empresa.id, empresa);
          }
          this.companies = [...dedup.values()].sort((a, b) =>
            this.empresaLabel(a).toLowerCase().localeCompare(this.empresaLabel(b).toLowerCase()));
          this.companyById = new Map<number, EmpresaResponse>(this.companies.map(item => [item.id, item]));
        },
        error: () => {
          this.loadingCompanies = false;
          this.notify.error('Nao foi possivel carregar a lista de empresas.');
        }
      });
    };
    fetchPage(0);
  }

  private loadEntityTypes(): void {
    this.loadingEntityTypes = true;
    const size = 200;
    const all: TipoEntidade[] = [];
    const fetchPage = (page: number) => {
      this.entityTypeService.list({ page, size, ativo: true, sort: 'nome,asc' }).subscribe({
        next: response => {
          const content = Array.isArray(response?.content) ? response.content : [];
          all.push(...content);
          if (content.length >= size) {
            fetchPage(page + 1);
            return;
          }
          this.loadingEntityTypes = false;
          const dedup = new Map<number, TipoEntidade>();
          for (const tipo of all) {
            dedup.set(tipo.id, tipo);
          }
          this.entityTypes = [...dedup.values()].sort((a, b) => a.nome.toLowerCase().localeCompare(b.nome.toLowerCase()));
          this.entityTypeById = new Map<number, TipoEntidade>(this.entityTypes.map(item => [item.id, item]));
        },
        error: () => {
          this.loadingEntityTypes = false;
          this.notify.error('Nao foi possivel carregar os tipos de entidade.');
        }
      });
    };
    fetchPage(0);
  }

  private buildPayload(): MovimentoConfigRequest | null {
    const raw = this.form.getRawValue();
    const empresaIds = uniquePositiveIds(raw.empresaIds || []);
    const tiposEntidadePermitidos = uniquePositiveIds(raw.tiposEntidadePermitidos || []);
    const tipoEntidadePadraoId = raw.tipoEntidadePadraoId == null
      ? null
      : Number(raw.tipoEntidadePadraoId);
    const blocked = new Set(this.blockedEmpresaIdsForForm());
    const conflicting = empresaIds.filter(id => blocked.has(id));

    if (conflicting.length > 0) {
      this.notify.error('Uma ou mais empresas ja estao vinculadas em outra configuracao ativa para o mesmo tipo de movimento.');
      return null;
    }

    if (tipoEntidadePadraoId != null && !tiposEntidadePermitidos.includes(tipoEntidadePadraoId)) {
      this.notify.error('Destinatario do movimento padrao deve estar na lista de permitidos.');
      return null;
    }

    return {
      tipoMovimento: this.activeType,
      nome: (raw.nome || '').trim(),
      ativo: !!raw.ativo,
      empresaIds,
      tiposEntidadePermitidos,
      tipoEntidadePadraoId
    };
  }

  private openDialog(): void {
    if (!this.configFormDialog) {
      return;
    }
    if (this.dialogRef) {
      this.dialogRef.updateSize('1100px');
      return;
    }
    this.dialogRef = this.dialog.open(this.configFormDialog, {
      width: '1100px',
      maxWidth: '96vw',
      maxHeight: 'calc(100dvh - 120px)',
      panelClass: 'movimento-config-dialog-panel',
      autoFocus: false,
      restoreFocus: true,
      position: { top: '88px' }
    });
    this.dialogRef.afterClosed().subscribe(() => {
      this.dialogRef = null;
      this.form.enable({ emitEvent: false });
      this.readOnlyMode = false;
      this.editingConfigId = null;
      this.activeFormTab = 'FILIAIS';
    });
  }

  private updateViewportMode(): void {
    this.isMobile = typeof window !== 'undefined' ? window.innerWidth < 900 : false;
    if (!this.isMobile) {
      this.mobileFiltersOpen = false;
    }
  }

  private errorFrom(err: any, fallback: string): string {
    return err?.error?.detail || err?.error?.message || fallback;
  }

  private empresaLabelFromId(id: number): string {
    const empresa = this.companyById.get(id);
    return empresa ? this.empresaLabel(empresa) : `Empresa #${id}`;
  }
}

function uniquePositiveIds(value: number[]): number[] {
  return [...new Set((value || []).map(item => Number(item)).filter(item => item > 0))];
}

function arrayRequiredValidator(control: AbstractControl): { required: true } | null {
  const value = control.value;
  if (!Array.isArray(value)) {
    return { required: true };
  }
  return value.length > 0 ? null : { required: true };
}
