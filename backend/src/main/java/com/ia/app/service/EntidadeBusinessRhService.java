package com.ia.app.service;

import com.ia.app.domain.EntidadeContratoRh;
import com.ia.app.domain.EntidadeDadosFiscais;
import com.ia.app.domain.EntidadeInfoComercial;
import com.ia.app.domain.EntidadeInfoRh;
import com.ia.app.domain.EntidadeQualificacaoItem;
import com.ia.app.domain.EntidadeReferencia;
import com.ia.app.domain.RegistroEntidade;
import com.ia.app.dto.EntidadeContratoRhRequest;
import com.ia.app.dto.EntidadeContratoRhResponse;
import com.ia.app.dto.EntidadeDadosFiscaisRequest;
import com.ia.app.dto.EntidadeDadosFiscaisResponse;
import com.ia.app.dto.EntidadeInfoComercialRequest;
import com.ia.app.dto.EntidadeInfoComercialResponse;
import com.ia.app.dto.EntidadeInfoRhRequest;
import com.ia.app.dto.EntidadeInfoRhResponse;
import com.ia.app.dto.EntidadeQualificacaoItemRequest;
import com.ia.app.dto.EntidadeQualificacaoItemResponse;
import com.ia.app.dto.EntidadeReferenciaRequest;
import com.ia.app.dto.EntidadeReferenciaResponse;
import com.ia.app.dto.EntidadeRhOptionResponse;
import com.ia.app.dto.EntidadeRhOptionsResponse;
import com.ia.app.repository.EntidadeContratoRhRepository;
import com.ia.app.repository.EntidadeDadosFiscaisRepository;
import com.ia.app.repository.EntidadeInfoComercialRepository;
import com.ia.app.repository.EntidadeInfoRhRepository;
import com.ia.app.repository.EntidadeQualificacaoItemRepository;
import com.ia.app.repository.EntidadeReferenciaRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntidadeBusinessRhService {

  private final RegistroEntidadeContextoService contextoService;
  private final RegistroEntidadeRepository registroRepository;
  private final EntidadeInfoComercialRepository infoComercialRepository;
  private final EntidadeDadosFiscaisRepository dadosFiscaisRepository;
  private final EntidadeContratoRhRepository contratoRhRepository;
  private final EntidadeInfoRhRepository infoRhRepository;
  private final EntidadeReferenciaRepository referenciaRepository;
  private final EntidadeQualificacaoItemRepository qualificacaoItemRepository;
  private final JdbcTemplate jdbcTemplate;

  public EntidadeBusinessRhService(
      RegistroEntidadeContextoService contextoService,
      RegistroEntidadeRepository registroRepository,
      EntidadeInfoComercialRepository infoComercialRepository,
      EntidadeDadosFiscaisRepository dadosFiscaisRepository,
      EntidadeContratoRhRepository contratoRhRepository,
      EntidadeInfoRhRepository infoRhRepository,
      EntidadeReferenciaRepository referenciaRepository,
      EntidadeQualificacaoItemRepository qualificacaoItemRepository,
      JdbcTemplate jdbcTemplate) {
    this.contextoService = contextoService;
    this.registroRepository = registroRepository;
    this.infoComercialRepository = infoComercialRepository;
    this.dadosFiscaisRepository = dadosFiscaisRepository;
    this.contratoRhRepository = contratoRhRepository;
    this.infoRhRepository = infoRhRepository;
    this.referenciaRepository = referenciaRepository;
    this.qualificacaoItemRepository = qualificacaoItemRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public EntidadeInfoComercialResponse getInfoComercial(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return infoComercialRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .map(this::toInfoComercialResponse)
      .orElseGet(() -> new EntidadeInfoComercialResponse(
        null, sharedRegistroId, null, null, null, null, false, null, null, null, false, null
      ));
  }

  @Transactional
  public EntidadeInfoComercialResponse upsertInfoComercial(
      Long tipoEntidadeId, Long entidadeId, EntidadeInfoComercialRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeInfoComercial entity = infoComercialRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseGet(EntidadeInfoComercial::new);
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    entity.setFaturamentoDiaInicial(request.faturamentoDiaInicial());
    entity.setFaturamentoDiaFinal(request.faturamentoDiaFinal());
    if (request.faturamentoDiaInicial() != null && request.faturamentoDiaFinal() != null
      && request.faturamentoDiaInicial().isAfter(request.faturamentoDiaFinal())) {
      throw new IllegalArgumentException("entidade_info_comercial_periodo_invalid");
    }
    entity.setFaturamentoDiasPrazo(validateNonNegativeInt(request.faturamentoDiasPrazo(), "entidade_info_comercial_prazo_invalid"));
    entity.setPrazoEntregaDias(validateNonNegativeInt(request.prazoEntregaDias(), "entidade_info_comercial_prazo_entrega_invalid"));
    entity.setBoletosEnviarEmail(Boolean.TRUE.equals(request.boletosEnviarEmail()));
    entity.setFaturamentoFrequenciaCobrancaId(validateOptionId(
      scope.tenantId(), request.faturamentoFrequenciaCobrancaId(), "tipo_frequencia_cobranca", "faturamento_frequencia_not_found"));
    entity.setJuroTaxaPadrao(validatePercent(request.juroTaxaPadrao(), "entidade_info_comercial_juro_invalid"));
    entity.setRamoAtividade(trim(request.ramoAtividade(), 100));
    entity.setConsumidorFinal(Boolean.TRUE.equals(request.consumidorFinal()));
    return toInfoComercialResponse(infoComercialRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public EntidadeDadosFiscaisResponse getDadosFiscais(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return dadosFiscaisRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .map(this::toDadosFiscaisResponse)
      .orElseGet(() -> new EntidadeDadosFiscaisResponse(
        null, sharedRegistroId, null, null, null, null
      ));
  }

  @Transactional
  public EntidadeDadosFiscaisResponse upsertDadosFiscais(
      Long tipoEntidadeId, Long entidadeId, EntidadeDadosFiscaisRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeDadosFiscais entity = dadosFiscaisRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseGet(EntidadeDadosFiscais::new);
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    entity.setManifestarNotaAutomaticamente(validateTriState(request.manifestarNotaAutomaticamente(), "entidade_dados_fiscais_flag_invalid"));
    entity.setUsaNotaFiscalFatura(validateTriState(request.usaNotaFiscalFatura(), "entidade_dados_fiscais_flag_invalid"));
    entity.setIgnorarImportacaoNota(validateTriState(request.ignorarImportacaoNota(), "entidade_dados_fiscais_flag_invalid"));
    return toDadosFiscaisResponse(dadosFiscaisRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public EntidadeContratoRhResponse getContratoRh(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return contratoRhRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .map(this::toContratoRhResponse)
      .orElseGet(() -> new EntidadeContratoRhResponse(
        null, sharedRegistroId, null, null, null, null, null, false,
        null, null, null, null, null, null, null, null
      ));
  }

  @Transactional
  public EntidadeContratoRhResponse upsertContratoRh(
      Long tipoEntidadeId, Long entidadeId, EntidadeContratoRhRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeContratoRh entity = contratoRhRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseGet(EntidadeContratoRh::new);
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    entity.setNumero(trim(request.numero(), 40));
    entity.setAdmissaoData(request.admissaoData());
    entity.setRemuneracao(validateMoney(request.remuneracao(), "entidade_rh_valor_invalid"));
    entity.setRemuneracaoComplementar(validateMoney(request.remuneracaoComplementar(), "entidade_rh_valor_invalid"));
    entity.setBonificacao(validateMoney(request.bonificacao(), "entidade_rh_valor_invalid"));
    entity.setSindicalizado(Boolean.TRUE.equals(request.sindicalizado()));
    entity.setPercentualInsalubridade(validatePercent(request.percentualInsalubridade(), "entidade_rh_percentual_invalid"));
    entity.setPercentualPericulosidade(validatePercent(request.percentualPericulosidade(), "entidade_rh_percentual_invalid"));
    entity.setTipoFuncionarioId(validateOptionId(scope.tenantId(), request.tipoFuncionarioId(), "rh_tipo_funcionario", "entidade_rh_tipo_funcionario_not_found"));
    entity.setSituacaoFuncionarioId(validateOptionId(scope.tenantId(), request.situacaoFuncionarioId(), "rh_situacao_funcionario", "entidade_rh_situacao_funcionario_not_found"));
    entity.setSetorId(validateOptionId(scope.tenantId(), request.setorId(), "rh_setor", "entidade_rh_setor_not_found"));
    entity.setCargoId(validateOptionId(scope.tenantId(), request.cargoId(), "rh_cargo", "entidade_rh_cargo_not_found"));
    entity.setOcupacaoAtividadeId(validateOptionId(scope.tenantId(), request.ocupacaoAtividadeId(), "rh_ocupacao_atividade", "entidade_rh_ocupacao_not_found"));
    return toContratoRhResponse(contratoRhRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public EntidadeInfoRhResponse getInfoRh(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return infoRhRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .map(this::toInfoRhResponse)
      .orElseGet(() -> new EntidadeInfoRhResponse(
        null, sharedRegistroId, null, null, null, false, false, false, null, null, null
      ));
  }

  @Transactional
  public EntidadeInfoRhResponse upsertInfoRh(
      Long tipoEntidadeId, Long entidadeId, EntidadeInfoRhRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeInfoRh entity = infoRhRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseGet(EntidadeInfoRh::new);
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    entity.setAtividades(trim(request.atividades(), 1000));
    entity.setHabilidades(trim(request.habilidades(), 1000));
    entity.setExperiencias(trim(request.experiencias(), 1000));
    entity.setAceitaViajar(Boolean.TRUE.equals(request.aceitaViajar()));
    entity.setPossuiCarro(Boolean.TRUE.equals(request.possuiCarro()));
    entity.setPossuiMoto(Boolean.TRUE.equals(request.possuiMoto()));
    if (request.metaMediaHorasVendidasDia() != null && request.metaMediaHorasVendidasDia() < 0) {
      throw new IllegalArgumentException("entidade_rh_meta_horas_invalid");
    }
    entity.setMetaMediaHorasVendidasDia(request.metaMediaHorasVendidasDia());
    entity.setMetaProdutosVendidos(validateMoney(request.metaProdutosVendidos(), "entidade_rh_meta_produtos_invalid"));
    return toInfoRhResponse(infoRhRepository.save(entity));
  }

  @Transactional(readOnly = true)
  public List<EntidadeReferenciaResponse> listReferencias(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return referenciaRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .stream()
      .map(this::toReferenciaResponse)
      .toList();
  }

  @Transactional
  public EntidadeReferenciaResponse createReferencia(Long tipoEntidadeId, Long entidadeId, EntidadeReferenciaRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeReferencia entity = new EntidadeReferencia();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    applyReferencia(entity, request);
    return toReferenciaResponse(referenciaRepository.save(entity));
  }

  @Transactional
  public EntidadeReferenciaResponse updateReferencia(Long tipoEntidadeId, Long entidadeId, Long referenciaId, EntidadeReferenciaRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeReferencia entity = referenciaRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(referenciaId, scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_referencia_not_found"));
    applyReferencia(entity, request);
    return toReferenciaResponse(referenciaRepository.save(entity));
  }

  @Transactional
  public void deleteReferencia(Long tipoEntidadeId, Long entidadeId, Long referenciaId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeReferencia entity = referenciaRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(referenciaId, scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_referencia_not_found"));
    referenciaRepository.delete(entity);
  }

  @Transactional(readOnly = true)
  public List<EntidadeQualificacaoItemResponse> listQualificacoes(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    return qualificacaoItemRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .stream()
      .map(item -> toQualificacaoResponse(scope.tenantId(), item))
      .toList();
  }

  @Transactional
  public EntidadeQualificacaoItemResponse createQualificacao(Long tipoEntidadeId, Long entidadeId, EntidadeQualificacaoItemRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeQualificacaoItem entity = new EntidadeQualificacaoItem();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(sharedEmpresaId);
    entity.setRegistroEntidadeId(sharedRegistroId);
    applyQualificacao(scope.tenantId(), entity, request);
    return toQualificacaoResponse(scope.tenantId(), qualificacaoItemRepository.save(entity));
  }

  @Transactional
  public EntidadeQualificacaoItemResponse updateQualificacao(
      Long tipoEntidadeId, Long entidadeId, Long qualificacaoId, EntidadeQualificacaoItemRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeQualificacaoItem entity = qualificacaoItemRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(qualificacaoId, scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_qualificacao_not_found"));
    applyQualificacao(scope.tenantId(), entity, request);
    return toQualificacaoResponse(scope.tenantId(), qualificacaoItemRepository.save(entity));
  }

  @Transactional
  public void deleteQualificacao(Long tipoEntidadeId, Long entidadeId, Long qualificacaoId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    RegistroEntidade sharedRegistro = sharedRegistro(scope, registro);
    Long sharedRegistroId = sharedRegistro.getId();
    Long sharedEmpresaId = sharedRegistro.getEmpresaId();
    EntidadeQualificacaoItem entity = qualificacaoItemRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(qualificacaoId, scope.tenantId(), sharedEmpresaId, sharedRegistroId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_qualificacao_not_found"));
    qualificacaoItemRepository.delete(entity);
  }

  @Transactional(readOnly = true)
  public EntidadeRhOptionsResponse loadRhOptions(Long tipoEntidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    return new EntidadeRhOptionsResponse(
      queryOptions(scope.tenantId(), "tipo_frequencia_cobranca", "nome"),
      queryOptions(scope.tenantId(), "rh_tipo_funcionario", "descricao"),
      queryOptions(scope.tenantId(), "rh_situacao_funcionario", "descricao"),
      queryOptions(scope.tenantId(), "rh_setor", "nome"),
      queryOptions(scope.tenantId(), "rh_cargo", "nome"),
      queryOptions(scope.tenantId(), "rh_ocupacao_atividade", "nome"),
      queryOptions(scope.tenantId(), "rh_qualificacao", "nome")
    );
  }

  private RegistroEntidade getRegistro(RegistroEntidadeContextoService.RegistroEntidadeScope scope, Long entidadeId) {
    RegistroEntidade registro = registroRepository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(entidadeId, scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("registro_entidade_not_found"));
    if (!scope.empresaId().equals(registro.getEmpresaId())) {
      throw new EntityNotFoundException("registro_entidade_not_found");
    }
    return registro;
  }

  private RegistroEntidade sharedRegistro(RegistroEntidadeContextoService.RegistroEntidadeScope scope, RegistroEntidade registro) {
    List<RegistroEntidade> rows = registroRepository.findAllByTenantIdAndPessoaIdOrderByIdAsc(
      scope.tenantId(), registro.getPessoaId());
    if (!rows.isEmpty() && rows.get(0) != null && rows.get(0).getId() != null) {
      return rows.get(0);
    }
    return registro;
  }

  private void applyReferencia(EntidadeReferencia entity, EntidadeReferenciaRequest request) {
    String nome = trim(request.nome(), 120);
    if (nome == null) throw new IllegalArgumentException("entidade_referencia_nome_required");
    if (request.dataInicio() != null && request.dataFim() != null && request.dataInicio().isAfter(request.dataFim())) {
      throw new IllegalArgumentException("entidade_referencia_periodo_invalid");
    }
    entity.setNome(nome);
    entity.setAtividades(trim(request.atividades(), 10000));
    entity.setDataInicio(request.dataInicio());
    entity.setDataFim(request.dataFim());
  }

  private void applyQualificacao(Long tenantId, EntidadeQualificacaoItem entity, EntidadeQualificacaoItemRequest request) {
    if (request.rhQualificacaoId() == null || request.rhQualificacaoId() <= 0) {
      throw new IllegalArgumentException("entidade_qualificacao_rh_required");
    }
    entity.setRhQualificacaoId(validateOptionId(tenantId, request.rhQualificacaoId(), "rh_qualificacao", "entidade_qualificacao_rh_not_found"));
    entity.setCompleto(Boolean.TRUE.equals(request.completo()));
    entity.setTipo(trim(request.tipo(), 1));
  }

  private EntidadeInfoComercialResponse toInfoComercialResponse(EntidadeInfoComercial e) {
    return new EntidadeInfoComercialResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getFaturamentoDiaInicial(), e.getFaturamentoDiaFinal(),
      e.getFaturamentoDiasPrazo(), e.getPrazoEntregaDias(), e.isBoletosEnviarEmail(), e.getFaturamentoFrequenciaCobrancaId(),
      e.getJuroTaxaPadrao(), e.getRamoAtividade(), e.isConsumidorFinal(), e.getVersion());
  }

  private EntidadeDadosFiscaisResponse toDadosFiscaisResponse(EntidadeDadosFiscais e) {
    return new EntidadeDadosFiscaisResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getManifestarNotaAutomaticamente(),
      e.getUsaNotaFiscalFatura(), e.getIgnorarImportacaoNota(), e.getVersion());
  }

  private EntidadeContratoRhResponse toContratoRhResponse(EntidadeContratoRh e) {
    return new EntidadeContratoRhResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getNumero(), e.getAdmissaoData(), e.getRemuneracao(),
      e.getRemuneracaoComplementar(), e.getBonificacao(), e.isSindicalizado(),
      e.getPercentualInsalubridade(), e.getPercentualPericulosidade(),
      e.getTipoFuncionarioId(), e.getSituacaoFuncionarioId(), e.getSetorId(), e.getCargoId(),
      e.getOcupacaoAtividadeId(), e.getVersion());
  }

  private EntidadeInfoRhResponse toInfoRhResponse(EntidadeInfoRh e) {
    return new EntidadeInfoRhResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getAtividades(), e.getHabilidades(), e.getExperiencias(),
      e.isAceitaViajar(), e.isPossuiCarro(), e.isPossuiMoto(), e.getMetaMediaHorasVendidasDia(),
      e.getMetaProdutosVendidos(), e.getVersion());
  }

  private EntidadeReferenciaResponse toReferenciaResponse(EntidadeReferencia e) {
    return new EntidadeReferenciaResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getNome(), e.getAtividades(), e.getDataInicio(), e.getDataFim(), e.getVersion());
  }

  private EntidadeQualificacaoItemResponse toQualificacaoResponse(Long tenantId, EntidadeQualificacaoItem e) {
    String nome = jdbcTemplate.query(
      "select nome from rh_qualificacao where tenant_id = ? and id = ?",
      (rs, rn) -> rs.getString("nome"),
      tenantId, e.getRhQualificacaoId()).stream().findFirst().orElse(null);
    return new EntidadeQualificacaoItemResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getRhQualificacaoId(), nome, e.isCompleto(), e.getTipo(), e.getVersion());
  }

  private List<EntidadeRhOptionResponse> queryOptions(Long tenantId, String table, String labelColumn) {
    return jdbcTemplate.query(
      "select id, " + labelColumn + " as nome from " + table + " where tenant_id = ? and ativo = true order by " + labelColumn,
      this::mapOption,
      tenantId);
  }

  private EntidadeRhOptionResponse mapOption(ResultSet rs, int rowNum) throws SQLException {
    return new EntidadeRhOptionResponse(rs.getLong("id"), rs.getString("nome"));
  }

  private Long validateOptionId(Long tenantId, Long value, String table, String notFoundCode) {
    if (value == null) return null;
    Boolean exists = jdbcTemplate.query(
      "select exists (select 1 from " + table + " where tenant_id = ? and id = ? and ativo = true)",
      (rs, rn) -> rs.getBoolean(1),
      tenantId, value).stream().findFirst().orElse(false);
    if (!Boolean.TRUE.equals(exists)) throw new EntityNotFoundException(notFoundCode);
    return value;
  }

  private BigDecimal validatePercent(BigDecimal value, String code) {
    if (value == null) return null;
    if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException(code);
    }
    return value;
  }

  private BigDecimal validateMoney(BigDecimal value, String code) {
    if (value == null) return null;
    if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException(code);
    return value;
  }

  private Short validateTriState(Short value, String code) {
    if (value == null) return null;
    if (value < 0 || value > 2) throw new IllegalArgumentException(code);
    return value;
  }

  private Integer validateNonNegativeInt(Integer value, String code) {
    if (value == null) return null;
    if (value < 0) throw new IllegalArgumentException(code);
    return value;
  }

  private String trim(String value, int maxLength) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty()) return null;
    if (normalized.length() > maxLength) throw new IllegalArgumentException("entidade_field_too_long");
    return normalized;
  }
}
