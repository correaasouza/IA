import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TextFieldModule } from '@angular/cdk/text-field';
import { Observable, forkJoin, of } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { catchError, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlService } from '../../core/access/access-control.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toDisplayDate, toIsoDate } from '../../shared/date-utils';
import { EntityTypeService } from '../entity-types/entity-type.service';
import { EntityTypeAccessService } from './entity-type-access.service';
import { CatalogPricingService, PriceBook } from '../catalog/catalog-pricing.service';
import { normalizeCpfCnpj } from '../../shared/cpf-cnpj.validator';
import {
  EntityRecordService,
  CepLookupResponse,
  PessoaLookupResponse,
  RegistroEntidade,
  RegistroEntidadeContexto,
  RegistroEntidadePayload,
  EntidadeDocumentacaoPayload,
  EntidadeInfoComercialPayload,
  EntidadeDadosFiscaisPayload,
  EntidadeContratoRhPayload,
  EntidadeInfoRhPayload,
  EntidadeRhOptions,
  EntidadeReferencia,
  EntidadeQualificacaoItem,
  EntidadeEndereco,
  EntidadeEnderecoPayload,
  EntidadeContato,
  EntidadeContatoForma,
  EntidadeContatoFormaPayload,
  EntidadeFamiliar,
  EntidadeFamiliarPayload,
  EntidadeFormConfigByGroup,
  EntidadeFormFieldConfig,
  EntidadeFormGroupConfig
} from './entity-record.service';

interface EntityRecordQuickNavItem {
  sectionId: string;
  label: string;
  icon: string;
}

interface EntidadeContatoFormaFlat extends EntidadeContatoForma {
  contatoNome?: string | null;
  contatoCargo?: string | null;
}

