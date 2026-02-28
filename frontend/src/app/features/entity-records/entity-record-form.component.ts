import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Subject, of } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { isValidDateInput, toDisplayDate, toIsoDate } from '../../shared/date-utils';
import { EntityTypeService } from '../entity-types/entity-type.service';
import { EntityTypeAccessService } from './entity-type-access.service';
import { CatalogPricingService, PriceBook } from '../catalog/catalog-pricing.service';
import { normalizeCpfCnpj } from '../../shared/cpf-cnpj.validator';
import {
  EntityRecordService,
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
    MatSlideToggleModule,
    DateMaskDirective,
    InlineLoaderComponent
  ],
  templateUrl: './entity-record-form.component.html',
  styleUrls: ['./entity-record-form.component.css']
})
export class EntityRecordFormComponent implements OnInit {
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
  defaultRequiredByFieldKey: Record<string, boolean> = {};
  referencias: EntidadeReferencia[] = [];
  qualificacoes: EntidadeQualificacaoItem[] = [];
  enderecos: EntidadeEndereco[] = [];
  contatos: EntidadeContato[] = [];
  contatoFormasByContato: Record<number, EntidadeContatoForma[]> = {};
  familiares: EntidadeFamiliar[] = [];
  familiarSearchResults: RegistroEntidade[] = [];
  familiarSelecionadoLabel = '';
  savingReferencia = false;
  savingQualificacao = false;
  savingEndereco = false;
  savingContato = false;
  savingContatoForma = false;
  savingFamiliar = false;
  searchingFamiliar = false;
  private familiarSearch$ = new Subject<string>();
  editingEnderecoId: number | null = null;
  editingContatoId: number | null = null;
  editingContatoFormaId: number | null = null;
  editingContatoFormaContatoId: number | null = null;
  editingFamiliarId: number | null = null;

