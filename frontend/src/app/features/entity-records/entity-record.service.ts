import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegistroEntidadeContexto {
  empresaId: number;
  empresaNome: string;
  tipoEntidadeId: number;
  agrupadorId?: number | null;
  agrupadorNome?: string | null;
  tipoEntidadeConfigAgrupadorId?: number | null;
  vinculado: boolean;
  motivo?: string | null;
  mensagem?: string | null;
}

export interface PessoaVinculoPayload {
  nome: string;
  apelido?: string;
  tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
  registroFederal: string;
}

export interface RegistroEntidadePayload {
  grupoEntidadeId?: number | null;
  priceBookId?: number | null;
  alerta?: string | null;
  observacao?: string | null;
  parecer?: string | null;
  codigoBarras?: string | null;
  textoTermoQuitacao?: string | null;
  tratamentoId?: number | null;
  version?: number | null;
  ativo: boolean;
  pessoa: PessoaVinculoPayload;
}

export interface RegistroEntidade {
  id: number;
  empresaId: number;
  tipoEntidadeConfigAgrupadorId: number;
  codigo: number;
  grupoEntidadeId?: number | null;
  grupoEntidadeNome?: string | null;
  priceBookId?: number | null;
  alerta?: string | null;
  observacao?: string | null;
  parecer?: string | null;
  codigoBarras?: string | null;
  textoTermoQuitacao?: string | null;
  tratamentoId?: number | null;
  version?: number | null;
  ativo: boolean;
  pessoa: {
    id: number;
    nome: string;
    apelido?: string | null;
    tipoRegistro: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
    registroFederal: string;
  };
}

export interface RegistroEntidadeListResponse {
  content: RegistroEntidade[];
  totalElements?: number;
  page?: {
    totalElements: number;
  };
}

export interface EntidadeDocumentacaoPayload {
  tipoRegistroFederal: 'CPF' | 'CNPJ' | 'ID_ESTRANGEIRO';
  registroFederal: string;
  registroFederalDataEmissao?: string | null;
  rg?: string | null;
  rgTipo?: string | null;
  rgDataEmissao?: string | null;
  rgUfEmissao?: string | null;
  registroEstadual?: string | null;
  registroEstadualDataEmissao?: string | null;
  registroEstadualUf?: string | null;
  registroEstadualContribuinte?: boolean | null;
  registroEstadualConsumidorFinal?: boolean | null;
  registroMunicipal?: string | null;
  registroMunicipalDataEmissao?: string | null;
  cnh?: string | null;
  cnhCategoria?: string | null;
  cnhObservacao?: string | null;
  cnhDataEmissao?: string | null;
  suframa?: string | null;
  rntc?: string | null;
  pis?: string | null;
  tituloEleitor?: string | null;
  tituloEleitorZona?: string | null;
  tituloEleitorSecao?: string | null;
  ctps?: string | null;
  ctpsSerie?: string | null;
  ctpsDataEmissao?: string | null;
  ctpsUfEmissao?: string | null;
  militarNumero?: string | null;
  militarSerie?: string | null;
  militarCategoria?: string | null;
  numeroNif?: string | null;
  motivoNaoNif?: number | null;
}

export interface EntidadeDocumentacao extends EntidadeDocumentacaoPayload {
  id: number;
  registroEntidadeId: number;
  registroFederalNormalizado: string;
  registroFederalHash: string;
  version?: number | null;
}

export interface EntidadeEnderecoPayload {
  nome?: string | null;
  cep?: string | null;
  cepEstrangeiro?: string | null;
  pais?: string | null;
  paisCodigoIbge?: number | null;
  uf?: string | null;
  ufCodigoIbge?: string | null;
  municipio?: string | null;
  municipioCodigoIbge?: string | null;
  logradouro?: string | null;
  logradouroTipo?: string | null;
  numero?: string | null;
  complemento?: string | null;
  enderecoTipo: 'RESIDENCIAL' | 'COMERCIAL' | 'ENTREGA' | 'COBRANCA' | 'OUTRO' | 'CORRESPONDENCIA';
  principal?: boolean | null;
  longitude?: number | null;
  latitude?: number | null;
  estadoProvinciaRegiaoEstrangeiro?: string | null;
  version?: number | null;
}

export interface EntidadeEndereco extends EntidadeEnderecoPayload {
  id: number;
  registroEntidadeId: number;
  version?: number | null;
}

export interface EntidadeContatoPayload {
  nome?: string | null;
  cargo?: string | null;
  version?: number | null;
}

export interface EntidadeContato extends EntidadeContatoPayload {
  id: number;
  registroEntidadeId: number;
  version?: number | null;
}