@Component({
  selector: 'app-entity-record-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatSlideToggleModule,
    TextFieldModule,
    DateMaskDirective,
    InlineLoaderComponent
  ],
  templateUrl: './entity-record-form.component.html',
  styleUrls: ['./entity-record-form.component.css']
})
export class EntityRecordFormComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly quickNavItems: EntityRecordQuickNavItem[] = [
    { sectionId: 'sec-topo', label: 'Topo da ficha', icon: 'vertical_align_top' },
    { sectionId: 'sec-dados-entidade', label: 'Dados da entidade', icon: 'account_box' },
    { sectionId: 'sec-enderecos', label: 'Enderecos', icon: 'home' },
    { sectionId: 'sec-contatos-formas', label: 'Contatos', icon: 'contact_phone' },
    { sectionId: 'sec-observacoes', label: 'Observacoes', icon: 'comment' },
    { sectionId: 'sec-grupos-pendentes', label: 'Grupos pendentes', icon: 'pending_actions' },
    { sectionId: 'sec-familiares', label: 'Familiares', icon: 'family_restroom' },
    { sectionId: 'sec-documentacao', label: 'Documentacao', icon: 'description' },
    { sectionId: 'sec-comercial', label: 'Comercial', icon: 'business_center' },
    { sectionId: 'sec-fiscal', label: 'Fiscal', icon: 'receipt_long' },
    { sectionId: 'sec-rh-contrato', label: 'Contrato RH', icon: 'badge' },
    { sectionId: 'sec-rh-info', label: 'Informacoes RH', icon: 'groups' },
    { sectionId: 'sec-referencias', label: 'Referencias', icon: 'link' },
    { sectionId: 'sec-qualificacoes', label: 'Qualificacoes', icon: 'school' }
  ];
  activeQuickNavSectionId = 'sec-topo';
  isMobileView = typeof window !== 'undefined' ? window.innerWidth < 768 : false;
  private tipoRegistroManualOverride = false;
  private lastRegistroLookupKey = '';
  private lookingUpRegistroFederal = false;
  readonly formGroupLabels: Array<{ key: string; label: string }> = [
    { key: 'CONTATOS', label: 'Contatos' },
    { key: 'ENDERECOS', label: 'Enderecos' },
    { key: 'DOCUMENTACAO', label: 'Documentacao' },
    { key: 'COMERCIAL_FISCAL', label: 'Comercial e fiscal' },
    { key: 'RH', label: 'Recursos humanos' },
    { key: 'FAMILIARES_REFERENCIAS', label: 'Familiares, referencias e qualificacoes' }
  ];
  mode: 'new' | 'view' | 'edit' = 'new';
  title = 'Nova entidade';
  tipoNome = '';
  tipoSeed = '';
  tipoEntidadeId = 0;
  entidadeId: number | null = null;
  contexto: RegistroEntidadeContexto | null = null;
  contextoWarning = '';
  loading = false;
  saving = false;
  deleting = false;
  loadingSubresources = false;
  codigoInfo = 'Gerado ao salvar';
  selectedGrupoIdFromRoute: number | null = null;
  currentGrupoEntidadeId: number | null = null;
  currentVersion: number | null = null;
  priceBooks: PriceBook[] = [];
  rhOptions: EntidadeRhOptions = {
    frequenciasCobranca: [],
    tiposFuncionario: [],
    situacoesFuncionario: [],
    setores: [],
    cargos: [],
    ocupacoesAtividade: [],
    qualificacoes: []
  };
  enderecosCount = 0;
  contatosCount = 0;
  familiaresCount = 0;
  referenciasCount = 0;
  qualificacoesCount = 0;
  formConfigByGroup: EntidadeFormConfigByGroup | null = null;
  formGroupByKey: Record<string, EntidadeFormGroupConfig> = {};
  formFieldByKey: Record<string, EntidadeFormFieldConfig> = {};
  fieldGroupByKey: Record<string, string> = {};
  defaultRequiredByFieldKey: Record<string, boolean> = {};
  referencias: EntidadeReferencia[] = [];
  qualificacoes: EntidadeQualificacaoItem[] = [];
  enderecos: EntidadeEndereco[] = [];
  contatos: EntidadeContato[] = [];
  contatoFormasByContato: Record<number, EntidadeContatoForma[]> = {};
  contatoFormasFlat: EntidadeContatoFormaFlat[] = [];
  familiares: EntidadeFamiliar[] = [];
  savingReferencia = false;
  savingQualificacao = false;
  savingEndereco = false;
  savingContato = false;
  savingContatoForma = false;
  savingFamiliar = false;
  private lookingUpCep = false;
  private scrollContainerElement: HTMLElement | null = null;
  private readonly onContainerScroll = () => this.syncActiveQuickNavSection();
  mapPreviewOpen = false;
  mapPreviewUrl: SafeResourceUrl | null = null;
  mapPreviewTitle = '';
  mapPreviewSubtitle = '';
  enderecoEditorOpen = false;
  enderecoEditorMode: 'create' | 'edit' | 'view' = 'create';
  contatoEditorOpen = false;
  contatoEditorMode: 'create' | 'edit' | 'view' = 'create';
  familiarEditorOpen = false;
  familiarEditorMode: 'create' | 'edit' | 'view' = 'create';
  referenciaEditorOpen = false;
  referenciaEditorMode: 'create' | 'edit' | 'view' = 'create';
  qualificacaoEditorOpen = false;
  qualificacaoEditorMode: 'create' | 'edit' | 'view' = 'create';
  editingEnderecoId: number | null = null;
  editingContatoId: number | null = null;
  editingContatoFormaId: number | null = null;
  editingContatoFormaContatoId: number | null = null;
  editingFamiliarId: number | null = null;
  editingReferenciaId: number | null = null;
  editingQualificacaoId: number | null = null;

  form = this.fb.group({
    ativo: [true, Validators.required],
    priceBookId: [null as number | null],
    tratamento: ['', [Validators.maxLength(120)]],
    codigoBarras: ['', [Validators.maxLength(60)]],
    tipoPessoa: ['FISICA', Validators.required],
    genero: ['', [Validators.maxLength(30)]],
    nacionalidade: ['', [Validators.maxLength(120)]],
    naturalidade: ['', [Validators.maxLength(120)]],
    estadoCivil: ['', [Validators.maxLength(30)]],
    dataNascimento: [''],
    alerta: ['', [Validators.maxLength(1000)]],
    observacao: [''],
    parecer: [''],
    textoTermoQuitacao: ['', [Validators.maxLength(4096)]],
    pessoaNome: ['', [Validators.required, Validators.maxLength(200)]],
    pessoaApelido: ['', [Validators.maxLength(200)]],
    tipoRegistro: ['CPF', Validators.required],
    registroFederal: ['', Validators.required]
  });

  documentacaoForm = this.fb.group({
    rg: [''],
    rgTipo: [''],
    rgDataEmissao: [''],
    rgUfEmissao: [''],
    rntc: [''],
    rntcCategoria: [''],
    ctps: [''],
    ctpsSerie: [''],
    ctpsDataEmissao: [''],
    ctpsUfEmissao: [''],
    pis: [''],
    tituloEleitor: [''],
    tituloEleitorZona: [''],
    tituloEleitorSecao: [''],
    cnh: [''],
    cnhCategoria: [''],
    cnhDataEmissao: [''],
    cnhObservacao: [''],
    militarNumero: [''],
    militarSerie: [''],
    militarCategoria: [''],
    registroEstadualContribuinte: [null as boolean | null],
    numeroNif: [''],
    motivoNaoNif: [null as number | null]
  });
  comercialForm = this.fb.group({
    faturamentoDiaInicial: [''],
    faturamentoDiaFinal: [''],
    faturamentoDiasPrazo: [null as number | null],
    prazoEntregaDias: [null as number | null],
    faturamentoFrequenciaCobrancaId: [null as number | null],
    juroTaxaPadrao: [null as number | null],
    ramoAtividade: [''],
    boletosEnviarEmail: [false],
    consumidorFinal: [false]
  });
  fiscalForm = this.fb.group({
    manifestarNotaAutomaticamente: [null as number | null],
    usaNotaFiscalFatura: [null as number | null],
    ignorarImportacaoNota: [null as number | null]
  });
  rhContratoForm = this.fb.group({
    numero: [''],
    admissaoData: [''],
    remuneracao: [null as number | null],
    remuneracaoComplementar: [null as number | null],
    bonificacao: [null as number | null],
    sindicalizado: [false],
    percentualInsalubridade: [null as number | null],
    percentualPericulosidade: [null as number | null],
    tipoFuncionarioId: [null as number | null],
    situacaoFuncionarioId: [null as number | null],
    setorId: [null as number | null],
    cargoId: [null as number | null],
    ocupacaoAtividadeId: [null as number | null]
  });
  rhInfoForm = this.fb.group({
    atividades: [''],
    habilidades: [''],
    experiencias: [''],
    aceitaViajar: [false],
    possuiCarro: [false],
    possuiMoto: [false],
    metaMediaHorasVendidasDia: [null as number | null],
    metaProdutosVendidos: [null as number | null]
  });
  referenciaForm = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(160)]],
    atividades: [''],
    dataInicio: [''],
    dataFim: ['']
  });
  qualificacaoForm = this.fb.group({
    rhQualificacaoId: [null as number | null, Validators.required],
    completo: [false],
    tipo: ['', [Validators.maxLength(1)]]
  });
  enderecoForm = this.fb.group({
    enderecoTipo: ['RESIDENCIAL', Validators.required],
    cep: ['', [Validators.maxLength(9), Validators.pattern(/^$|^\d{5}-?\d{3}$/)]],
    pais: ['Brasil', [Validators.maxLength(120)]],
    paisCodigoIbge: [1058 as number | null],
    uf: ['', [Validators.maxLength(2), Validators.pattern(/^$|^[A-Za-z]{2}$/)]],
    ufCodigoIbge: ['', [Validators.maxLength(10)]],
    municipio: ['', [Validators.maxLength(120)]],
    municipioCodigoIbge: ['', [Validators.maxLength(10)]],
    bairro: ['', [Validators.maxLength(120)]],
    logradouro: ['', [Validators.maxLength(160)]],
    numero: ['', [Validators.maxLength(20)]],
    complemento: ['', [Validators.maxLength(120)]],
    estadoProvinciaRegiaoEstrangeiro: ['', [Validators.maxLength(120)]],
    latitude: [''],
    longitude: [''],
    principal: [false]
  });
  contatoForm = this.fb.group({
    nome: ['', [Validators.maxLength(160)]],
    cargo: ['', [Validators.maxLength(100)]]
  });
  contatoFormaForm = this.fb.group({
    contatoId: [null as number | null],
    tipoContato: ['EMAIL', Validators.required],
    valor: ['', [Validators.required, Validators.maxLength(255)]],
    preferencial: [false]
  });
  familiarForm = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(160)]],
    parentesco: ['OUTROS', Validators.required],
    dependente: [false]
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private service: EntityRecordService,
    private pricingService: CatalogPricingService,
    private notify: NotificationService,
    private accessControl: AccessControlService,
    private typeService: EntityTypeService,
    private typeAccess: EntityTypeAccessService,
    private elRef: ElementRef<HTMLElement>,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.tipoEntidadeId = Number(this.route.snapshot.queryParamMap.get('tipoEntidadeId') || 0);
    if (!this.tipoEntidadeId) { this.notify.error('Tipo de entidade nao informado.'); this.back(); return; }
    const idParam = this.route.snapshot.paramMap.get('id');
    this.selectedGrupoIdFromRoute = this.toNumber(this.route.snapshot.queryParamMap.get('grupoId'));
    const isEdit = this.route.snapshot.url.some(s => s.path === 'edit');
    if (idParam) { this.entidadeId = Number(idParam); this.mode = isEdit ? 'edit' : 'view'; this.title = this.mode === 'edit' ? 'Editar entidade' : 'Consultar entidade'; }
    this.setupSubresourceValidationAndSearch();
    this.captureDefaultRequiredByFieldKey();
    this.loadTipoAndContinue();
    setTimeout(() => this.syncActiveQuickNavSection(), 0);
  }

  ngAfterViewInit(): void {
    this.bindScrollContainer();
    setTimeout(() => this.syncActiveQuickNavSection(), 0);
  }

  ngOnDestroy(): void {
    this.unbindScrollContainer();
  }

  canEdit(): boolean { return this.mode !== 'view' && !!this.contexto?.vinculado; }
  canEditGroup(groupKey: string): boolean {
    return this.canEdit() && this.hasGroupEditAccess(groupKey);
  }
  toEdit(): void { if (this.entidadeId) this.router.navigate(['/entities', this.entidadeId, 'edit'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } }); }
  back(): void { this.router.navigate(['/entities'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId || null } }); }
  cadastroTitle(): string { const n = (this.tipoNome || '').trim(); return n ? `Cadastro ${n}` : 'Cadastro de Entidades'; }
  cadastroTipoIcon(): string {
    const seed = (this.tipoSeed || '').trim().toUpperCase();
    if (seed === 'CLIENTE') return 'person';
    if (seed === 'FORNECEDOR') return 'local_shipping';
    if (seed === 'EQUIPE') return 'groups';
    return 'category';
  }
  ativoHeaderLabel(): string { return this.form.value.ativo ? 'Ativo' : 'Inativo'; }
  setAtivoFromHeader(nextValue: boolean): void { this.form.controls.ativo.setValue(!!nextValue); }
  tipoRegistroLabel(): string {
    const tipo = (this.form.value.tipoRegistro || 'CPF') as string;
    if (tipo === 'CNPJ') return 'CNPJ';
    if (tipo === 'ID_ESTRANGEIRO') return 'ID estrangeiro';
    return 'CPF';
  }
  registroFederalMaxLength(): number {
    const tipo = (this.form.value.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    if (tipo === 'CPF') return 14;
    if (tipo === 'CNPJ') return 18;
    return 30;
  }
  registroFederalInputMode(): string {
    const tipo = (this.form.value.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    return tipo === 'ID_ESTRANGEIRO' ? 'text' : 'numeric';
  }
  onRegistroFederalInput(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    if (!input) return;
    const rawValue = input.value || '';
    let tipoAtual = (this.form.controls.tipoRegistro.value || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    if (!this.tipoRegistroManualOverride) {
      const inferido = this.inferTipoRegistro(rawValue);
      if (inferido !== tipoAtual) {
        tipoAtual = inferido;
        this.form.controls.tipoRegistro.setValue(inferido, { emitEvent: false });
      }
    }
    const formatted = this.formatRegistroFederalByTipo(tipoAtual, rawValue);
    if (formatted !== rawValue) input.value = formatted;
    if (this.form.controls.registroFederal.value !== formatted) {
      this.form.controls.registroFederal.setValue(formatted, { emitEvent: false });
    }
  }
  quickNavVisibleItems(): EntityRecordQuickNavItem[] { return this.quickNavItems.filter(item => this.isQuickNavItemVisible(item.sectionId)); }
  scrollToSection(sectionId: string): void {
    const element = document.getElementById(sectionId);
    if (!element) return;
    const topOffset = this.sectionTopOffset();
    const container = this.scrollContainerElement;
    if (container) {
      const containerTop = container.getBoundingClientRect().top;
      const top = container.scrollTop + element.getBoundingClientRect().top - containerTop - topOffset;
      container.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    } else {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
    this.activeQuickNavSectionId = sectionId;
  }
  isQuickNavSectionActive(sectionId: string): boolean { return this.activeQuickNavSectionId === sectionId; }

  @HostListener('window:resize')
  onWindowLayoutChange(): void {
    this.isMobileView = window.innerWidth < 768;
    this.syncActiveQuickNavSection();
  }

  save(): void {
    if (!this.canEdit()) return;
    const raw = this.form.getRawValue();
    const dataNascimentoRaw = (raw.dataNascimento || '').trim();
    if (dataNascimentoRaw && !isValidDateInput(dataNascimentoRaw)) {
      this.notify.error('Data de nascimento invalida. Use DD/MM/AAAA.');
      return;
    }
    if (!this.validateDocumentacaoDates()) return;
    if (!this.validateComercialDates()) return;
    if (this.mode !== 'new' && this.entidadeId && this.isGroupEnabled('RH')) {
      const contratoRaw = this.rhContratoForm.getRawValue();
      const admissaoRaw = (contratoRaw.admissaoData || '').trim();
      if (admissaoRaw && !isValidDateInput(admissaoRaw)) {
        this.notify.error('Data de admissao invalida. Use DD/MM/AAAA.');
        return;
      }
    }
    const tipoRegistro = (raw.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    const payload: RegistroEntidadePayload = {
      grupoEntidadeId: this.mode === 'new' ? this.selectedGrupoIdFromRoute : this.currentGrupoEntidadeId,
      priceBookId: raw.priceBookId ?? null,
      tratamento: (raw.tratamento || '').trim() || null,
      codigoBarras: (raw.codigoBarras || '').trim() || null,
      alerta: (raw.alerta || '').trim() || null,
      observacao: (raw.observacao || '').trim() || null,
      parecer: (raw.parecer || '').trim() || null,
      textoTermoQuitacao: (raw.textoTermoQuitacao || '').trim() || null,
      version: this.currentVersion, ativo: !!raw.ativo,
      pessoa: {
        nome: (raw.pessoaNome || '').trim(),
        apelido: (raw.pessoaApelido || '').trim() || undefined,
        tipoRegistro,
        registroFederal: this.normalizeRegistroFederal(tipoRegistro, raw.registroFederal || ''),
        tipoPessoa: (raw.tipoPessoa || 'FISICA') as 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA',
        genero: (raw.genero || '').trim() || undefined,
        nacionalidade: (raw.nacionalidade || '').trim() || undefined,
        naturalidade: (raw.naturalidade || '').trim() || undefined,
        estadoCivil: (raw.estadoCivil || '').trim() || undefined,
        dataNascimento: dataNascimentoRaw ? toIsoDate(dataNascimentoRaw) : null
      }
    };
    this.saving = true;
    const obs = this.mode === 'new'
      ? this.service.create(this.tipoEntidadeId, payload)
      : this.service.update(this.tipoEntidadeId, this.entidadeId!, payload);
    obs.subscribe({
      next: row => {
        this.currentVersion = row.version ?? this.currentVersion;
        if (this.mode === 'new') {
          this.saving = false;
          this.notify.success('Entidade criada.');
          this.router.navigate(['/entities', row.id, 'edit'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
          return;
        }
        const savedId = Number(row?.id || this.entidadeId || 0);
        if (!savedId) {
          this.saving = false;
          this.notify.success('Entidade atualizada.');
          return;
        }
        this.persistOneToOneSections(savedId).subscribe({
          next: () => {
            this.saving = false;
            this.notify.success('Entidade atualizada.');
          },
          error: (err: any) => {
            this.saving = false;
            this.notify.error(err?.error?.detail || 'Entidade salva, mas nao foi possivel salvar dados complementares.');
          }
        });
      },
      error: err => {
        this.saving = false;
        this.notify.error(err?.error?.detail || 'Nao foi possivel salvar entidade.');
      }
    });
  }

  private persistOneToOneSections(entidadeId: number): Observable<unknown> {
    const requests: Observable<unknown>[] = [];

    if (this.isGroupEnabled('DOCUMENTACAO') && this.canEditGroup('DOCUMENTACAO')) {
      const payload = this.buildDocumentacaoPayload();
      requests.push(this.service.upsertDocumentacao(this.tipoEntidadeId, entidadeId, payload));
    }

    if (this.isGroupEnabled('COMERCIAL_FISCAL') && this.canEditGroup('COMERCIAL_FISCAL')) {
      const comercialRaw = this.comercialForm.getRawValue();
      const faturamentoDiaInicialRaw = (comercialRaw.faturamentoDiaInicial || '').trim();
      const faturamentoDiaFinalRaw = (comercialRaw.faturamentoDiaFinal || '').trim();
      const comercialPayload: EntidadeInfoComercialPayload = {
        faturamentoDiaInicial: faturamentoDiaInicialRaw ? toIsoDate(faturamentoDiaInicialRaw) : null,
        faturamentoDiaFinal: faturamentoDiaFinalRaw ? toIsoDate(faturamentoDiaFinalRaw) : null,
        faturamentoDiasPrazo: this.toNonNegativeInteger(comercialRaw.faturamentoDiasPrazo),
        prazoEntregaDias: this.toNonNegativeInteger(comercialRaw.prazoEntregaDias),
        faturamentoFrequenciaCobrancaId: this.toNumber(comercialRaw.faturamentoFrequenciaCobrancaId),
        juroTaxaPadrao: this.toFloat(comercialRaw.juroTaxaPadrao),
        ramoAtividade: (comercialRaw.ramoAtividade || '').trim() || null,
        boletosEnviarEmail: !!comercialRaw.boletosEnviarEmail,
        consumidorFinal: !!comercialRaw.consumidorFinal
      };
      requests.push(this.service.upsertInfoComercial(this.tipoEntidadeId, entidadeId, comercialPayload));

      const fiscalRaw = this.fiscalForm.getRawValue();
      const fiscalPayload: EntidadeDadosFiscaisPayload = {
        manifestarNotaAutomaticamente: this.toSmallInt(fiscalRaw.manifestarNotaAutomaticamente),
        usaNotaFiscalFatura: this.toSmallInt(fiscalRaw.usaNotaFiscalFatura),
        ignorarImportacaoNota: this.toSmallInt(fiscalRaw.ignorarImportacaoNota)
      };
      requests.push(this.service.upsertDadosFiscais(this.tipoEntidadeId, entidadeId, fiscalPayload));
    }

    if (this.isGroupEnabled('RH') && this.canEditGroup('RH')) {
      const contratoRaw = this.rhContratoForm.getRawValue();
      const admissaoRaw = (contratoRaw.admissaoData || '').trim();
      const contratoPayload: EntidadeContratoRhPayload = {
        numero: (contratoRaw.numero || '').trim() || null,
        admissaoData: admissaoRaw ? toIsoDate(admissaoRaw) : null,
        remuneracao: this.toFloat(contratoRaw.remuneracao),
        remuneracaoComplementar: this.toFloat(contratoRaw.remuneracaoComplementar),
        bonificacao: this.toFloat(contratoRaw.bonificacao),
        sindicalizado: !!contratoRaw.sindicalizado,
        percentualInsalubridade: this.toFloat(contratoRaw.percentualInsalubridade),
        percentualPericulosidade: this.toFloat(contratoRaw.percentualPericulosidade),
        tipoFuncionarioId: this.toNumber(contratoRaw.tipoFuncionarioId),
        situacaoFuncionarioId: this.toNumber(contratoRaw.situacaoFuncionarioId),
        setorId: this.toNumber(contratoRaw.setorId),
        cargoId: this.toNumber(contratoRaw.cargoId),
        ocupacaoAtividadeId: this.toNumber(contratoRaw.ocupacaoAtividadeId)
      };
      requests.push(this.service.upsertContratoRh(this.tipoEntidadeId, entidadeId, contratoPayload));

      const infoRaw = this.rhInfoForm.getRawValue();
      const infoPayload: EntidadeInfoRhPayload = {
        atividades: (infoRaw.atividades || '').trim() || null,
        habilidades: (infoRaw.habilidades || '').trim() || null,
        experiencias: (infoRaw.experiencias || '').trim() || null,
        aceitaViajar: !!infoRaw.aceitaViajar,
        possuiCarro: !!infoRaw.possuiCarro,
        possuiMoto: !!infoRaw.possuiMoto,
        metaMediaHorasVendidasDia: this.toNumber(infoRaw.metaMediaHorasVendidasDia),
        metaProdutosVendidos: this.toFloat(infoRaw.metaProdutosVendidos)
      };
      requests.push(this.service.upsertInfoRh(this.tipoEntidadeId, entidadeId, infoPayload));
    }

    if (!requests.length) return of(null);
    return forkJoin(requests);
  }

  remove(): void {
    if (!this.entidadeId || this.mode === 'new') return;
    if (!confirm(`Excluir entidade codigo ${this.codigoInfo || ''}?`)) return;
    this.deleting = true;
    this.service.delete(this.tipoEntidadeId, this.entidadeId).pipe(finalize(() => (this.deleting = false))).subscribe({
      next: () => { this.notify.success('Entidade excluida.'); this.back(); },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel excluir entidade.')
    });
  }

  saveDocumentacao(): void {
    if (!this.canEditGroup('DOCUMENTACAO') || !this.entidadeId) return;
    if (!this.validateDocumentacaoDates()) return;
    const payload = this.buildDocumentacaoPayload();
    this.service.upsertDocumentacao(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Documentacao salva.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar documentacao.')
    });
  }

  saveComercial(): void {
    if (!this.canEditGroup('COMERCIAL_FISCAL') || !this.entidadeId) return;
    if (!this.validateComercialDates()) return;
    const raw = this.comercialForm.getRawValue();
    const faturamentoDiaInicialRaw = (raw.faturamentoDiaInicial || '').trim();
    const faturamentoDiaFinalRaw = (raw.faturamentoDiaFinal || '').trim();
    const payload: EntidadeInfoComercialPayload = {
      faturamentoDiaInicial: faturamentoDiaInicialRaw ? toIsoDate(faturamentoDiaInicialRaw) : null,
      faturamentoDiaFinal: faturamentoDiaFinalRaw ? toIsoDate(faturamentoDiaFinalRaw) : null,
      faturamentoDiasPrazo: this.toNonNegativeInteger(raw.faturamentoDiasPrazo),
      prazoEntregaDias: this.toNonNegativeInteger(raw.prazoEntregaDias),
      faturamentoFrequenciaCobrancaId: this.toNumber(raw.faturamentoFrequenciaCobrancaId),
      juroTaxaPadrao: this.toFloat(raw.juroTaxaPadrao),
      ramoAtividade: (raw.ramoAtividade || '').trim() || null,
      boletosEnviarEmail: !!raw.boletosEnviarEmail,
      consumidorFinal: !!raw.consumidorFinal
    };
    this.service.upsertInfoComercial(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Informacoes comerciais salvas.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar informacoes comerciais.')
    });
  }

  saveFiscal(): void {
    if (!this.canEditGroup('COMERCIAL_FISCAL') || !this.entidadeId) return;
    const raw = this.fiscalForm.getRawValue();
    const payload: EntidadeDadosFiscaisPayload = {
      manifestarNotaAutomaticamente: this.toSmallInt(raw.manifestarNotaAutomaticamente),
      usaNotaFiscalFatura: this.toSmallInt(raw.usaNotaFiscalFatura),
      ignorarImportacaoNota: this.toSmallInt(raw.ignorarImportacaoNota)
    };
    this.service.upsertDadosFiscais(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Dados fiscais salvos.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar dados fiscais.')
    });
  }

  saveRhContrato(): void {
    if (!this.canEditGroup('RH') || !this.entidadeId) return;
    const raw = this.rhContratoForm.getRawValue();
    const admissaoRaw = (raw.admissaoData || '').trim();
    if (admissaoRaw && !isValidDateInput(admissaoRaw)) {
      this.notify.error('Data de admissao invalida. Use DD/MM/AAAA.');
      return;
    }
    const payload: EntidadeContratoRhPayload = {
      numero: (raw.numero || '').trim() || null,
      admissaoData: admissaoRaw ? toIsoDate(admissaoRaw) : null,
      remuneracao: this.toFloat(raw.remuneracao),
      remuneracaoComplementar: this.toFloat(raw.remuneracaoComplementar),
      bonificacao: this.toFloat(raw.bonificacao),
      sindicalizado: !!raw.sindicalizado,
      percentualInsalubridade: this.toFloat(raw.percentualInsalubridade),
      percentualPericulosidade: this.toFloat(raw.percentualPericulosidade),
      tipoFuncionarioId: this.toNumber(raw.tipoFuncionarioId),
      situacaoFuncionarioId: this.toNumber(raw.situacaoFuncionarioId),
      setorId: this.toNumber(raw.setorId),
      cargoId: this.toNumber(raw.cargoId),
      ocupacaoAtividadeId: this.toNumber(raw.ocupacaoAtividadeId)
    };
    this.service.upsertContratoRh(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Contrato RH salvo.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar contrato RH.')
    });
  }

  saveRhInfo(): void {
    if (!this.canEditGroup('RH') || !this.entidadeId) return;
    const raw = this.rhInfoForm.getRawValue();
    const payload: EntidadeInfoRhPayload = {
      atividades: (raw.atividades || '').trim() || null,
      habilidades: (raw.habilidades || '').trim() || null,
      experiencias: (raw.experiencias || '').trim() || null,
      aceitaViajar: !!raw.aceitaViajar,
      possuiCarro: !!raw.possuiCarro,
      possuiMoto: !!raw.possuiMoto,
      metaMediaHorasVendidasDia: this.toNumber(raw.metaMediaHorasVendidasDia),
      metaProdutosVendidos: this.toFloat(raw.metaProdutosVendidos)
    };
    this.service.upsertInfoRh(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Informacoes RH salvas.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar informacoes RH.')
    });
  }

  saveReferencia(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId || this.referenciaEditorMode === 'view') return;
    if (this.referenciaForm.invalid) {
      this.referenciaForm.markAllAsTouched();
      this.notify.error('Informe ao menos o nome da referencia.');
      return;
    }
    const raw = this.referenciaForm.getRawValue();
    const dataInicioRaw = (raw.dataInicio || '').trim();
    const dataFimRaw = (raw.dataFim || '').trim();
    if (dataInicioRaw && !isValidDateInput(dataInicioRaw)) {
      this.notify.error('Data inicio invalida. Use DD/MM/AAAA.');
      return;
    }
    if (dataFimRaw && !isValidDateInput(dataFimRaw)) {
      this.notify.error('Data fim invalida. Use DD/MM/AAAA.');
      return;
    }
    const payload = {
      nome: (raw.nome || '').trim(),
      atividades: (raw.atividades || '').trim() || null,
      dataInicio: dataInicioRaw ? toIsoDate(dataInicioRaw) : null,
      dataFim: dataFimRaw ? toIsoDate(dataFimRaw) : null
    };
    this.savingReferencia = true;
    const current = this.referencias.find(item => item.id === this.editingReferenciaId);
    const req = this.editingReferenciaId
      ? this.service.updateReferencia(
        this.tipoEntidadeId,
        this.entidadeId,
        this.editingReferenciaId,
        { ...payload, version: current?.version ?? null }
      )
      : this.service.createReferencia(this.tipoEntidadeId, this.entidadeId, payload);
    req
      .pipe(finalize(() => (this.savingReferencia = false)))
      .subscribe({
        next: () => {
          this.notify.success(this.editingReferenciaId ? 'Referencia atualizada.' : 'Referencia adicionada.');
          this.cancelReferenciaEdit();
          this.loadReferencias();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar referencia.')
      });
  }

  removeReferencia(id: number): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId) return;
    this.service.deleteReferencia(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Referencia removida.');
        this.loadReferencias();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover referencia.')
    });
  }

  saveQualificacao(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId || this.qualificacaoEditorMode === 'view') return;
    if (this.qualificacaoForm.invalid) {
      this.qualificacaoForm.markAllAsTouched();
      this.notify.error('Selecione a qualificacao.');
      return;
    }
    const raw = this.qualificacaoForm.getRawValue();
    const payload = {
      rhQualificacaoId: this.toNumber(raw.rhQualificacaoId)!,
      completo: !!raw.completo,
      tipo: (raw.tipo || '').trim() || null
    };
    this.savingQualificacao = true;
    const current = this.qualificacoes.find(item => item.id === this.editingQualificacaoId);
    const req = this.editingQualificacaoId
      ? this.service.updateQualificacao(
        this.tipoEntidadeId,
        this.entidadeId,
        this.editingQualificacaoId,
        { ...payload, version: current?.version ?? null }
      )
      : this.service.createQualificacao(this.tipoEntidadeId, this.entidadeId, payload);
    req
      .pipe(finalize(() => (this.savingQualificacao = false)))
      .subscribe({
        next: () => {
          this.notify.success(this.editingQualificacaoId ? 'Qualificacao atualizada.' : 'Qualificacao adicionada.');
          this.cancelQualificacaoEdit();
          this.loadQualificacoes();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar qualificacao.')
      });
  }

  removeQualificacao(id: number): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId) return;
    this.service.deleteQualificacao(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Qualificacao removida.');
        this.loadQualificacoes();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover qualificacao.')
    });
  }

  saveEndereco(): void {
    if (!this.canEditGroup('ENDERECOS') || !this.entidadeId || this.enderecoEditorMode === 'view') return;
    if (this.enderecoForm.invalid) {
      this.enderecoForm.markAllAsTouched();
      this.notify.error('Preencha os campos obrigatorios do endereco.');
      return;
    }
    const raw = this.enderecoForm.getRawValue();
    const payload: EntidadeEnderecoPayload = {
      nome: null,
      enderecoTipo: (raw.enderecoTipo || 'RESIDENCIAL') as EntidadeEnderecoPayload['enderecoTipo'],
      cep: (raw.cep || '').trim() || null,
      pais: (raw.pais || '').trim() || null,
      paisCodigoIbge: this.toNumber(raw.paisCodigoIbge),
      uf: ((raw.uf || '').trim() || '').toUpperCase() || null,
      ufCodigoIbge: (raw.ufCodigoIbge || '').trim() || null,
      municipio: (raw.municipio || '').trim() || null,
      municipioCodigoIbge: (raw.municipioCodigoIbge || '').trim() || null,
      bairro: (raw.bairro || '').trim() || null,
      logradouro: (raw.logradouro || '').trim() || null,
      numero: (raw.numero || '').trim() || null,
      complemento: (raw.complemento || '').trim() || null,
      estadoProvinciaRegiaoEstrangeiro: (raw.estadoProvinciaRegiaoEstrangeiro || '').trim() || null,
      latitude: this.toDecimal(raw.latitude),
      longitude: this.toDecimal(raw.longitude),
      principal: !!raw.principal
    };
    this.savingEndereco = true;
    const current = this.enderecos.find(item => item.id === this.editingEnderecoId);
    const req = this.editingEnderecoId
      ? this.service.updateEndereco(this.tipoEntidadeId, this.entidadeId, this.editingEnderecoId, { ...payload, version: current?.version ?? null })
      : this.service.createEndereco(this.tipoEntidadeId, this.entidadeId, payload);
    req.pipe(finalize(() => (this.savingEndereco = false))).subscribe({
      next: () => {
        this.notify.success(this.editingEnderecoId ? 'Endereco atualizado.' : 'Endereco adicionado.');
        this.cancelEnderecoEdit();
        this.loadEnderecos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar endereco.')
    });
  }

  removeEndereco(id: number): void {
    if (!this.canEditGroup('ENDERECOS') || !this.entidadeId) return;
    this.service.deleteEndereco(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Endereco removido.');
        this.loadEnderecos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover endereco.')
    });
  }

  saveContato(): void {
    if (!this.canEditGroup('CONTATOS') || !this.entidadeId || this.contatoEditorMode === 'view') return;
    const raw = this.contatoForm.getRawValue();
    const payload = {
      nome: (raw.nome || '').trim() || null,
      cargo: (raw.cargo || '').trim() || null
    };
    this.savingContato = true;
    const current = this.contatos.find(item => item.id === this.editingContatoId);
    const req = this.editingContatoId
      ? this.service.updateContato(this.tipoEntidadeId, this.entidadeId, this.editingContatoId, { ...payload, version: current?.version ?? null })
      : this.service.createContato(this.tipoEntidadeId, this.entidadeId, payload);
    req.pipe(finalize(() => (this.savingContato = false))).subscribe({
      next: () => {
        this.notify.success(this.editingContatoId ? 'Contato atualizado.' : 'Contato adicionado.');
        this.cancelContatoEdit();
        this.loadContatos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar contato.')
    });
  }

  removeContato(id: number): void {
    if (!this.canEditGroup('CONTATOS') || !this.entidadeId) return;
    this.service.deleteContato(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Contato removido.');
        this.loadContatos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover contato.')
    });
  }

  saveContatoForma(): void {
    if (!this.canEditGroup('CONTATOS') || !this.entidadeId || this.contatoEditorMode === 'view') return;
    if (this.contatoFormaForm.invalid) {
      this.contatoFormaForm.markAllAsTouched();
      this.notify.error('Informe tipo e valor.');
      return;
    }
    const raw = this.contatoFormaForm.getRawValue();
    const payload: EntidadeContatoFormaPayload = {
      tipoContato: (raw.tipoContato || 'EMAIL') as EntidadeContatoFormaPayload['tipoContato'],
      valor: (raw.valor || '').trim(),
      preferencial: !!raw.preferencial
    };
    const persistedContatoId = this.editingContatoFormaContatoId ?? this.toNumber(raw.contatoId);

    const persist = (contatoId: number) => {
      this.savingContatoForma = true;
      const formas = this.contatoFormasByContato[contatoId] || [];
      const current = formas.find(item => item.id === this.editingContatoFormaId);
      const req = this.editingContatoFormaId
        ? this.service.updateContatoForma(
          this.tipoEntidadeId,
          this.entidadeId!,
          contatoId,
          this.editingContatoFormaId,
          { ...payload, version: current?.version ?? null }
        )
        : this.service.createContatoForma(this.tipoEntidadeId, this.entidadeId!, contatoId, payload);
      req.pipe(finalize(() => (this.savingContatoForma = false))).subscribe({
        next: () => {
          this.notify.success(this.editingContatoFormaId ? 'Contato atualizado.' : 'Contato adicionado.');
          this.cancelContatoEdit();
          this.loadContatos();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar contato.')
      });
    };

    if (persistedContatoId) {
      persist(persistedContatoId);
      return;
    }
    this.ensureContatoBaseId(persist);
  }

  removeContatoForma(contatoId: number, formaId: number): void {
    if (!this.canEditGroup('CONTATOS') || !this.entidadeId) return;
    this.service.deleteContatoForma(this.tipoEntidadeId, this.entidadeId, contatoId, formaId).subscribe({
      next: () => {
        this.notify.success('Contato removido.');
        this.loadContatos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover contato.')
    });
  }

  saveFamiliar(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId || this.familiarEditorMode === 'view') return;
    if (this.familiarForm.invalid) {
      this.familiarForm.markAllAsTouched();
      this.notify.error('Informe os campos obrigatorios do familiar.');
      return;
    }
    const raw = this.familiarForm.getRawValue();
    const payload: EntidadeFamiliarPayload = {
      nome: (raw.nome || '').trim(),
      parentesco: (raw.parentesco || 'OUTROS') as EntidadeFamiliarPayload['parentesco'],
      dependente: !!raw.dependente
    };
    this.savingFamiliar = true;
    const current = this.familiares.find(item => item.id === this.editingFamiliarId);
    const req = this.editingFamiliarId
      ? this.service.updateFamiliar(
        this.tipoEntidadeId,
        this.entidadeId,
        this.editingFamiliarId,
        { ...payload, version: current?.version ?? null }
      )
      : this.service.createFamiliar(this.tipoEntidadeId, this.entidadeId, payload);
    req.pipe(finalize(() => (this.savingFamiliar = false))).subscribe({
      next: () => {
        this.notify.success(this.editingFamiliarId ? 'Familiar atualizado.' : 'Familiar adicionado.');
        this.cancelFamiliarEdit();
        this.loadFamiliares();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar familiar.')
    });
  }

  removeFamiliar(id: number): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS') || !this.entidadeId) return;
    this.service.deleteFamiliar(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Familiar removido.');
        this.loadFamiliares();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover familiar.')
    });
  }

  editEndereco(item: EntidadeEndereco): void {
    this.enderecoEditorOpen = true;
    this.enderecoEditorMode = 'edit';
    this.editingEnderecoId = item.id;
    this.enderecoForm.patchValue({
      enderecoTipo: item.enderecoTipo || 'RESIDENCIAL',
      cep: item.cep || '',
      pais: item.pais || 'Brasil',
      paisCodigoIbge: item.paisCodigoIbge ?? 1058,
      uf: item.uf || '',
      ufCodigoIbge: item.ufCodigoIbge || '',
      municipio: item.municipio || '',
      municipioCodigoIbge: item.municipioCodigoIbge || '',
      bairro: item.bairro || '',
      logradouro: item.logradouro || '',
      numero: item.numero || '',
      complemento: item.complemento || '',
      estadoProvinciaRegiaoEstrangeiro: item.estadoProvinciaRegiaoEstrangeiro || '',
      latitude: item.latitude?.toString() || '',
      longitude: item.longitude?.toString() || '',
      principal: !!item.principal
    });
    this.enderecoForm.enable({ emitEvent: false });
  }

  cancelEnderecoEdit(): void {
    this.enderecoEditorOpen = false;
    this.enderecoEditorMode = 'create';
    this.editingEnderecoId = null;
    this.closeEnderecoMapPreview();
    this.enderecoForm.reset({
      enderecoTipo: 'RESIDENCIAL',
      cep: '',
      pais: 'Brasil',
      paisCodigoIbge: 1058,
      uf: '',
      ufCodigoIbge: '',
      municipio: '',
      municipioCodigoIbge: '',
      bairro: '',
      logradouro: '',
      numero: '',
      complemento: '',
      estadoProvinciaRegiaoEstrangeiro: '',
      latitude: '',
      longitude: '',
      principal: false
    });
    this.enderecoForm.enable({ emitEvent: false });
  }

  editContato(item: EntidadeContato): void {
    this.contatoEditorOpen = true;
    this.contatoEditorMode = 'edit';
    this.editingContatoId = item.id;
    this.contatoForm.patchValue({
      nome: item.nome || '',
      cargo: item.cargo || ''
    });
    this.contatoForm.enable({ emitEvent: false });
    this.contatoFormaForm.enable({ emitEvent: false });
  }

  cancelContatoEdit(): void {
    this.contatoEditorOpen = false;
    this.contatoEditorMode = 'create';
    this.editingContatoId = null;
    this.editingContatoFormaContatoId = null;
    this.editingContatoFormaId = null;
    this.contatoForm.reset({ nome: '', cargo: '' });
    this.contatoFormaForm.reset({
      contatoId: null,
      tipoContato: 'EMAIL',
      valor: '',
      preferencial: false
    });
    this.contatoForm.enable({ emitEvent: false });
    this.contatoFormaForm.enable({ emitEvent: false });
  }

  editContatoForma(contatoId: number, item: EntidadeContatoForma): void {
    this.contatoEditorOpen = true;
    this.contatoEditorMode = 'edit';
    this.editingContatoFormaContatoId = contatoId;
    this.editingContatoFormaId = item.id;
    this.contatoFormaForm.patchValue({
      contatoId,
      tipoContato: item.tipoContato,
      valor: item.valor,
      preferencial: !!item.preferencial
    });
    this.contatoForm.enable({ emitEvent: false });
    this.contatoFormaForm.enable({ emitEvent: false });
  }

  cancelContatoFormaEdit(): void {
    this.cancelContatoEdit();
  }

  editFamiliar(item: EntidadeFamiliar): void {
    this.familiarEditorOpen = true;
    this.familiarEditorMode = 'edit';
    this.editingFamiliarId = item.id;
    this.familiarForm.patchValue({
      nome: item.nome || '',
      parentesco: item.parentesco || 'OUTROS',
      dependente: !!item.dependente
    });
    this.familiarForm.enable({ emitEvent: false });
  }

  cancelFamiliarEdit(): void {
    this.familiarEditorOpen = false;
    this.familiarEditorMode = 'create';
    this.editingFamiliarId = null;
    this.familiarForm.reset({
      nome: '',
      parentesco: 'OUTROS',
      dependente: false
    });
    this.familiarForm.enable({ emitEvent: false });
  }

  openEnderecoCreate(): void {
    if (!this.canEditGroup('ENDERECOS')) return;
    this.cancelEnderecoEdit();
    this.enderecoEditorOpen = true;
    this.enderecoEditorMode = 'create';
  }

  viewEndereco(item: EntidadeEndereco): void {
    this.editEndereco(item);
    this.enderecoEditorMode = 'view';
    this.enderecoForm.disable({ emitEvent: false });
  }

  openContatoCreate(): void {
    if (!this.canEditGroup('CONTATOS')) return;
    this.cancelContatoEdit();
    this.contatoEditorOpen = true;
    this.contatoEditorMode = 'create';
  }

  viewContato(item: EntidadeContato): void {
    this.editContato(item);
    this.contatoEditorMode = 'view';
    this.contatoForm.disable({ emitEvent: false });
    this.contatoFormaForm.disable({ emitEvent: false });
  }

  viewContatoForma(contatoId: number, item: EntidadeContatoForma): void {
    this.editContatoForma(contatoId, item);
    this.contatoEditorMode = 'view';
    this.contatoForm.disable({ emitEvent: false });
    this.contatoFormaForm.disable({ emitEvent: false });
  }

  openFamiliarCreate(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS')) return;
    this.cancelFamiliarEdit();
    this.familiarEditorOpen = true;
    this.familiarEditorMode = 'create';
  }

  viewFamiliar(item: EntidadeFamiliar): void {
    this.editFamiliar(item);
    this.familiarEditorMode = 'view';
    this.familiarForm.disable({ emitEvent: false });
  }

  openReferenciaCreate(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS')) return;
    this.cancelReferenciaEdit();
    this.referenciaEditorOpen = true;
    this.referenciaEditorMode = 'create';
  }

  editReferencia(item: EntidadeReferencia): void {
    this.referenciaEditorOpen = true;
    this.referenciaEditorMode = 'edit';
    this.editingReferenciaId = item.id;
    this.referenciaForm.patchValue({
      nome: item.nome || '',
      atividades: item.atividades || '',
      dataInicio: item.dataInicio ? toDisplayDate(item.dataInicio) : '',
      dataFim: item.dataFim ? toDisplayDate(item.dataFim) : ''
    });
    this.referenciaForm.enable({ emitEvent: false });
  }

  viewReferencia(item: EntidadeReferencia): void {
    this.editReferencia(item);
    this.referenciaEditorMode = 'view';
    this.referenciaForm.disable({ emitEvent: false });
  }

  cancelReferenciaEdit(): void {
    this.referenciaEditorOpen = false;
    this.referenciaEditorMode = 'create';
    this.editingReferenciaId = null;
    this.referenciaForm.reset({ nome: '', atividades: '', dataInicio: '', dataFim: '' });
    this.referenciaForm.enable({ emitEvent: false });
  }

  openQualificacaoCreate(): void {
    if (!this.canEditGroup('FAMILIARES_REFERENCIAS')) return;
    this.cancelQualificacaoEdit();
    this.qualificacaoEditorOpen = true;
    this.qualificacaoEditorMode = 'create';
  }

  editQualificacao(item: EntidadeQualificacaoItem): void {
    this.qualificacaoEditorOpen = true;
    this.qualificacaoEditorMode = 'edit';
    this.editingQualificacaoId = item.id;
    this.qualificacaoForm.patchValue({
      rhQualificacaoId: item.rhQualificacaoId ?? null,
      completo: !!item.completo,
      tipo: item.tipo || ''
    });
    this.qualificacaoForm.enable({ emitEvent: false });
  }

  viewQualificacao(item: EntidadeQualificacaoItem): void {
    this.editQualificacao(item);
    this.qualificacaoEditorMode = 'view';
    this.qualificacaoForm.disable({ emitEvent: false });
  }

  cancelQualificacaoEdit(): void {
    this.qualificacaoEditorOpen = false;
    this.qualificacaoEditorMode = 'create';
    this.editingQualificacaoId = null;
    this.qualificacaoForm.reset({ rhQualificacaoId: null, completo: false, tipo: '' });
    this.qualificacaoForm.enable({ emitEvent: false });
  }

  isContatoFormaEmail(): boolean {
    return (this.contatoFormaForm.value.tipoContato || 'EMAIL') === 'EMAIL';
  }

  onContatoTipoChange(): void {
    this.applyContatoFormaValorValidators();
    this.contatoFormaForm.controls.valor.updateValueAndValidity({ emitEvent: false });
  }

  contatoFormaValorPlaceholder(): string {
    const tipo = (this.contatoFormaForm.value.tipoContato || 'EMAIL').trim();
    if (tipo === 'EMAIL') return 'exemplo@email.com';
    if (tipo === 'TELEFONE' || tipo === 'FONE_CELULAR' || tipo === 'FONE_RESIDENCIAL' || tipo === 'FONE_COMERCIAL' || tipo === 'WHATSAPP') {
      return '(51) 99999-9999';
    }
    if (tipo === 'SITE') return 'https://www.exemplo.com';
    if (tipo === 'LINKEDIN') return 'https://www.linkedin.com/in/usuario';
    if (tipo === 'INSTAGRAM') return '@usuario';
    if (tipo === 'FACEBOOK') return 'https://www.facebook.com/usuario';
    return 'Informe o valor';
  }

  contatoFormaValorLabel(): string {
    const tipo = (this.contatoFormaForm.value.tipoContato || 'EMAIL').trim();
    if (tipo === 'EMAIL') return 'E-mail';
    if (tipo === 'TELEFONE') return 'Telefone';
    if (tipo === 'FONE_CELULAR') return 'Celular';
    if (tipo === 'FONE_RESIDENCIAL') return 'Fone residencial';
    if (tipo === 'FONE_COMERCIAL') return 'Fone comercial';
    if (tipo === 'WHATSAPP') return 'WhatsApp';
    if (tipo === 'SITE') return 'Site';
    if (tipo === 'LINKEDIN') return 'LinkedIn';
    if (tipo === 'INSTAGRAM') return 'Instagram';
    if (tipo === 'FACEBOOK') return 'Facebook';
    return 'Valor';
  }

  isGroupEnabled(groupKey: string): boolean {
    const normalizedGroupKey = (groupKey || '').trim();
    if (!normalizedGroupKey) return true;
    if (!this.hasGroupViewAccess(normalizedGroupKey)) return false;
    const config = this.formGroupByKey[normalizedGroupKey];
    return config ? !!config.enabled : true;
  }

  isFieldVisible(fieldKey: string): boolean {
    const normalizedFieldKey = (fieldKey || '').trim();
    if (!normalizedFieldKey) return true;
    const groupKey = this.resolveGroupKeyForField(normalizedFieldKey);
    if (groupKey && !this.hasGroupViewAccess(groupKey)) return false;
    if (!this.hasFieldViewAccess(normalizedFieldKey)) return false;
    const config = this.formFieldByKey[normalizedFieldKey];
    return config ? !!config.visible : true;
  }

  isFieldRequired(fieldKey: string): boolean {
    const key = (fieldKey || '').trim();
    const config = this.formFieldByKey[key];
    if (config) return !!config.visible && !!config.required;
    return !!this.defaultRequiredByFieldKey[key];
  }

  isContatoFormaTipoVisible(tipoContato: string): boolean {
    return this.isFieldVisible(`contatos.formas.${(tipoContato || '').trim()}`);
  }

  private isQuickNavItemVisible(sectionId: string): boolean {
    switch (sectionId) {
      case 'sec-topo':
        return true;
      case 'sec-dados-entidade':
        return this.isGroupEnabled('DADOS_ENTIDADE');
      case 'sec-enderecos':
        return !!this.entidadeId && this.isGroupEnabled('ENDERECOS');
      case 'sec-contatos-formas':
        return !!this.entidadeId && this.isGroupEnabled('CONTATOS');
      case 'sec-observacoes':
        return this.isGroupEnabled('DADOS_ENTIDADE');
      case 'sec-grupos-pendentes':
        return !this.entidadeId && this.pendingEnabledGroupsForNew().length > 0;
      case 'sec-familiares':
      case 'sec-referencias':
      case 'sec-qualificacoes':
        return !!this.entidadeId && this.isGroupEnabled('FAMILIARES_REFERENCIAS');
      case 'sec-documentacao':
        return !!this.entidadeId && this.isGroupEnabled('DOCUMENTACAO');
      case 'sec-comercial':
      case 'sec-fiscal':
        return !!this.entidadeId && this.isGroupEnabled('COMERCIAL_FISCAL');
      case 'sec-rh-contrato':
      case 'sec-rh-info':
        return !!this.entidadeId && this.isGroupEnabled('RH');
      default:
        return false;
    }
  }

  private syncActiveQuickNavSection(): void {
    const navItems = this.quickNavVisibleItems();
    if (!navItems.length) {
      this.activeQuickNavSectionId = 'sec-topo';
      return;
    }
    const markerY = this.markerY();
    const firstItem = navItems[0];
    if (!firstItem) {
      this.activeQuickNavSectionId = 'sec-topo';
      return;
    }
    let activeId = firstItem.sectionId;
    let bestDistance = Number.POSITIVE_INFINITY;

    navItems.forEach(item => {
      const element = document.getElementById(item.sectionId);
      if (!element) return;
      const top = element.getBoundingClientRect().top;
      if (top <= markerY) {
        const distance = markerY - top;
        if (distance < bestDistance) {
          bestDistance = distance;
          activeId = item.sectionId;
        }
      }
    });

    if (bestDistance === Number.POSITIVE_INFINITY) {
      const firstExisting = navItems.find(item => !!document.getElementById(item.sectionId));
      if (firstExisting) activeId = firstExisting.sectionId;
    }
    this.activeQuickNavSectionId = activeId;
  }

  private markerY(): number {
    const container = this.scrollContainerElement;
    const offset = this.sectionTopOffset() + 8;
    if (!container) return offset;
    return container.getBoundingClientRect().top + offset;
  }

  private sectionTopOffset(): number {
    const header = this.elRef.nativeElement.querySelector('.entity-record-main .page-header-sticky') as HTMLElement | null;
    const headerHeight = header ? Math.ceil(header.getBoundingClientRect().height) : 0;
    return Math.max(108, headerHeight + 14);
  }

  private bindScrollContainer(): void {
    this.unbindScrollContainer();
    const host = this.elRef.nativeElement;
    const container = host.closest('.mat-drawer-content') as HTMLElement | null;
    if (!container) return;
    this.scrollContainerElement = container;
    this.scrollContainerElement.addEventListener('scroll', this.onContainerScroll, { passive: true });
  }

  private unbindScrollContainer(): void {
    if (!this.scrollContainerElement) return;
    this.scrollContainerElement.removeEventListener('scroll', this.onContainerScroll);
    this.scrollContainerElement = null;
  }

  pendingEnabledGroupsForNew(): Array<{ key: string; label: string }> {
    if (this.entidadeId) return [];
    return this.formGroupLabels.filter(item => this.isGroupEnabled(item.key));
  }

  private loadTipoAndContinue(): void {
    this.loadPriceBooks();
    this.typeService.get(this.tipoEntidadeId).subscribe({
      next: tipo => {
        this.tipoNome = (tipo?.nome || '').trim();
        this.tipoSeed = (tipo?.codigoSeed || '').trim();
        if (!this.typeAccess.canAccessType(tipo)) { this.notify.error('Voce nao possui acesso ao tipo de entidade selecionado.'); this.back(); return; }
        this.resolveContextAndLoad();
      },
      error: () => this.resolveContextAndLoad()
    });
  }

  private resolveContextAndLoad(): void {
    if (!this.hasEmpresaContext()) { this.contextoWarning = 'Selecione uma empresa no topo do sistema para continuar.'; this.form.disable(); return; }
    this.loading = true;
    this.service.contextoEmpresa(this.tipoEntidadeId).pipe(finalize(() => (this.loading = false))).subscribe({
      next: ctx => {
        this.contexto = ctx;
        if (!ctx.vinculado) { this.contextoWarning = ctx.mensagem || 'Empresa sem grupo para o tipo selecionado.'; this.form.disable(); return; }
        this.contextoWarning = '';
        this.loadFichaConfig();
        if (this.entidadeId) this.loadEntidade(this.entidadeId); else this.form.enable();
      },
      error: err => { this.contextoWarning = err?.error?.detail || 'Nao foi possivel resolver contexto da empresa.'; this.form.disable(); }
    });
  }

  private loadEntidade(id: number): void {
    this.loading = true;
    this.service.get(this.tipoEntidadeId, id).pipe(finalize(() => (this.loading = false))).subscribe({
      next: entity => { this.patchRoot(entity); this.loadSubresources(); if (!this.canEdit()) { this.form.disable(); } },
      error: err => { this.notify.error(err?.error?.detail || 'Nao foi possivel carregar entidade.'); this.back(); }
    });
  }

  private patchRoot(entity: RegistroEntidade): void {
    this.currentGrupoEntidadeId = entity.grupoEntidadeId || null;
    this.currentVersion = entity.version ?? null;
    this.codigoInfo = String(entity.codigo || '');
    this.form.patchValue({
      ativo: entity.ativo, priceBookId: entity.priceBookId ?? null,
      codigoBarras: entity.codigoBarras || '', alerta: entity.alerta || '', observacao: entity.observacao || '',
      parecer: entity.parecer || '', textoTermoQuitacao: entity.textoTermoQuitacao || '',
      tratamento: entity.tratamento || '',
      tipoPessoa: (entity.pessoa.tipoPessoa || this.tipoPessoaFromTipoRegistro(entity.pessoa.tipoRegistro)) as 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA',
      genero: entity.pessoa.genero || '',
      nacionalidade: entity.pessoa.nacionalidade || '',
      naturalidade: entity.pessoa.naturalidade || '',
      estadoCivil: entity.pessoa.estadoCivil || '',
      dataNascimento: entity.pessoa.dataNascimento ? toDisplayDate(entity.pessoa.dataNascimento) : '',
      pessoaNome: entity.pessoa.nome, pessoaApelido: entity.pessoa.apelido || '',
      tipoRegistro: entity.pessoa.tipoRegistro, registroFederal: entity.pessoa.registroFederal
    });
    this.applyRegistroFederalMask();
  }

  private loadSubresources(): void {
    if (!this.entidadeId) return;
    this.loadingSubresources = true;
    this.service.loadRhOptions(this.tipoEntidadeId).subscribe({ next: r => this.rhOptions = r || this.rhOptions, error: () => {} });
    this.service.getDocumentacao(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: d => this.documentacaoForm.patchValue({
        rg: d.rg || '',
        rgTipo: d.rgTipo || '',
        rgDataEmissao: d.rgDataEmissao ? toDisplayDate(d.rgDataEmissao) : '',
        rgUfEmissao: d.rgUfEmissao || '',
        rntc: d.rntc || '',
        rntcCategoria: d.rntcCategoria || '',
        ctps: d.ctps || '',
        ctpsSerie: d.ctpsSerie || '',
        ctpsDataEmissao: d.ctpsDataEmissao ? toDisplayDate(d.ctpsDataEmissao) : '',
        ctpsUfEmissao: d.ctpsUfEmissao || '',
        pis: d.pis || '',
        tituloEleitor: d.tituloEleitor || '',
        tituloEleitorZona: d.tituloEleitorZona || '',
        tituloEleitorSecao: d.tituloEleitorSecao || '',
        cnh: d.cnh || '',
        cnhCategoria: d.cnhCategoria || '',
        cnhDataEmissao: d.cnhDataEmissao ? toDisplayDate(d.cnhDataEmissao) : '',
        cnhObservacao: d.cnhObservacao || '',
        militarNumero: d.militarNumero || '',
        militarSerie: d.militarSerie || '',
        militarCategoria: d.militarCategoria || '',
        registroEstadualContribuinte: d.registroEstadualContribuinte ?? null,
        numeroNif: d.numeroNif || '',
        motivoNaoNif: d.motivoNaoNif ?? null
      }),
      error: () => {}
    });
    this.service.getInfoComercial(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: c => this.comercialForm.patchValue({
        faturamentoDiaInicial: c.faturamentoDiaInicial ? toDisplayDate(c.faturamentoDiaInicial) : '',
        faturamentoDiaFinal: c.faturamentoDiaFinal ? toDisplayDate(c.faturamentoDiaFinal) : '',
        faturamentoDiasPrazo: c.faturamentoDiasPrazo ?? null,
        prazoEntregaDias: c.prazoEntregaDias ?? null,
        faturamentoFrequenciaCobrancaId: c.faturamentoFrequenciaCobrancaId ?? null,
        juroTaxaPadrao: c.juroTaxaPadrao ?? null,
        ramoAtividade: c.ramoAtividade || '',
        boletosEnviarEmail: !!c.boletosEnviarEmail,
        consumidorFinal: !!c.consumidorFinal
      }),
      error: () => {}
    });
    this.service.getDadosFiscais(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: f => this.fiscalForm.patchValue({ manifestarNotaAutomaticamente: f.manifestarNotaAutomaticamente ?? null, usaNotaFiscalFatura: f.usaNotaFiscalFatura ?? null, ignorarImportacaoNota: f.ignorarImportacaoNota ?? null }),
      error: () => {}
    });
    this.service.getContratoRh(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rh => this.rhContratoForm.patchValue({
        numero: rh.numero || '',
        admissaoData: rh.admissaoData ? toDisplayDate(rh.admissaoData) : '',
        remuneracao: rh.remuneracao ?? null,
        remuneracaoComplementar: rh.remuneracaoComplementar ?? null,
        bonificacao: rh.bonificacao ?? null,
        sindicalizado: !!rh.sindicalizado,
        percentualInsalubridade: rh.percentualInsalubridade ?? null,
        percentualPericulosidade: rh.percentualPericulosidade ?? null,
        tipoFuncionarioId: rh.tipoFuncionarioId ?? null,
        situacaoFuncionarioId: rh.situacaoFuncionarioId ?? null,
        setorId: rh.setorId ?? null,
        cargoId: rh.cargoId ?? null,
        ocupacaoAtividadeId: rh.ocupacaoAtividadeId ?? null
      }),
      error: () => {}
    });
    this.service.getInfoRh(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rh => this.rhInfoForm.patchValue({ atividades: rh.atividades || '', habilidades: rh.habilidades || '', experiencias: rh.experiencias || '', aceitaViajar: !!rh.aceitaViajar, possuiCarro: !!rh.possuiCarro, possuiMoto: !!rh.possuiMoto, metaMediaHorasVendidasDia: rh.metaMediaHorasVendidasDia ?? null, metaProdutosVendidos: rh.metaProdutosVendidos ?? null }),
      error: () => {}
    });
    this.loadEnderecos();
    this.loadContatos();
    this.loadFamiliares();
    this.loadReferencias();
    this.loadQualificacoes();
    this.loadingSubresources = false;
  }

  contatoFormas(contatoId: number): EntidadeContatoForma[] {
    return this.contatoFormasByContato[contatoId] || [];
  }

  formatTipoContato(tipo: string | null | undefined): string {
    const value = (tipo || '').trim();
    if (!value) return '-';
    const labels: Record<string, string> = {
      EMAIL: 'E-mail',
      TELEFONE: 'Telefone',
      FONE_CELULAR: 'Celular',
      FONE_RESIDENCIAL: 'Fone residencial',
      FONE_COMERCIAL: 'Fone comercial',
      WHATSAPP: 'WhatsApp',
      FACEBOOK: 'Facebook',
      SITE: 'Site',
      LINKEDIN: 'LinkedIn',
      INSTAGRAM: 'Instagram'
    };
    return labels[value] || value.replace(/_/g, ' ');
  }

  enderecoTipoIcon(tipo: string | null | undefined): string {
    const value = (tipo || '').trim();
    const icons: Record<string, string> = {
      RESIDENCIAL: 'home',
      COMERCIAL: 'storefront',
      ENTREGA: 'local_shipping',
      COBRANCA: 'receipt_long',
      CORRESPONDENCIA: 'mail',
      OUTRO: 'place'
    };
    return icons[value] || 'location_on';
  }

  contatoTipoIcon(tipo: string | null | undefined): string {
    const value = (tipo || '').trim();
    const icons: Record<string, string> = {
      EMAIL: 'mail',
      TELEFONE: 'phone',
      FONE_CELULAR: 'smartphone',
      FONE_RESIDENCIAL: 'call',
      FONE_COMERCIAL: 'business',
      WHATSAPP: 'chat',
      FACEBOOK: 'thumb_up',
      SITE: 'language',
      LINKEDIN: 'work',
      INSTAGRAM: 'photo_camera'
    };
    return icons[value] || 'contact_phone';
  }

  private loadEnderecos(): void {
    if (!this.entidadeId) return;
    this.service.listEnderecos(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rows => {
        this.enderecos = rows || [];
        this.enderecosCount = this.enderecos.length;
      },
      error: () => {
        this.enderecos = [];
        this.enderecosCount = 0;
      }
    });
  }

  private loadContatos(): void {
    if (!this.entidadeId) return;
    this.service.listContatos(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rows => {
        this.contatos = rows || [];
        this.contatoFormasByContato = {};
        this.contatoFormasFlat = [];
        this.contatosCount = 0;
        if (this.contatos.length) {
          this.contatos.forEach(contato => this.loadContatoFormas(contato.id));
        }
        if (!this.contatoFormaForm.value.contatoId && this.contatos.length) {
          const firstContato = this.contatos[0];
          if (firstContato) {
            this.contatoFormaForm.patchValue({ contatoId: firstContato.id });
          }
        }
      },
      error: () => {
        this.contatos = [];
        this.contatosCount = 0;
        this.contatoFormasByContato = {};
        this.contatoFormasFlat = [];
      }
    });
  }

  private loadContatoFormas(contatoId: number): void {
    if (!this.entidadeId) return;
    this.service.listContatoFormas(this.tipoEntidadeId, this.entidadeId, contatoId).subscribe({
      next: rows => {
        this.contatoFormasByContato[contatoId] = rows || [];
        this.rebuildContatoFormasFlat();
      },
      error: () => {
        this.contatoFormasByContato[contatoId] = [];
        this.rebuildContatoFormasFlat();
      }
    });
  }

  private rebuildContatoFormasFlat(): void {
    const rows: EntidadeContatoFormaFlat[] = [];
    this.contatos.forEach(contato => {
      const formas = this.contatoFormasByContato[contato.id] || [];
      formas.forEach(forma => {
        rows.push({
          ...forma,
          contatoNome: contato.nome || null,
          contatoCargo: contato.cargo || null
        });
      });
    });
    rows.sort((a, b) => (a.id || 0) - (b.id || 0));
    this.contatoFormasFlat = rows;
    this.contatosCount = rows.length;
  }

  private ensureContatoBaseId(onReady: (contatoId: number) => void): void {
    const existing = this.toNumber(this.contatoFormaForm.value.contatoId);
    if (existing) {
      onReady(existing);
      return;
    }
    const firstContato = this.contatos[0];
    if (firstContato?.id) {
      this.contatoFormaForm.patchValue({ contatoId: firstContato.id });
      onReady(firstContato.id);
      return;
    }
    if (!this.entidadeId) return;
    this.savingContato = true;
    this.service
      .createContato(this.tipoEntidadeId, this.entidadeId, { nome: null, cargo: null })
      .pipe(finalize(() => (this.savingContato = false)))
      .subscribe({
        next: contato => {
          this.contatos = [...this.contatos, contato];
          this.contatoFormaForm.patchValue({ contatoId: contato.id });
          onReady(contato.id);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel preparar o cadastro de contato.')
      });
  }

  private loadFamiliares(): void {
    if (!this.entidadeId) return;
    this.service.listFamiliares(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rows => {
        this.familiares = rows || [];
        this.familiaresCount = this.familiares.length;
      },
      error: () => {
        this.familiares = [];
        this.familiaresCount = 0;
      }
    });
  }

  private loadReferencias(): void {
    if (!this.entidadeId) return;
    this.service.listReferencias(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rows => {
        this.referencias = (rows || []).map(item => ({
          ...item,
          dataInicio: item.dataInicio ? toDisplayDate(item.dataInicio) : null,
          dataFim: item.dataFim ? toDisplayDate(item.dataFim) : null
        }));
        this.referenciasCount = this.referencias.length;
      },
      error: () => {
        this.referencias = [];
        this.referenciasCount = 0;
      }
    });
  }

  private loadQualificacoes(): void {
    if (!this.entidadeId) return;
    this.service.listQualificacoes(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: rows => {
        this.qualificacoes = rows || [];
        this.qualificacoesCount = this.qualificacoes.length;
      },
      error: () => {
        this.qualificacoes = [];
        this.qualificacoesCount = 0;
      }
    });
  }

  private loadFichaConfig(): void {
    const agrupadorId = this.contexto?.agrupadorId || null;
    this.formConfigByGroup = null;
    this.formGroupByKey = {};
    this.formFieldByKey = {};
    this.fieldGroupByKey = {};
    if (!agrupadorId) return;
    this.service.getFormConfigByGroup(this.tipoEntidadeId, agrupadorId).subscribe({
      next: cfg => {
        this.formConfigByGroup = cfg;
        const groups = cfg?.groups || [];
        groups.forEach(group => {
          const key = (group.groupKey || '').trim();
          if (key) this.formGroupByKey[key] = group;
          (group.fields || []).forEach(field => {
            const fieldKey = (field.fieldKey || '').trim();
            if (fieldKey) {
              this.formFieldByKey[fieldKey] = field;
              this.fieldGroupByKey[fieldKey] = key;
            }
          });
        });

        const currentTipoContato = (this.contatoFormaForm.value.tipoContato || 'EMAIL').trim();
        if (!this.isContatoFormaTipoVisible(currentTipoContato)) {
          const fallbackTipoContato = ['EMAIL', 'TELEFONE', 'WHATSAPP', 'SITE', 'LINKEDIN', 'INSTAGRAM', 'FACEBOOK', 'FONE_CELULAR', 'FONE_RESIDENCIAL', 'FONE_COMERCIAL']
            .find(tipo => this.isContatoFormaTipoVisible(tipo)) || 'EMAIL';
          this.contatoFormaForm.patchValue({ tipoContato: fallbackTipoContato }, { emitEvent: false });
        }
        this.applyFieldConfigRules();
      },
      error: () => {
        this.formConfigByGroup = null;
        this.formGroupByKey = {};
        this.formFieldByKey = {};
        this.fieldGroupByKey = {};
        this.applyFieldConfigRules();
      }
    });
  }

  private captureDefaultRequiredByFieldKey(): void {
    this.controlsByFieldKey().forEach((control, fieldKey) => {
      this.defaultRequiredByFieldKey[fieldKey] = control.hasValidator(Validators.required);
    });
  }

  private applyFieldConfigRules(): void {
    this.controlsByFieldKey().forEach((control, fieldKey) => {
      const fieldCfg = this.formFieldByKey[fieldKey];
      const defaultRequired = !!this.defaultRequiredByFieldKey[fieldKey];
      const shouldView = this.isFieldVisible(fieldKey);
      const groupKey = this.resolveGroupKeyForField(fieldKey);
      const fieldEditableByConfig = !fieldCfg || !!fieldCfg.editable;
      const fieldEditableByAccess = this.hasFieldEditAccess(fieldKey);
      const groupEditableByAccess = groupKey ? this.hasGroupEditAccess(groupKey) : true;
      const shouldRequire = shouldView && (fieldCfg ? !!fieldCfg.required : defaultRequired);
      const shouldEdit = shouldView && this.canEdit() && fieldEditableByConfig && fieldEditableByAccess && groupEditableByAccess;

      if (shouldRequire) control.addValidators(Validators.required);
      else control.removeValidators(Validators.required);
      control.updateValueAndValidity({ emitEvent: false });

      if (shouldEdit) {
        if (control.disabled) control.enable({ emitEvent: false });
      } else if (control.enabled) {
        control.disable({ emitEvent: false });
      }
    });
  }

  private controlsByFieldKey(): Map<string, AbstractControl> {
    const map = new Map<string, AbstractControl>();
    const push = (fieldKey: string, control: AbstractControl | null) => { if (control) map.set(fieldKey, control); };
    push('registroFederal', this.form.get('registroFederal'));
    push('tipoPessoa', this.form.get('tipoPessoa'));
    push('pessoaNome', this.form.get('pessoaNome'));
    push('pessoaApelido', this.form.get('pessoaApelido'));
    push('genero', this.form.get('genero'));
    push('nacionalidade', this.form.get('nacionalidade'));
    push('naturalidade', this.form.get('naturalidade'));
    push('estadoCivil', this.form.get('estadoCivil'));
    push('dataNascimento', this.form.get('dataNascimento'));
    push('codigoBarras', this.form.get('codigoBarras'));
    push('tratamento', this.form.get('tratamento'));
    push('alerta', this.form.get('alerta'));
    push('observacao', this.form.get('observacao'));
    push('parecer', this.form.get('parecer'));
    push('textoTermoQuitacao', this.form.get('textoTermoQuitacao'));
    push('contatos.tipoContato', this.contatoFormaForm.get('tipoContato'));
    push('contatos.valor', this.contatoFormaForm.get('valor'));
    push('contatos.preferencial', this.contatoFormaForm.get('preferencial'));
    push('contatos.formas.EMAIL', this.contatoFormaForm.get('valor'));
    push('contatos.formas.TELEFONE', this.contatoFormaForm.get('valor'));
    push('contatos.formas.FONE_CELULAR', this.contatoFormaForm.get('valor'));
    push('contatos.formas.FONE_RESIDENCIAL', this.contatoFormaForm.get('valor'));
    push('contatos.formas.FONE_COMERCIAL', this.contatoFormaForm.get('valor'));
    push('contatos.formas.WHATSAPP', this.contatoFormaForm.get('valor'));
    push('contatos.formas.FACEBOOK', this.contatoFormaForm.get('valor'));
    push('contatos.formas.SITE', this.contatoFormaForm.get('valor'));
    push('contatos.formas.LINKEDIN', this.contatoFormaForm.get('valor'));
    push('contatos.formas.INSTAGRAM', this.contatoFormaForm.get('valor'));
    push('enderecos.enderecoTipo', this.enderecoForm.get('enderecoTipo'));
    push('enderecos.cep', this.enderecoForm.get('cep'));
    push('enderecos.pais', this.enderecoForm.get('pais'));
    push('enderecos.paisCodigoIbge', this.enderecoForm.get('paisCodigoIbge'));
    push('enderecos.uf', this.enderecoForm.get('uf'));
    push('enderecos.ufCodigoIbge', this.enderecoForm.get('ufCodigoIbge'));
    push('enderecos.municipio', this.enderecoForm.get('municipio'));
    push('enderecos.municipioCodigoIbge', this.enderecoForm.get('municipioCodigoIbge'));
    push('enderecos.bairro', this.enderecoForm.get('bairro'));
    push('enderecos.logradouro', this.enderecoForm.get('logradouro'));
    push('enderecos.numero', this.enderecoForm.get('numero'));
    push('enderecos.complemento', this.enderecoForm.get('complemento'));
    push('enderecos.estadoProvinciaRegiaoEstrangeiro', this.enderecoForm.get('estadoProvinciaRegiaoEstrangeiro'));
    push('enderecos.latitude', this.enderecoForm.get('latitude'));
    push('enderecos.longitude', this.enderecoForm.get('longitude'));
    push('enderecos.principal', this.enderecoForm.get('principal'));
    push('documentacao.rg', this.documentacaoForm.get('rg'));
    push('documentacao.rgTipo', this.documentacaoForm.get('rgTipo'));
    push('documentacao.rgDataEmissao', this.documentacaoForm.get('rgDataEmissao'));
    push('documentacao.rgUfEmissao', this.documentacaoForm.get('rgUfEmissao'));
    push('documentacao.rntc', this.documentacaoForm.get('rntc'));
    push('documentacao.rntcCategoria', this.documentacaoForm.get('rntcCategoria'));
    push('documentacao.ctps', this.documentacaoForm.get('ctps'));
    push('documentacao.ctpsSerie', this.documentacaoForm.get('ctpsSerie'));
    push('documentacao.ctpsDataEmissao', this.documentacaoForm.get('ctpsDataEmissao'));
    push('documentacao.ctpsUfEmissao', this.documentacaoForm.get('ctpsUfEmissao'));
    push('documentacao.pis', this.documentacaoForm.get('pis'));
    push('documentacao.tituloEleitor', this.documentacaoForm.get('tituloEleitor'));
    push('documentacao.tituloEleitorZona', this.documentacaoForm.get('tituloEleitorZona'));
    push('documentacao.tituloEleitorSecao', this.documentacaoForm.get('tituloEleitorSecao'));
    push('documentacao.cnh', this.documentacaoForm.get('cnh'));
    push('documentacao.cnhCategoria', this.documentacaoForm.get('cnhCategoria'));
    push('documentacao.cnhDataEmissao', this.documentacaoForm.get('cnhDataEmissao'));
    push('documentacao.cnhObservacao', this.documentacaoForm.get('cnhObservacao'));
    push('documentacao.militarNumero', this.documentacaoForm.get('militarNumero'));
    push('documentacao.militarSerie', this.documentacaoForm.get('militarSerie'));
    push('documentacao.militarCategoria', this.documentacaoForm.get('militarCategoria'));
    push('documentacao.numeroNif', this.documentacaoForm.get('numeroNif'));
    push('documentacao.motivoNaoNif', this.documentacaoForm.get('motivoNaoNif'));
    push('documentacao.registroEstadualContribuinte', this.documentacaoForm.get('registroEstadualContribuinte'));
    push('priceBookId', this.form.get('priceBookId'));
    push('comercial.ramoAtividade', this.comercialForm.get('ramoAtividade'));
    push('comercial.faturamentoFrequenciaCobrancaId', this.comercialForm.get('faturamentoFrequenciaCobrancaId'));
    push('comercial.faturamentoDiaInicial', this.comercialForm.get('faturamentoDiaInicial'));
    push('comercial.faturamentoDiaFinal', this.comercialForm.get('faturamentoDiaFinal'));
    push('comercial.faturamentoDiasPrazo', this.comercialForm.get('faturamentoDiasPrazo'));
    push('comercial.prazoEntregaDias', this.comercialForm.get('prazoEntregaDias'));
    push('comercial.juroTaxaPadrao', this.comercialForm.get('juroTaxaPadrao'));
    push('comercial.consumidorFinal', this.comercialForm.get('consumidorFinal'));
    push('comercial.boletosEnviarEmail', this.comercialForm.get('boletosEnviarEmail'));
    push('fiscal.manifestarNotaAutomaticamente', this.fiscalForm.get('manifestarNotaAutomaticamente'));
    push('fiscal.usaNotaFiscalFatura', this.fiscalForm.get('usaNotaFiscalFatura'));
    push('fiscal.ignorarImportacaoNota', this.fiscalForm.get('ignorarImportacaoNota'));
    push('rh.contrato.numero', this.rhContratoForm.get('numero'));
    push('rh.contrato.admissaoData', this.rhContratoForm.get('admissaoData'));
    push('rh.contrato.remuneracao', this.rhContratoForm.get('remuneracao'));
    push('rh.contrato.remuneracaoComplementar', this.rhContratoForm.get('remuneracaoComplementar'));
    push('rh.contrato.bonificacao', this.rhContratoForm.get('bonificacao'));
    push('rh.contrato.sindicalizado', this.rhContratoForm.get('sindicalizado'));
    push('rh.contrato.percentualInsalubridade', this.rhContratoForm.get('percentualInsalubridade'));
    push('rh.contrato.percentualPericulosidade', this.rhContratoForm.get('percentualPericulosidade'));
    push('rh.contrato.tipoFuncionarioId', this.rhContratoForm.get('tipoFuncionarioId'));
    push('rh.contrato.situacaoFuncionarioId', this.rhContratoForm.get('situacaoFuncionarioId'));
    push('rh.contrato.setorId', this.rhContratoForm.get('setorId'));
    push('rh.contrato.cargoId', this.rhContratoForm.get('cargoId'));
    push('rh.contrato.ocupacaoAtividadeId', this.rhContratoForm.get('ocupacaoAtividadeId'));
    push('rh.info.atividades', this.rhInfoForm.get('atividades'));
    push('rh.info.habilidades', this.rhInfoForm.get('habilidades'));
    push('rh.info.experiencias', this.rhInfoForm.get('experiencias'));
    push('rh.info.aceitaViajar', this.rhInfoForm.get('aceitaViajar'));
    push('rh.info.possuiCarro', this.rhInfoForm.get('possuiCarro'));
    push('rh.info.possuiMoto', this.rhInfoForm.get('possuiMoto'));
    push('rh.info.metaMediaHorasVendidasDia', this.rhInfoForm.get('metaMediaHorasVendidasDia'));
    push('rh.info.metaProdutosVendidos', this.rhInfoForm.get('metaProdutosVendidos'));
    push('familiares.nome', this.familiarForm.get('nome'));
    push('familiares.parentesco', this.familiarForm.get('parentesco'));
    push('familiares.dependente', this.familiarForm.get('dependente'));
    push('referencias.nome', this.referenciaForm.get('nome'));
    push('referencias.atividades', this.referenciaForm.get('atividades'));
    push('referencias.dataInicio', this.referenciaForm.get('dataInicio'));
    push('referencias.dataFim', this.referenciaForm.get('dataFim'));
    push('qualificacoes.rhQualificacaoId', this.qualificacaoForm.get('rhQualificacaoId'));
    push('qualificacoes.completo', this.qualificacaoForm.get('completo'));
    push('qualificacoes.tipo', this.qualificacaoForm.get('tipo'));
    return map;
  }

  private resolveGroupKeyForField(fieldKey: string): string | null {
    const normalizedFieldKey = (fieldKey || '').trim();
    if (!normalizedFieldKey) return null;
    const configured = this.fieldGroupByKey[normalizedFieldKey];
    if (configured) return configured;

    if (normalizedFieldKey.startsWith('enderecos.')) return 'ENDERECOS';
    if (normalizedFieldKey.startsWith('contatos.')) return 'CONTATOS';
    if (normalizedFieldKey.startsWith('documentacao.')) {
      if (normalizedFieldKey === 'documentacao.registroEstadualContribuinte') return 'COMERCIAL_FISCAL';
      return 'DOCUMENTACAO';
    }
    if (normalizedFieldKey === 'priceBookId' || normalizedFieldKey.startsWith('comercial.') || normalizedFieldKey.startsWith('fiscal.')) {
      return 'COMERCIAL_FISCAL';
    }
    if (normalizedFieldKey.startsWith('rh.')) return 'RH';
    if (normalizedFieldKey.startsWith('familiares.') || normalizedFieldKey.startsWith('referencias.') || normalizedFieldKey.startsWith('qualificacoes.')) {
      return 'FAMILIARES_REFERENCIAS';
    }
    if (
      normalizedFieldKey === 'registroFederal'
      || normalizedFieldKey === 'tipoPessoa'
      || normalizedFieldKey === 'pessoaNome'
      || normalizedFieldKey === 'pessoaApelido'
      || normalizedFieldKey === 'genero'
      || normalizedFieldKey === 'nacionalidade'
      || normalizedFieldKey === 'naturalidade'
      || normalizedFieldKey === 'estadoCivil'
      || normalizedFieldKey === 'dataNascimento'
      || normalizedFieldKey === 'codigoBarras'
      || normalizedFieldKey === 'tratamento'
      || normalizedFieldKey === 'alerta'
      || normalizedFieldKey === 'observacao'
      || normalizedFieldKey === 'parecer'
      || normalizedFieldKey === 'textoTermoQuitacao'
    ) return 'DADOS_ENTIDADE';

    return null;
  }

  private hasGroupViewAccess(groupKey: string): boolean {
    return this.accessControl.can(`entities.form.group.${(groupKey || '').trim()}.view`, ['MASTER', 'ADMIN']);
  }

  private hasGroupEditAccess(groupKey: string): boolean {
    return this.accessControl.can(`entities.form.group.${(groupKey || '').trim()}.edit`, ['MASTER', 'ADMIN']);
  }

  private hasFieldViewAccess(fieldKey: string): boolean {
    return this.accessControl.can(`entities.form.field.${(fieldKey || '').trim()}.view`, ['MASTER', 'ADMIN']);
  }

  private hasFieldEditAccess(fieldKey: string): boolean {
    return this.accessControl.can(`entities.form.field.${(fieldKey || '').trim()}.edit`, ['MASTER', 'ADMIN']);
  }

  private validateDocumentacaoDates(): boolean {
    const raw = this.documentacaoForm.getRawValue();
    const checks: Array<{ value: string; label: string }> = [
      { value: (raw.rgDataEmissao || '').trim(), label: 'Data expedicao do RG' },
      { value: (raw.ctpsDataEmissao || '').trim(), label: 'Data emissao da CTPS' },
      { value: (raw.cnhDataEmissao || '').trim(), label: 'Data emissao da CNH' }
    ];
    for (const item of checks) {
      if (item.value && !isValidDateInput(item.value)) {
        this.notify.error(`${item.label} invalida. Use DD/MM/AAAA.`);
        return false;
      }
    }
    return true;
  }

  private validateComercialDates(): boolean {
    const raw = this.comercialForm.getRawValue();
    const inicio = (raw.faturamentoDiaInicial || '').trim();
    const fim = (raw.faturamentoDiaFinal || '').trim();
    if (inicio && !isValidDateInput(inicio)) {
      this.notify.error('Faturamento data inicial invalida. Use DD/MM/AAAA.');
      return false;
    }
    if (fim && !isValidDateInput(fim)) {
      this.notify.error('Faturamento data final invalida. Use DD/MM/AAAA.');
      return false;
    }
    if (inicio && fim) {
      const inicioIso = toIsoDate(inicio);
      const fimIso = toIsoDate(fim);
      if (inicioIso > fimIso) {
        this.notify.error('Periodo de faturamento invalido: data inicial maior que data final.');
        return false;
      }
    }
    return true;
  }

  private buildDocumentacaoPayload(): EntidadeDocumentacaoPayload {
    const raw = this.documentacaoForm.getRawValue();
    const root = this.form.getRawValue();
    const tipoRegistro = (root.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    const rgData = (raw.rgDataEmissao || '').trim();
    const ctpsData = (raw.ctpsDataEmissao || '').trim();
    const cnhData = (raw.cnhDataEmissao || '').trim();

    return {
      tipoRegistroFederal: tipoRegistro,
      registroFederal: this.normalizeRegistroFederal(tipoRegistro, root.registroFederal || ''),
      rg: (raw.rg || '').trim() || null,
      rgTipo: (raw.rgTipo || '').trim() || null,
      rgDataEmissao: rgData ? toIsoDate(rgData) : null,
      rgUfEmissao: (raw.rgUfEmissao || '').trim() || null,
      rntc: (raw.rntc || '').trim() || null,
      rntcCategoria: (raw.rntcCategoria || '').trim() || null,
      ctps: (raw.ctps || '').trim() || null,
      ctpsSerie: (raw.ctpsSerie || '').trim() || null,
      ctpsDataEmissao: ctpsData ? toIsoDate(ctpsData) : null,
      ctpsUfEmissao: (raw.ctpsUfEmissao || '').trim() || null,
      pis: (raw.pis || '').trim() || null,
      tituloEleitor: (raw.tituloEleitor || '').trim() || null,
      tituloEleitorZona: (raw.tituloEleitorZona || '').trim() || null,
      tituloEleitorSecao: (raw.tituloEleitorSecao || '').trim() || null,
      cnh: (raw.cnh || '').trim() || null,
      cnhCategoria: (raw.cnhCategoria || '').trim() || null,
      cnhDataEmissao: cnhData ? toIsoDate(cnhData) : null,
      cnhObservacao: (raw.cnhObservacao || '').trim() || null,
      militarNumero: (raw.militarNumero || '').trim() || null,
      militarSerie: (raw.militarSerie || '').trim() || null,
      militarCategoria: (raw.militarCategoria || '').trim() || null,
      registroEstadualContribuinte: raw.registroEstadualContribuinte,
      numeroNif: (raw.numeroNif || '').trim() || null,
      motivoNaoNif: raw.motivoNaoNif ?? null
    };
  }

  private toNumber(value: unknown): number | null {
    const n = Number(value); return Number.isFinite(n) && n > 0 ? n : null;
  }
  private toNonNegativeInteger(value: unknown): number | null {
    if (value === null || value === undefined || `${value}`.trim() === '') return null;
    const n = Number(value);
    if (!Number.isFinite(n)) return null;
    const integer = Math.trunc(n);
    return integer >= 0 ? integer : null;
  }
  private toFloat(value: unknown): number | null {
    if (value === null || value === undefined || `${value}`.trim() === '') return null;
    const n = Number(value); return Number.isFinite(n) ? n : null;
  }
  private toSmallInt(value: unknown): number | null {
    const n = this.toFloat(value); if (n === null) return null; const i = Math.trunc(n); return i >= 0 && i <= 2 ? i : null;
  }
  private toDecimal(value: unknown): number | null {
    if (value === null || value === undefined) return null;
    const raw = `${value}`.trim();
    if (!raw) return null;
    const normalized = raw.replace(',', '.');
    const n = Number(normalized);
    return Number.isFinite(n) ? n : null;
  }
  private hasEmpresaContext(): boolean { return !!(localStorage.getItem('empresaContextId') || '').trim(); }
  private loadPriceBooks(): void { this.pricingService.listBooks().subscribe({ next: rows => this.priceBooks = rows || [], error: () => this.priceBooks = [] }); }
  private normalizeRegistroFederal(tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO', value: string): string {
    const trimmed = (value || '').toString().trim();
    return tipoRegistro === 'CPF' || tipoRegistro === 'CNPJ' ? normalizeCpfCnpj(trimmed) : trimmed;
  }

  private setupSubresourceValidationAndSearch(): void {
    this.applyContatoFormaValorValidators();
    this.contatoFormaForm.controls.tipoContato.valueChanges.subscribe(() => this.applyContatoFormaValorValidators());
    this.form.controls.registroFederal.valueChanges
      .pipe(debounceTime(200), distinctUntilChanged())
      .subscribe(value => {
        let tipoEfetivo = (this.form.controls.tipoRegistro.value || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
        if (this.tipoRegistroManualOverride) {
          if (!(value || '').toString().trim()) this.tipoRegistroManualOverride = false;
        } else {
          const nextTipo = this.inferTipoRegistro(value || '');
          this.form.controls.tipoRegistro.setValue(nextTipo, { emitEvent: false });
          this.form.controls.tipoPessoa.setValue(this.tipoPessoaFromTipoRegistro(nextTipo), { emitEvent: false });
          tipoEfetivo = nextTipo;
        }
        this.tryAutofillFromRegistroFederal(value || '', tipoEfetivo);
      });

    this.enderecoForm.controls.cep.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(value => this.lookupCepAndPatch(value || ''));
  }

  private applyContatoFormaValorValidators(): void {
    const tipo = this.contatoFormaForm.value.tipoContato || 'EMAIL';
    const control = this.contatoFormaForm.controls.valor;
    if (tipo === 'EMAIL') {
      control.setValidators([Validators.required, Validators.maxLength(255), Validators.email]);
    } else if (tipo === 'TELEFONE' || tipo === 'FONE_CELULAR' || tipo === 'FONE_RESIDENCIAL' || tipo === 'FONE_COMERCIAL' || tipo === 'WHATSAPP') {
      control.setValidators([
        Validators.required,
        Validators.maxLength(255),
        Validators.pattern(/^(\+?\d{10,15}|\(?\d{2}\)?\s?\d{4,5}-?\d{4})$/)
      ]);
    } else {
      control.setValidators([Validators.required, Validators.maxLength(255)]);
    }
    control.updateValueAndValidity({ emitEvent: false });
  }

  private inferTipoRegistro(value: string): 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO' {
    const raw = (value || '').trim();
    if (!raw) return 'CPF';
    const hasNonDocumentChar = /[^0-9.\-\/()\s]/.test(raw);
    if (hasNonDocumentChar) return 'ID_ESTRANGEIRO';
    const digits = raw.replace(/\D/g, '');
    if (digits.length <= 11) return 'CPF';
    if (digits.length <= 14) return 'CNPJ';
    return 'ID_ESTRANGEIRO';
  }

  private tryAutofillFromRegistroFederal(rawValue: string, tipo: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO'): void {
    const normalized = this.normalizeRegistroFederal(tipo, rawValue || '');
    const minLength = tipo === 'CPF' ? 11 : tipo === 'CNPJ' ? 14 : 4;
    if (!normalized || normalized.length < minLength) {
      this.lastRegistroLookupKey = '';
      return;
    }

    const lookupKey = `${tipo}:${normalized}`;
    if (lookupKey === this.lastRegistroLookupKey || this.lookingUpRegistroFederal) return;
    this.lastRegistroLookupKey = lookupKey;
    this.lookingUpRegistroFederal = true;

    this.service
      .findPessoaByDocumento(normalized)
      .pipe(
        finalize(() => (this.lookingUpRegistroFederal = false)),
        catchError(() => of(null as PessoaLookupResponse | null))
      )
      .subscribe(pessoa => {
        if (!pessoa) return;
        const tipoFromPessoa = ((pessoa.tipoRegistro || tipo) as string).toUpperCase();
        const tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO' =
          tipoFromPessoa === 'CNPJ'
            ? 'CNPJ'
            : tipoFromPessoa === 'ID_ESTRANGEIRO'
              ? 'ID_ESTRANGEIRO'
              : 'CPF';
        this.form.patchValue(
          {
            pessoaNome: pessoa.nome || this.form.value.pessoaNome || '',
            pessoaApelido: pessoa.apelido || '',
            tipoRegistro,
            tipoPessoa: ((pessoa.tipoPessoa || this.tipoPessoaFromTipoRegistro(tipoRegistro)) as 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA')
          },
          { emitEvent: false }
        );
        this.applyRegistroFederalMask();
      });
  }

  onTipoRegistroManualChange(nextTipo: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO'): void {
    this.tipoRegistroManualOverride = true;
    this.form.controls.tipoRegistro.setValue(nextTipo, { emitEvent: false });
    this.form.controls.tipoPessoa.setValue(this.tipoPessoaFromTipoRegistro(nextTipo), { emitEvent: false });
    this.applyRegistroFederalMask();
  }

  onTipoPessoaChange(nextTipoPessoa: 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA'): void {
    this.form.controls.tipoPessoa.setValue(nextTipoPessoa, { emitEvent: false });
    const tipoRegistro = this.tipoRegistroFromTipoPessoa(nextTipoPessoa);
    this.tipoRegistroManualOverride = true;
    this.form.controls.tipoRegistro.setValue(tipoRegistro, { emitEvent: false });
    this.applyRegistroFederalMask();
  }

  private applyRegistroFederalMask(): void {
    const tipo = (this.form.controls.tipoRegistro.value || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    const current = (this.form.controls.registroFederal.value || '').toString();
    const formatted = this.formatRegistroFederalByTipo(tipo, current);
    if (formatted !== current) {
      this.form.controls.registroFederal.setValue(formatted, { emitEvent: false });
    }
  }

  private formatRegistroFederalByTipo(tipo: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO', value: string): string {
    const raw = (value || '').toString();
    if (tipo === 'ID_ESTRANGEIRO') return raw.replace(/[.\-\/()\s]/g, '').slice(0, 30);
    const digits = raw.replace(/\D/g, '');
    if (tipo === 'CPF') {
      const d = digits.slice(0, 11);
      if (d.length <= 3) return d;
      if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
      if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
      return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
    }
    const d = digits.slice(0, 14);
    if (d.length <= 2) return d;
    if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`;
    if (d.length <= 8) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`;
    if (d.length <= 12) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8)}`;
    return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
  }

  private tipoPessoaFromTipoRegistro(tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO'): 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA' {
    if (tipoRegistro === 'CNPJ') return 'JURIDICA';
    if (tipoRegistro === 'ID_ESTRANGEIRO') return 'ESTRANGEIRA';
    return 'FISICA';
  }

  private tipoRegistroFromTipoPessoa(tipoPessoa: 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA'): 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO' {
    if (tipoPessoa === 'JURIDICA') return 'CNPJ';
    if (tipoPessoa === 'ESTRANGEIRA') return 'ID_ESTRANGEIRO';
    return 'CPF';
  }

  private lookupCepAndPatch(rawCep: string): void {
    const digits = (rawCep || '').replace(/\D/g, '');
    if (digits.length !== 8 || this.lookingUpCep) return;
    this.lookingUpCep = true;
    this.service.lookupCep(digits)
      .pipe(finalize(() => (this.lookingUpCep = false)))
      .subscribe({
        next: (cepData: CepLookupResponse) => {
          this.enderecoForm.patchValue({
            pais: (this.enderecoForm.value.pais || '').trim() || 'Brasil',
            paisCodigoIbge: this.enderecoForm.value.paisCodigoIbge || 1058,
            uf: (cepData.uf || '').toUpperCase(),
            ufCodigoIbge: (cepData.ufCodigoIbge || (cepData.ibge || '').replace(/\D/g, '').slice(0, 2)) || '',
            municipio: cepData.localidade || '',
            municipioCodigoIbge: cepData.ibge || '',
            bairro: cepData.bairro || '',
            logradouro: cepData.logradouro || ''
          });
        },
        error: () => {}
      });
  }

  openEnderecoGoogleMaps(): void {
    const raw = this.enderecoForm.getRawValue();
    const latitude = this.toDecimal(raw.latitude);
    const longitude = this.toDecimal(raw.longitude);
    if (latitude !== null && longitude !== null) {
      const url = `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;
      window.open(url, '_blank', 'noopener,noreferrer');
      return;
    }

    const queryParts = [
      (raw.logradouro || '').trim(),
      (raw.numero || '').trim(),
      (raw.bairro || '').trim(),
      (raw.municipio || '').trim(),
      (raw.uf || '').trim(),
      (raw.pais || '').trim(),
      (raw.cep || '').trim()
    ].filter(Boolean);

    if (!queryParts.length) {
      this.notify.error('Informe endereco ou latitude/longitude para abrir no mapa.');
      return;
    }
    const query = encodeURIComponent(queryParts.join(', '));
    const url = `https://www.google.com/maps/search/?api=1&query=${query}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  toggleEnderecoMapPreview(): void {
    if (this.isMobileView) {
      this.openEnderecoGoogleMaps();
      return;
    }
    if (this.mapPreviewOpen) {
      this.closeEnderecoMapPreview();
      return;
    }
    const prepared = this.prepareEnderecoMapPreview();
    if (!prepared) return;
    this.mapPreviewOpen = true;
  }

  closeEnderecoMapPreview(): void {
    this.mapPreviewOpen = false;
    this.mapPreviewUrl = null;
  }

  private prepareEnderecoMapPreview(): boolean {
    const raw = this.enderecoForm.getRawValue();
    const latitude = this.toDecimal(raw.latitude);
    const longitude = this.toDecimal(raw.longitude);

    const titleParts = [
      (raw.logradouro || '').trim(),
      (raw.numero || '').trim()
    ].filter(Boolean);
    this.mapPreviewTitle = titleParts.join(', ') || 'Endereco sem logradouro';
    this.mapPreviewSubtitle = [(raw.municipio || '').trim(), (raw.uf || '').trim()].filter(Boolean).join(' - ');

    let queryValue = '';
    if (latitude !== null && longitude !== null) {
      queryValue = `${latitude},${longitude}`;
    } else {
      const queryParts = [
        (raw.logradouro || '').trim(),
        (raw.numero || '').trim(),
        (raw.bairro || '').trim(),
        (raw.municipio || '').trim(),
        (raw.uf || '').trim(),
        (raw.pais || '').trim(),
        (raw.cep || '').trim()
      ].filter(Boolean);
      if (!queryParts.length) {
        this.notify.error('Informe endereco ou latitude/longitude para abrir o mapa.');
        return false;
      }
      queryValue = queryParts.join(', ');
    }

    const embedUrl = `https://www.google.com/maps?q=${encodeURIComponent(queryValue)}&output=embed`;
    this.mapPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
    return true;
  }
}