  form = this.fb.group({
    ativo: [true, Validators.required],
    priceBookId: [null as number | null],
    tratamentoId: [null as number | null],
    codigoBarras: ['', [Validators.maxLength(60)]],
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
    tipoRegistroFederal: ['CPF'],
    registroFederal: [''],
    rg: [''],
    rgUfEmissao: [''],
    cnh: [''],
    numeroNif: ['']
  });
  comercialForm = this.fb.group({
    faturamentoDiasPrazo: [null as number | null],
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
    nome: ['', [Validators.maxLength(120)]],
    enderecoTipo: ['RESIDENCIAL', Validators.required],
    cep: ['', [Validators.maxLength(9), Validators.pattern(/^$|^\d{5}-?\d{3}$/)]],
    uf: ['', [Validators.maxLength(2), Validators.pattern(/^$|^[A-Za-z]{2}$/)]],
    municipio: ['', [Validators.maxLength(120)]],
    logradouro: ['', [Validators.maxLength(160)]],
    numero: ['', [Validators.maxLength(20)]],
    complemento: ['', [Validators.maxLength(120)]],
    principal: [false]
  });
  contatoForm = this.fb.group({
    nome: ['', [Validators.maxLength(160)]],
    cargo: ['', [Validators.maxLength(100)]]
  });
  contatoFormaForm = this.fb.group({
    contatoId: [null as number | null, Validators.required],
    tipoContato: ['EMAIL', Validators.required],
    valor: ['', [Validators.required, Validators.maxLength(255)]],
    preferencial: [false]
  });
  familiarForm = this.fb.group({
    entidadeParenteBusca: [''],
    entidadeParenteId: [null as number | null, Validators.required],
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
    private typeService: EntityTypeService,
    private typeAccess: EntityTypeAccessService
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
  }

  canEdit(): boolean { return this.mode !== 'view' && !!this.contexto?.vinculado; }
  toEdit(): void { if (this.entidadeId) this.router.navigate(['/entities', this.entidadeId, 'edit'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } }); }
  back(): void { this.router.navigate(['/entities'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId || null } }); }
  cadastroTitle(): string { const n = (this.tipoNome || '').trim(); return n ? `Cadastro ${n}` : 'Cadastro de Entidades'; }
  ativoHeaderLabel(): string { return this.form.value.ativo ? 'Ativo' : 'Inativo'; }
  setAtivoFromHeader(nextValue: boolean): void { this.form.controls.ativo.setValue(!!nextValue); }

  save(): void {
    if (!this.canEdit()) return;
    const raw = this.form.getRawValue();
    const tipoRegistro = (raw.tipoRegistro || 'CPF') as 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    const payload: RegistroEntidadePayload = {
      grupoEntidadeId: this.mode === 'new' ? this.selectedGrupoIdFromRoute : this.currentGrupoEntidadeId,
      priceBookId: raw.priceBookId ?? null,
      tratamentoId: raw.tratamentoId ?? null,
      codigoBarras: (raw.codigoBarras || '').trim() || null,
      alerta: (raw.alerta || '').trim() || null,
      observacao: (raw.observacao || '').trim() || null,
      parecer: (raw.parecer || '').trim() || null,
      textoTermoQuitacao: (raw.textoTermoQuitacao || '').trim() || null,
      version: this.currentVersion, ativo: !!raw.ativo,
      pessoa: {
        nome: (raw.pessoaNome || '').trim(),
        apelido: (raw.pessoaApelido || '').trim() || undefined,
        tipoRegistro, registroFederal: this.normalizeRegistroFederal(tipoRegistro, raw.registroFederal || '')
      }
    };
    this.saving = true;
    const obs = this.mode === 'new'
      ? this.service.create(this.tipoEntidadeId, payload)
      : this.service.update(this.tipoEntidadeId, this.entidadeId!, payload);
    obs.pipe(finalize(() => (this.saving = false))).subscribe({
      next: row => {
        this.notify.success(this.mode === 'new' ? 'Entidade criada.' : 'Entidade atualizada.');
        if (this.mode === 'new') this.router.navigate(['/entities', row.id, 'edit'], { queryParams: { tipoEntidadeId: this.tipoEntidadeId } });
        this.currentVersion = row.version ?? this.currentVersion;
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar entidade.')
    });
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
    if (!this.canEdit() || !this.entidadeId) return;
    const raw = this.documentacaoForm.getRawValue();
    const payload: EntidadeDocumentacaoPayload = {
      tipoRegistroFederal: (raw.tipoRegistroFederal || 'CPF') as any,
      registroFederal: (raw.registroFederal || '').trim(),
      rg: (raw.rg || '').trim() || null,
      rgUfEmissao: (raw.rgUfEmissao || '').trim() || null,
      cnh: (raw.cnh || '').trim() || null,
      numeroNif: (raw.numeroNif || '').trim() || null
    };
    this.service.upsertDocumentacao(this.tipoEntidadeId, this.entidadeId, payload).subscribe({
      next: () => this.notify.success('Documentacao salva.'),
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar documentacao.')
    });
  }

  saveComercial(): void {
    if (!this.canEdit() || !this.entidadeId) return;
    const raw = this.comercialForm.getRawValue();
    const payload: EntidadeInfoComercialPayload = {
      faturamentoDiasPrazo: this.toNumber(raw.faturamentoDiasPrazo),
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
    if (!this.canEdit() || !this.entidadeId) return;
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
    if (!this.canEdit() || !this.entidadeId) return;
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
    if (!this.canEdit() || !this.entidadeId) return;
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
    if (!this.canEdit() || !this.entidadeId) return;
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
    this.service.createReferencia(this.tipoEntidadeId, this.entidadeId, payload)
      .pipe(finalize(() => (this.savingReferencia = false)))
      .subscribe({
        next: () => {
          this.notify.success('Referencia adicionada.');
          this.referenciaForm.reset({ nome: '', atividades: '', dataInicio: '', dataFim: '' });
          this.loadReferencias();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel adicionar referencia.')
      });
  }

  removeReferencia(id: number): void {
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteReferencia(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Referencia removida.');
        this.loadReferencias();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover referencia.')
    });
  }

  saveQualificacao(): void {
    if (!this.canEdit() || !this.entidadeId) return;
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
    this.service.createQualificacao(this.tipoEntidadeId, this.entidadeId, payload)
      .pipe(finalize(() => (this.savingQualificacao = false)))
      .subscribe({
        next: () => {
          this.notify.success('Qualificacao adicionada.');
          this.qualificacaoForm.reset({ rhQualificacaoId: null, completo: false, tipo: '' });
          this.loadQualificacoes();
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel adicionar qualificacao.')
      });
  }

  removeQualificacao(id: number): void {
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteQualificacao(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Qualificacao removida.');
        this.loadQualificacoes();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover qualificacao.')
    });
  }

  saveEndereco(): void {
    if (!this.canEdit() || !this.entidadeId) return;
    if (this.enderecoForm.invalid) {
      this.enderecoForm.markAllAsTouched();
      this.notify.error('Preencha os campos obrigatorios do endereco.');
      return;
    }
    const raw = this.enderecoForm.getRawValue();
    const payload: EntidadeEnderecoPayload = {
      nome: (raw.nome || '').trim() || null,
      enderecoTipo: (raw.enderecoTipo || 'RESIDENCIAL') as EntidadeEnderecoPayload['enderecoTipo'],
      cep: (raw.cep || '').trim() || null,
      uf: ((raw.uf || '').trim() || '').toUpperCase() || null,
      municipio: (raw.municipio || '').trim() || null,
      logradouro: (raw.logradouro || '').trim() || null,
      numero: (raw.numero || '').trim() || null,
      complemento: (raw.complemento || '').trim() || null,
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
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteEndereco(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Endereco removido.');
        this.loadEnderecos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover endereco.')
    });
  }

  saveContato(): void {
    if (!this.canEdit() || !this.entidadeId) return;
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
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteContato(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Contato removido.');
        this.loadContatos();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover contato.')
    });
  }

  saveContatoForma(): void {
    if (!this.canEdit() || !this.entidadeId) return;
    if (this.contatoFormaForm.invalid) {
      this.contatoFormaForm.markAllAsTouched();
      this.notify.error('Informe contato, tipo e valor.');
      return;
    }
    const raw = this.contatoFormaForm.getRawValue();
    const contatoId = this.toNumber(raw.contatoId);
    if (!contatoId) {
      this.notify.error('Contato invalido.');
      return;
    }
    const payload: EntidadeContatoFormaPayload = {
      tipoContato: (raw.tipoContato || 'EMAIL') as EntidadeContatoFormaPayload['tipoContato'],
      valor: (raw.valor || '').trim(),
      preferencial: !!raw.preferencial
    };
    this.savingContatoForma = true;
    const formas = this.contatoFormasByContato[contatoId] || [];
    const current = formas.find(item => item.id === this.editingContatoFormaId);
    const req = this.editingContatoFormaId
      ? this.service.updateContatoForma(
        this.tipoEntidadeId,
        this.entidadeId,
        contatoId,
        this.editingContatoFormaId,
        { ...payload, version: current?.version ?? null }
      )
      : this.service.createContatoForma(this.tipoEntidadeId, this.entidadeId, contatoId, payload);
    req.pipe(finalize(() => (this.savingContatoForma = false))).subscribe({
      next: () => {
        this.notify.success(this.editingContatoFormaId ? 'Forma de contato atualizada.' : 'Forma de contato adicionada.');
        this.cancelContatoFormaEdit();
        this.contatoFormaForm.patchValue({ contatoId });
        this.loadContatoFormas(contatoId);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel salvar forma de contato.')
    });
  }

  removeContatoForma(contatoId: number, formaId: number): void {
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteContatoForma(this.tipoEntidadeId, this.entidadeId, contatoId, formaId).subscribe({
      next: () => {
        this.notify.success('Forma de contato removida.');
        this.loadContatoFormas(contatoId);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover forma de contato.')
    });
  }

  saveFamiliar(): void {
    if (!this.canEdit() || !this.entidadeId) return;
    if (this.familiarForm.invalid) {
      this.familiarForm.markAllAsTouched();
      this.notify.error('Informe os campos obrigatorios do familiar.');
      return;
    }
    const raw = this.familiarForm.getRawValue();
    const entidadeParenteId = this.toNumber(raw.entidadeParenteId);
    if (!entidadeParenteId) {
      this.notify.error('Entidade parente invalida.');
      return;
    }
    const payload: EntidadeFamiliarPayload = {
      entidadeParenteId,
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
    if (!this.canEdit() || !this.entidadeId) return;
    this.service.deleteFamiliar(this.tipoEntidadeId, this.entidadeId, id).subscribe({
      next: () => {
        this.notify.success('Familiar removido.');
        this.loadFamiliares();
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel remover familiar.')
    });
  }

  editEndereco(item: EntidadeEndereco): void {
    this.editingEnderecoId = item.id;
    this.enderecoForm.patchValue({
      nome: item.nome || '',
      enderecoTipo: item.enderecoTipo || 'RESIDENCIAL',
      cep: item.cep || '',
      uf: item.uf || '',
      municipio: item.municipio || '',
      logradouro: item.logradouro || '',
      numero: item.numero || '',
      complemento: item.complemento || '',
      principal: !!item.principal
    });
  }

  cancelEnderecoEdit(): void {
    this.editingEnderecoId = null;
    this.enderecoForm.reset({
      nome: '',
      enderecoTipo: 'RESIDENCIAL',
      cep: '',
      uf: '',
      municipio: '',
      logradouro: '',
      numero: '',
      complemento: '',
      principal: false
    });
  }

  editContato(item: EntidadeContato): void {
    this.editingContatoId = item.id;
    this.contatoForm.patchValue({
      nome: item.nome || '',
      cargo: item.cargo || ''
    });
  }

  cancelContatoEdit(): void {
    this.editingContatoId = null;
    this.contatoForm.reset({ nome: '', cargo: '' });
  }

  editContatoForma(contatoId: number, item: EntidadeContatoForma): void {
    this.editingContatoFormaContatoId = contatoId;
    this.editingContatoFormaId = item.id;
    this.contatoFormaForm.patchValue({
      contatoId,
      tipoContato: item.tipoContato,
      valor: item.valor,
      preferencial: !!item.preferencial
    });
  }

  cancelContatoFormaEdit(): void {
    const keepContatoId = this.editingContatoFormaContatoId ?? this.contatoFormaForm.value.contatoId;
    this.editingContatoFormaContatoId = null;
    this.editingContatoFormaId = null;
    this.contatoFormaForm.reset({
      contatoId: keepContatoId || null,
      tipoContato: 'EMAIL',
      valor: '',
      preferencial: false
    });
  }

  editFamiliar(item: EntidadeFamiliar): void {
    this.editingFamiliarId = item.id;
    this.familiarSelecionadoLabel = item.entidadeParenteNome
      ? `#${item.entidadeParenteId} - ${item.entidadeParenteNome}`
      : `#${item.entidadeParenteId}`;
    this.familiarForm.patchValue({
      entidadeParenteBusca: item.entidadeParenteNome || '',
      entidadeParenteId: item.entidadeParenteId,
      parentesco: item.parentesco || 'OUTROS',
      dependente: !!item.dependente
    });
  }

  cancelFamiliarEdit(): void {
    this.editingFamiliarId = null;
    this.familiarSearchResults = [];
    this.familiarSelecionadoLabel = '';
    this.familiarForm.reset({
      entidadeParenteBusca: '',
      entidadeParenteId: null,
      parentesco: 'OUTROS',
      dependente: false
    });
  }

  chooseFamiliarParente(row: RegistroEntidade, userInput = true): void {
    if (!userInput) return;
    this.familiarForm.patchValue({
      entidadeParenteBusca: row.pessoa?.nome || '',
      entidadeParenteId: row.id
    });
    this.familiarSelecionadoLabel = `#${row.codigo} - ${row.pessoa?.nome || ''}`;
  }

  clearFamiliarSelection(): void {
    this.familiarSelecionadoLabel = '';
    this.familiarForm.patchValue({
      entidadeParenteBusca: '',
      entidadeParenteId: null
    });
    this.familiarSearchResults = [];
  }

  onFamiliarQueryChange(): void {
    const termo = (this.familiarForm.value.entidadeParenteBusca || '').trim();
    if (!termo) {
      this.familiarForm.patchValue({ entidadeParenteId: null }, { emitEvent: false });
      this.familiarSelecionadoLabel = '';
    }
    this.familiarSearch$.next(termo);
  }

  isContatoFormaEmail(): boolean {
    return (this.contatoFormaForm.value.tipoContato || 'EMAIL') === 'EMAIL';
  }

  isGroupEnabled(groupKey: string): boolean {
    const config = this.formGroupByKey[(groupKey || '').trim()];
    return config ? !!config.enabled : true;
  }

  isFieldVisible(fieldKey: string): boolean {
    const config = this.formFieldByKey[(fieldKey || '').trim()];
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

  pendingEnabledGroupsForNew(): Array<{ key: string; label: string }> {
    if (this.entidadeId) return [];
    return this.formGroupLabels.filter(item => this.isGroupEnabled(item.key));
  }

  private loadTipoAndContinue(): void {
    this.loadPriceBooks();
    this.typeService.get(this.tipoEntidadeId).subscribe({
      next: tipo => {
        this.tipoNome = (tipo?.nome || '').trim();
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
      ativo: entity.ativo, priceBookId: entity.priceBookId ?? null, tratamentoId: entity.tratamentoId ?? null,
      codigoBarras: entity.codigoBarras || '', alerta: entity.alerta || '', observacao: entity.observacao || '',
      parecer: entity.parecer || '', textoTermoQuitacao: entity.textoTermoQuitacao || '',
      pessoaNome: entity.pessoa.nome, pessoaApelido: entity.pessoa.apelido || '',
      tipoRegistro: entity.pessoa.tipoRegistro, registroFederal: entity.pessoa.registroFederal
    });
  }

  private loadSubresources(): void {
    if (!this.entidadeId) return;
    this.loadingSubresources = true;
    this.service.loadRhOptions(this.tipoEntidadeId).subscribe({ next: r => this.rhOptions = r || this.rhOptions, error: () => {} });
    this.service.getDocumentacao(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: d => this.documentacaoForm.patchValue({ tipoRegistroFederal: d.tipoRegistroFederal as any, registroFederal: d.registroFederal || '', rg: d.rg || '', rgUfEmissao: d.rgUfEmissao || '', cnh: d.cnh || '', numeroNif: d.numeroNif || '' }),
      error: () => {}
    });
    this.service.getInfoComercial(this.tipoEntidadeId, this.entidadeId).subscribe({
      next: c => this.comercialForm.patchValue({ faturamentoDiasPrazo: c.faturamentoDiasPrazo ?? null, faturamentoFrequenciaCobrancaId: c.faturamentoFrequenciaCobrancaId ?? null, juroTaxaPadrao: c.juroTaxaPadrao ?? null, ramoAtividade: c.ramoAtividade || '', boletosEnviarEmail: !!c.boletosEnviarEmail, consumidorFinal: !!c.consumidorFinal }),
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
    return value.replace(/_/g, ' ');
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
        this.contatosCount = this.contatos.length;
        this.contatoFormasByContato = {};
        if (!this.contatoFormaForm.value.contatoId && this.contatos.length) {
          const firstContato = this.contatos[0];
          if (firstContato) {
            this.contatoFormaForm.patchValue({ contatoId: firstContato.id });
          }
        }
        this.contatos.forEach(contato => this.loadContatoFormas(contato.id));
      },
      error: () => {
        this.contatos = [];
        this.contatosCount = 0;
        this.contatoFormasByContato = {};
      }
    });
  }

  private loadContatoFormas(contatoId: number): void {
    if (!this.entidadeId) return;
    this.service.listContatoFormas(this.tipoEntidadeId, this.entidadeId, contatoId).subscribe({
      next: rows => {
        this.contatoFormasByContato[contatoId] = rows || [];
      },
      error: () => {
        this.contatoFormasByContato[contatoId] = [];
      }
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
            if (fieldKey) this.formFieldByKey[fieldKey] = field;
          });
        });

        const currentTipoContato = (this.contatoFormaForm.value.tipoContato || 'EMAIL').trim();
        if (!this.isContatoFormaTipoVisible(currentTipoContato)) {
          const fallbackTipoContato = ['EMAIL', 'FONE_CELULAR', 'FONE_RESIDENCIAL', 'FONE_COMERCIAL', 'WHATSAPP', 'FACEBOOK']
            .find(tipo => this.isContatoFormaTipoVisible(tipo)) || 'EMAIL';
          this.contatoFormaForm.patchValue({ tipoContato: fallbackTipoContato }, { emitEvent: false });
        }
        this.applyFieldConfigRules();
      },
      error: () => {
        this.formConfigByGroup = null;
        this.formGroupByKey = {};
        this.formFieldByKey = {};
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
      const shouldRequire = fieldCfg ? !!fieldCfg.visible && !!fieldCfg.required : defaultRequired;
      const shouldEdit = this.canEdit() && (!fieldCfg || !!fieldCfg.editable);

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
    push('pessoaNome', this.form.get('pessoaNome'));
    push('pessoaApelido', this.form.get('pessoaApelido'));
    push('contatos.nome', this.contatoForm.get('nome'));
    push('contatos.cargo', this.contatoForm.get('cargo'));
    push('enderecos.cep', this.enderecoForm.get('cep'));
    push('enderecos.uf', this.enderecoForm.get('uf'));
    push('enderecos.municipio', this.enderecoForm.get('municipio'));
    push('enderecos.logradouro', this.enderecoForm.get('logradouro'));
    push('documentacao.rg', this.documentacaoForm.get('rg'));
    push('documentacao.cnh', this.documentacaoForm.get('cnh'));
    push('documentacao.numeroNif', this.documentacaoForm.get('numeroNif'));
    push('comercial.faturamentoDiasPrazo', this.comercialForm.get('faturamentoDiasPrazo'));
    push('comercial.juroTaxaPadrao', this.comercialForm.get('juroTaxaPadrao'));
    push('fiscal.manifestarNotaAutomaticamente', this.fiscalForm.get('manifestarNotaAutomaticamente'));
    push('rh.contrato.numero', this.rhContratoForm.get('numero'));
    push('rh.contrato.admissaoData', this.rhContratoForm.get('admissaoData'));
    push('rh.info.atividades', this.rhInfoForm.get('atividades'));
    push('familiares.parentesco', this.familiarForm.get('parentesco'));
    push('referencias.nome', this.referenciaForm.get('nome'));
    push('qualificacoes.rhQualificacaoId', this.qualificacaoForm.get('rhQualificacaoId'));
    return map;
  }

  private toNumber(value: unknown): number | null {
    const n = Number(value); return Number.isFinite(n) && n > 0 ? n : null;
  }
  private toFloat(value: unknown): number | null {
    if (value === null || value === undefined || `${value}`.trim() === '') return null;
    const n = Number(value); return Number.isFinite(n) ? n : null;
  }
  private toSmallInt(value: unknown): number | null {
    const n = this.toFloat(value); if (n === null) return null; const i = Math.trunc(n); return i >= 0 && i <= 2 ? i : null;
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

    this.familiarSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(termo => {
          if (!this.entidadeId || !termo || termo.length < 2) {
            this.searchingFamiliar = false;
            return of([] as RegistroEntidade[]);
          }
          this.searchingFamiliar = true;
          return this.service.list(this.tipoEntidadeId, { page: 0, size: 20, pessoaNome: termo, ativo: '' }).pipe(
            map(resp => (resp?.content || []).filter(row => row.id !== this.entidadeId)),
            catchError(() => of([] as RegistroEntidade[])),
            finalize(() => (this.searchingFamiliar = false))
          );
        })
      )
      .subscribe(rows => (this.familiarSearchResults = rows));
  }

  private applyContatoFormaValorValidators(): void {
    const tipo = this.contatoFormaForm.value.tipoContato || 'EMAIL';
    const control = this.contatoFormaForm.controls.valor;
    if (tipo === 'EMAIL') {
      control.setValidators([Validators.required, Validators.maxLength(255), Validators.email]);
    } else if (tipo === 'FONE_CELULAR' || tipo === 'FONE_RESIDENCIAL' || tipo === 'FONE_COMERCIAL' || tipo === 'WHATSAPP') {
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
}