export interface EntidadeContatoFormaPayload {
  tipoContato: 'EMAIL' | 'FONE_CELULAR' | 'FONE_RESIDENCIAL' | 'FONE_COMERCIAL' | 'FACEBOOK' | 'WHATSAPP';
  valor: string;
  preferencial?: boolean | null;
  version?: number | null;
}

export interface EntidadeContatoForma extends EntidadeContatoFormaPayload {
  id: number;
  contatoId: number;
  valorNormalizado: string;
  version?: number | null;
}

export interface EntidadeFamiliarPayload {
  entidadeParenteId: number;
  dependente?: boolean | null;
  parentesco: 'PAI' | 'FILHO' | 'IRMAO' | 'IRMA' | 'TIO' | 'TIA' | 'PRIMO' | 'PRIMA' | 'VO' | 'VOMAE' | 'BISAVO' | 'BISAVOMAE' | 'OUTROS';
  version?: number | null;
}

export interface EntidadeFamiliar extends EntidadeFamiliarPayload {
  id: number;
  registroEntidadeId: number;
  entidadeParenteNome?: string | null;
  version?: number | null;
}

export interface EntidadeInfoComercialPayload {
  faturamentoDiaInicial?: string | null;
  faturamentoDiaFinal?: string | null;
  faturamentoDiasPrazo?: number | null;
  boletosEnviarEmail?: boolean | null;
  faturamentoFrequenciaCobrancaId?: number | null;
  juroTaxaPadrao?: number | null;
  ramoAtividade?: string | null;
  consumidorFinal?: boolean | null;
  version?: number | null;
}

export interface EntidadeInfoComercial extends EntidadeInfoComercialPayload {
  id: number;
  registroEntidadeId: number;
  boletosEnviarEmail: boolean;
  consumidorFinal: boolean;
}

export interface EntidadeDadosFiscaisPayload {
  manifestarNotaAutomaticamente?: number | null;
  usaNotaFiscalFatura?: number | null;
  ignorarImportacaoNota?: number | null;
  version?: number | null;
}

export interface EntidadeDadosFiscais extends EntidadeDadosFiscaisPayload {
  id: number;
  registroEntidadeId: number;
}

export interface EntidadeContratoRhPayload {
  numero?: string | null;
  admissaoData?: string | null;
  remuneracao?: number | null;
  remuneracaoComplementar?: number | null;
  bonificacao?: number | null;
  sindicalizado?: boolean | null;
  percentualInsalubridade?: number | null;
  percentualPericulosidade?: number | null;
  tipoFuncionarioId?: number | null;
  situacaoFuncionarioId?: number | null;
  setorId?: number | null;
  cargoId?: number | null;
  ocupacaoAtividadeId?: number | null;
  version?: number | null;
}

export interface EntidadeContratoRh extends EntidadeContratoRhPayload {
  id: number;
  registroEntidadeId: number;
  sindicalizado: boolean;
}

export interface EntidadeInfoRhPayload {
  atividades?: string | null;
  habilidades?: string | null;
  experiencias?: string | null;
  aceitaViajar?: boolean | null;
  possuiCarro?: boolean | null;
  possuiMoto?: boolean | null;
  metaMediaHorasVendidasDia?: number | null;
  metaProdutosVendidos?: number | null;
  version?: number | null;
}

export interface EntidadeInfoRh extends EntidadeInfoRhPayload {
  id: number;
  registroEntidadeId: number;
  aceitaViajar: boolean;
  possuiCarro: boolean;
  possuiMoto: boolean;
}

export interface EntidadeReferenciaPayload {
  nome: string;
  atividades?: string | null;
  dataInicio?: string | null;
  dataFim?: string | null;
  version?: number | null;
}

export interface EntidadeReferencia extends EntidadeReferenciaPayload {
  id: number;
  registroEntidadeId: number;
}

export interface EntidadeQualificacaoItemPayload {
  rhQualificacaoId: number;
  completo?: boolean | null;
  tipo?: string | null;
  version?: number | null;
}

export interface EntidadeQualificacaoItem extends EntidadeQualificacaoItemPayload {
  id: number;
  registroEntidadeId: number;
  rhQualificacaoNome?: string | null;
  completo: boolean;
}

export interface EntidadeRhOption {
  id: number;
  nome: string;
}

export interface EntidadeRhOptions {
  frequenciasCobranca: EntidadeRhOption[];
  tiposFuncionario: EntidadeRhOption[];
  situacoesFuncionario: EntidadeRhOption[];
  setores: EntidadeRhOption[];
  cargos: EntidadeRhOption[];
  ocupacoesAtividade: EntidadeRhOption[];
  qualificacoes: EntidadeRhOption[];
}

export interface EntidadeFormFieldConfig {
  id?: number | null;
  fieldKey: string;
  label?: string | null;
  ordem: number;
  visible: boolean;
  editable: boolean;
  required: boolean;
}

export interface EntidadeFormGroupConfig {
  id?: number | null;
  groupKey: string;
  label?: string | null;
  ordem: number;
  enabled: boolean;
  collapsedByDefault: boolean;
  fields: EntidadeFormFieldConfig[];
}

export interface EntidadeFormConfigByGroup {
  tipoEntidadeId: number;
  agrupadorId: number;
  agrupadorNome: string;
  obrigarUmTelefone: boolean;
  groups: EntidadeFormGroupConfig[];
}

@Injectable({ providedIn: 'root' })
export class EntityRecordService {
  private baseUrl = `${environment.apiBaseUrl}/api/tipos-entidade`;

  constructor(private http: HttpClient) {}

  contextoEmpresa(tipoEntidadeId: number): Observable<RegistroEntidadeContexto> {
    return this.http.get<RegistroEntidadeContexto>(`${this.baseUrl}/${tipoEntidadeId}/entidades/contexto-empresa`);
  }

  list(
    tipoEntidadeId: number,
    params: {
      page?: number;
      size?: number;
      codigo?: number | null;
      pessoaNome?: string;
      registroFederal?: string;
      grupoId?: number | null;
      ativo?: boolean | '';
    }
  ): Observable<RegistroEntidadeListResponse> {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && `${value}` !== '') query.set(key, `${value}`);
    });
    return this.http.get<RegistroEntidadeListResponse>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades?${query.toString()}`
    );
  }

  get(tipoEntidadeId: number, id: number): Observable<RegistroEntidade> {
    return this.http.get<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`);
  }

  create(tipoEntidadeId: number, payload: RegistroEntidadePayload): Observable<RegistroEntidade> {
    return this.http.post<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades`, payload);
  }

  update(tipoEntidadeId: number, id: number, payload: RegistroEntidadePayload): Observable<RegistroEntidade> {
    return this.http.put<RegistroEntidade>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`, payload);
  }

  delete(tipoEntidadeId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${id}`);
  }

  getDocumentacao(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeDocumentacao> {
    return this.http.get<EntidadeDocumentacao>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/documentacao`);
  }

  upsertDocumentacao(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeDocumentacaoPayload
  ): Observable<EntidadeDocumentacao> {
    return this.http.put<EntidadeDocumentacao>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/documentacao`,
      payload
    );
  }

  listEnderecos(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeEndereco[]> {
    return this.http.get<EntidadeEndereco[]>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/enderecos`);
  }

  createEndereco(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeEnderecoPayload
  ): Observable<EntidadeEndereco> {
    return this.http.post<EntidadeEndereco>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/enderecos`,
      payload
    );
  }

  updateEndereco(
    tipoEntidadeId: number,
    entidadeId: number,
    enderecoId: number,
    payload: EntidadeEnderecoPayload
  ): Observable<EntidadeEndereco> {
    return this.http.put<EntidadeEndereco>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/enderecos/${enderecoId}`,
      payload
    );
  }

  deleteEndereco(tipoEntidadeId: number, entidadeId: number, enderecoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/enderecos/${enderecoId}`);
  }

  listContatos(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeContato[]> {
    return this.http.get<EntidadeContato[]>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos`);
  }

  createContato(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeContatoPayload
  ): Observable<EntidadeContato> {
    return this.http.post<EntidadeContato>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos`, payload);
  }

  updateContato(
    tipoEntidadeId: number,
    entidadeId: number,
    contatoId: number,
    payload: EntidadeContatoPayload
  ): Observable<EntidadeContato> {
    return this.http.put<EntidadeContato>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}`,
      payload
    );
  }

  deleteContato(tipoEntidadeId: number, entidadeId: number, contatoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}`);
  }

  listContatoFormas(tipoEntidadeId: number, entidadeId: number, contatoId: number): Observable<EntidadeContatoForma[]> {
    return this.http.get<EntidadeContatoForma[]>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}/formas`
    );
  }

  createContatoForma(
    tipoEntidadeId: number,
    entidadeId: number,
    contatoId: number,
    payload: EntidadeContatoFormaPayload
  ): Observable<EntidadeContatoForma> {
    return this.http.post<EntidadeContatoForma>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}/formas`,
      payload
    );
  }

  updateContatoForma(
    tipoEntidadeId: number,
    entidadeId: number,
    contatoId: number,
    formaId: number,
    payload: EntidadeContatoFormaPayload
  ): Observable<EntidadeContatoForma> {
    return this.http.put<EntidadeContatoForma>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}/formas/${formaId}`,
      payload
    );
  }

  deleteContatoForma(tipoEntidadeId: number, entidadeId: number, contatoId: number, formaId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/contatos/${contatoId}/formas/${formaId}`
    );
  }

  listFamiliares(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeFamiliar[]> {
    return this.http.get<EntidadeFamiliar[]>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/familiares`);
  }

  createFamiliar(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeFamiliarPayload
  ): Observable<EntidadeFamiliar> {
    return this.http.post<EntidadeFamiliar>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/familiares`, payload);
  }

  updateFamiliar(
    tipoEntidadeId: number,
    entidadeId: number,
    familiarId: number,
    payload: EntidadeFamiliarPayload
  ): Observable<EntidadeFamiliar> {
    return this.http.put<EntidadeFamiliar>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/familiares/${familiarId}`,
      payload
    );
  }

  deleteFamiliar(tipoEntidadeId: number, entidadeId: number, familiarId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/familiares/${familiarId}`);
  }

  getInfoComercial(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeInfoComercial> {
    return this.http.get<EntidadeInfoComercial>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/comercial`);
  }

  upsertInfoComercial(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeInfoComercialPayload
  ): Observable<EntidadeInfoComercial> {
    return this.http.put<EntidadeInfoComercial>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/comercial`, payload);
  }

  getDadosFiscais(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeDadosFiscais> {
    return this.http.get<EntidadeDadosFiscais>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/dados-fiscais`);
  }

  upsertDadosFiscais(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeDadosFiscaisPayload
  ): Observable<EntidadeDadosFiscais> {
    return this.http.put<EntidadeDadosFiscais>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/dados-fiscais`, payload);
  }

  getContratoRh(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeContratoRh> {
    return this.http.get<EntidadeContratoRh>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/contrato`);
  }

  upsertContratoRh(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeContratoRhPayload
  ): Observable<EntidadeContratoRh> {
    return this.http.put<EntidadeContratoRh>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/contrato`, payload);
  }

  getInfoRh(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeInfoRh> {
    return this.http.get<EntidadeInfoRh>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/info`);
  }

  upsertInfoRh(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeInfoRhPayload
  ): Observable<EntidadeInfoRh> {
    return this.http.put<EntidadeInfoRh>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/info`, payload);
  }

  listReferencias(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeReferencia[]> {
    return this.http.get<EntidadeReferencia[]>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/referencias`);
  }

  createReferencia(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeReferenciaPayload
  ): Observable<EntidadeReferencia> {
    return this.http.post<EntidadeReferencia>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/referencias`, payload);
  }

  updateReferencia(
    tipoEntidadeId: number,
    entidadeId: number,
    referenciaId: number,
    payload: EntidadeReferenciaPayload
  ): Observable<EntidadeReferencia> {
    return this.http.put<EntidadeReferencia>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/referencias/${referenciaId}`,
      payload
    );
  }

  deleteReferencia(tipoEntidadeId: number, entidadeId: number, referenciaId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/referencias/${referenciaId}`);
  }

  listQualificacoes(tipoEntidadeId: number, entidadeId: number): Observable<EntidadeQualificacaoItem[]> {
    return this.http.get<EntidadeQualificacaoItem[]>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/qualificacoes`);
  }

  createQualificacao(
    tipoEntidadeId: number,
    entidadeId: number,
    payload: EntidadeQualificacaoItemPayload
  ): Observable<EntidadeQualificacaoItem> {
    return this.http.post<EntidadeQualificacaoItem>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/qualificacoes`, payload);
  }

  updateQualificacao(
    tipoEntidadeId: number,
    entidadeId: number,
    qualificacaoId: number,
    payload: EntidadeQualificacaoItemPayload
  ): Observable<EntidadeQualificacaoItem> {
    return this.http.put<EntidadeQualificacaoItem>(
      `${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/qualificacoes/${qualificacaoId}`,
      payload
    );
  }

  deleteQualificacao(tipoEntidadeId: number, entidadeId: number, qualificacaoId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${tipoEntidadeId}/entidades/${entidadeId}/rh/qualificacoes/${qualificacaoId}`);
  }

  loadRhOptions(tipoEntidadeId: number): Observable<EntidadeRhOptions> {
    return this.http.get<EntidadeRhOptions>(`${this.baseUrl}/${tipoEntidadeId}/entidades/rh/opcoes`);
  }

  getFormConfigByGroup(tipoEntidadeId: number, agrupadorId: number): Observable<EntidadeFormConfigByGroup> {
    return this.http.get<EntidadeFormConfigByGroup>(
      `${this.baseUrl}/${tipoEntidadeId}/config-agrupadores/${agrupadorId}/ficha`
    );
  }
}
