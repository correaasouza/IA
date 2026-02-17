package com.ia.app.service;

import com.ia.app.domain.Empresa;
import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.MovimentoConfigDuplicarRequest;
import com.ia.app.dto.MovimentoConfigCoverageWarningResponse;
import com.ia.app.dto.MovimentoConfigRequest;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoConfigResponse;
import com.ia.app.dto.MovimentoTipoResponse;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoConfigService {

  private static final int PRIORIDADE_PADRAO = 100;

  private final MovimentoConfigRepository repository;
  private final EmpresaRepository empresaRepository;
  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final AuditService auditService;

  public MovimentoConfigService(
      MovimentoConfigRepository repository,
      EmpresaRepository empresaRepository,
      TipoEntidadeRepository tipoEntidadeRepository,
      AuditService auditService) {
    this.repository = repository;
    this.empresaRepository = empresaRepository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<MovimentoTipoResponse> listTipos() {
    return Arrays.stream(MovimentoTipo.values())
      .map(tipo -> new MovimentoTipoResponse(tipo.name(), tipo.descricao()))
      .toList();
  }

  @Transactional(readOnly = true)
  public Page<MovimentoConfigResponse> listByTipo(MovimentoTipo tipoMovimento, Pageable pageable) {
    Long tenantId = requireTenant();
    return repository
      .findAllByTenantIdAndTipoMovimentoOrderByUpdatedAtDescIdDesc(tenantId, tipoMovimento, pageable)
      .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public MovimentoConfigResponse getById(Long id) {
    Long tenantId = requireTenant();
    MovimentoConfig entity = findByIdForTenant(id, tenantId);
    return toResponse(entity);
  }

  @Transactional
  public MovimentoConfigResponse create(MovimentoConfigRequest request) {
    Long tenantId = requireTenant();
    NormalizedPayload payload = normalizeAndValidatePayload(tenantId, request);

    MovimentoConfig entity = new MovimentoConfig();
    entity.setTenantId(tenantId);
    applyPayload(entity, payload);
    validateConflict(entity, payload.empresaIds(), null);

    try {
      MovimentoConfig saved = repository.saveAndFlush(entity);
      auditService.log(
        tenantId,
        "MOVIMENTO_CONFIG_CRIADA",
        "movimento_config",
        String.valueOf(saved.getId()),
        "tipo=" + saved.getTipoMovimento()
          + ";nome=" + saved.getNome()
          + ";ativo=" + saved.isAtivo());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  @Transactional
  public MovimentoConfigResponse update(Long id, MovimentoConfigRequest request) {
    Long tenantId = requireTenant();
    MovimentoConfig entity = findByIdForTenant(id, tenantId);
    NormalizedPayload payload = normalizeAndValidatePayload(tenantId, request);

    applyPayload(entity, payload);
    validateConflict(entity, payload.empresaIds(), entity.getId());

    try {
      MovimentoConfig saved = repository.saveAndFlush(entity);
      auditService.log(
        tenantId,
        "MOVIMENTO_CONFIG_ATUALIZADA",
        "movimento_config",
        String.valueOf(saved.getId()),
        "tipo=" + saved.getTipoMovimento()
          + ";nome=" + saved.getNome()
          + ";ativo=" + saved.isAtivo());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  @Transactional
  public MovimentoConfigResponse duplicar(Long id, MovimentoConfigDuplicarRequest request) {
    Long tenantId = requireTenant();
    MovimentoConfig origem = findByIdForTenant(id, tenantId);

    MovimentoConfig clone = new MovimentoConfig();
    clone.setTenantId(tenantId);
    clone.setTipoMovimento(origem.getTipoMovimento());
    clone.setNome(normalizeDuplicateName(request == null ? null : request.nome(), origem.getNome()));
    clone.setDescricao(null);
    clone.setPrioridade(PRIORIDADE_PADRAO);
    clone.setContextoKey(request == null ? origem.getContextoKey() : normalizeContexto(request.contextoKey()));
    clone.setTipoEntidadePadraoId(origem.getTipoEntidadePadraoId());
    clone.setAtivo(request != null && request.ativo() != null ? request.ativo() : false);
    clone.replaceEmpresas(collectEmpresaIds(origem));
    clone.replaceTiposEntidadePermitidos(collectTipoEntidadeIds(origem));

    Set<Long> empresaIds = new LinkedHashSet<>(collectEmpresaIds(clone));
    validateConflict(clone, empresaIds, null);

    try {
      MovimentoConfig saved = repository.saveAndFlush(clone);
      auditService.log(
        tenantId,
        "MOVIMENTO_CONFIG_DUPLICADA",
        "movimento_config",
        String.valueOf(saved.getId()),
        "origemId=" + origem.getId() + ";tipo=" + saved.getTipoMovimento() + ";nome=" + saved.getNome());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    MovimentoConfig entity = findByIdForTenant(id, tenantId);
    if (!entity.isAtivo()) {
      return;
    }
    entity.setAtivo(false);
    repository.save(entity);
    auditService.log(
      tenantId,
      "MOVIMENTO_CONFIG_INATIVADA",
      "movimento_config",
      String.valueOf(entity.getId()),
      "tipo=" + entity.getTipoMovimento() + ";nome=" + entity.getNome());
  }

  @Transactional(readOnly = true)
  public MovimentoConfigResolverResponse resolve(MovimentoTipo tipoMovimento, Long empresaId, String contextoKey) {
    Long tenantId = requireTenant();
    validateEmpresaExists(tenantId, Set.of(requirePositiveId(empresaId, "movimento_config_empresa_id_invalid")));
    String contexto = normalizeContexto(contextoKey);
    boolean contextoInformado = contexto != null;

    List<MovimentoConfig> candidatos = repository.findCandidatesForResolver(
      tenantId,
      tipoMovimento,
      empresaId,
      contexto,
      contextoInformado);

    if (candidatos.isEmpty()) {
      throw new IllegalArgumentException("movimento_config_nao_encontrada");
    }

    if (hasResolverConflict(candidatos)) {
      throw new IllegalArgumentException("movimento_config_conflito_resolucao");
    }

    MovimentoConfig selected = candidatos.get(0);
    return new MovimentoConfigResolverResponse(
      selected.getId(),
      selected.getTipoMovimento(),
      empresaId,
      contexto,
      selected.getTipoEntidadePadraoId(),
      collectTipoEntidadeIds(selected));
  }

  @Transactional(readOnly = true)
  public List<MovimentoTipoResponse> listMenuTiposForEmpresa(Long empresaId) {
    Long tenantId = requireTenant();
    Long normalizedEmpresaId = requirePositiveId(empresaId, "movimento_config_empresa_id_invalid");
    validateEmpresaExists(tenantId, Set.of(normalizedEmpresaId));
    Set<MovimentoTipo> tiposAtivos = new LinkedHashSet<>(
      repository.findTiposAtivosByTenantAndEmpresa(tenantId, normalizedEmpresaId));
    return Arrays.stream(MovimentoTipo.values())
      .filter(tiposAtivos::contains)
      .map(tipo -> new MovimentoTipoResponse(tipo.name(), tipo.descricao()))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<MovimentoConfigCoverageWarningResponse> listCoverageWarnings() {
    Long tenantId = requireTenant();
    List<Empresa> empresas = empresaRepository.findAllByTenantIdAndAtivoTrueOrderByRazaoSocialAsc(tenantId);
    if (empresas.isEmpty()) {
      return List.of();
    }
    List<MovimentoConfigCoverageWarningResponse> warnings = new java.util.ArrayList<>();
    for (Empresa empresa : empresas) {
      for (MovimentoTipo tipo : MovimentoTipo.values()) {
        boolean covered = repository.existsActiveGlobalByTenantAndTipoAndEmpresa(tenantId, tipo, empresa.getId());
        if (covered) {
          continue;
        }
        warnings.add(new MovimentoConfigCoverageWarningResponse(
          empresa.getId(),
          resolveEmpresaNome(empresa),
          tipo,
          "Sem configuracao ativa de fallback para empresa e tipo."));
      }
    }
    return warnings;
  }

  private void applyPayload(MovimentoConfig entity, NormalizedPayload payload) {
    entity.setTipoMovimento(payload.tipoMovimento());
    entity.setNome(payload.nome());
    entity.setDescricao(null);
    entity.setPrioridade(PRIORIDADE_PADRAO);
    entity.setContextoKey(payload.contextoKey());
    entity.setTipoEntidadePadraoId(payload.tipoEntidadePadraoId());
    entity.setAtivo(payload.ativo());
    entity.replaceEmpresas(payload.empresaIds());
    entity.replaceTiposEntidadePermitidos(payload.tiposEntidadePermitidos());
  }

  private NormalizedPayload normalizeAndValidatePayload(Long tenantId, MovimentoConfigRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("movimento_config_payload_required");
    }
    MovimentoTipo tipoMovimento = request.tipoMovimento();
    if (tipoMovimento == null) {
      throw new IllegalArgumentException("movimento_tipo_invalid");
    }
    String nome = normalizeNome(request.nome());
    String contextoKey = normalizeContexto(request.contextoKey());
    boolean ativo = request.ativo() == null || request.ativo();
    Set<Long> empresaIds = normalizePositiveIds(request.empresaIds(), "movimento_config_empresa_ids_required");
    Set<Long> tiposEntidadePermitidos =
      normalizePositiveIds(request.tiposEntidadePermitidos(), "movimento_config_tipos_entidade_required");
    Long tipoEntidadePadraoId = requirePositiveId(
      request.tipoEntidadePadraoId(),
      "movimento_config_tipo_entidade_padrao_required");

    if (!tiposEntidadePermitidos.contains(tipoEntidadePadraoId)) {
      throw new IllegalArgumentException("movimento_config_tipo_padrao_fora_permitidos");
    }

    validateEmpresaExists(tenantId, empresaIds);
    validateTipoEntidadeExists(tenantId, tiposEntidadePermitidos);

    return new NormalizedPayload(
      tipoMovimento,
      nome,
      contextoKey,
      ativo,
      empresaIds,
      tiposEntidadePermitidos,
      tipoEntidadePadraoId);
  }

  private void validateConflict(MovimentoConfig entity, Set<Long> empresaIds, Long ignoreId) {
    if (!entity.isAtivo()) {
      return;
    }
    long conflicts = repository.countActiveConflicts(
      entity.getTenantId(),
      entity.getTipoMovimento(),
      entity.getContextoKey(),
      empresaIds,
      ignoreId);
    if (conflicts > 0) {
      throw new IllegalArgumentException("movimento_config_conflito_prioridade_contexto_empresa");
    }
  }

  private boolean hasResolverConflict(List<MovimentoConfig> candidatos) {
    if (candidatos.size() < 2) {
      return false;
    }
    MovimentoConfig first = candidatos.get(0);
    MovimentoConfig second = candidatos.get(1);
    String firstContexto = normalizeContexto(first.getContextoKey());
    String secondContexto = normalizeContexto(second.getContextoKey());
    if (firstContexto == null && secondContexto == null) {
      return true;
    }
    return firstContexto != null && firstContexto.equals(secondContexto);
  }

  private MovimentoConfig findByIdForTenant(Long id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_config_not_found"));
  }

  private Set<Long> normalizePositiveIds(List<Long> ids, String requiredErrorCode) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException(requiredErrorCode);
    }
    Set<Long> normalized = ids.stream()
      .filter(value -> value != null && value > 0)
      .collect(Collectors.toCollection(LinkedHashSet::new));
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(requiredErrorCode);
    }
    return normalized;
  }

  private Long requirePositiveId(Long value, String errorCode) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private void validateEmpresaExists(Long tenantId, Set<Long> empresaIds) {
    List<Empresa> empresas = empresaRepository.findAllByTenantIdAndIdIn(tenantId, empresaIds);
    if (empresas.size() != empresaIds.size()) {
      throw new IllegalArgumentException("movimento_config_empresa_invalida");
    }
  }

  private void validateTipoEntidadeExists(Long tenantId, Set<Long> tipoEntidadeIds) {
    List<TipoEntidade> tipos = tipoEntidadeRepository.findAllByTenantIdAndIdIn(tenantId, tipoEntidadeIds);
    if (tipos.size() != tipoEntidadeIds.size()) {
      throw new IllegalArgumentException("movimento_config_tipo_entidade_invalido");
    }
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("movimento_config_nome_required");
    }
    String value = nome.trim();
    return value.length() > 120 ? value.substring(0, 120) : value;
  }

  private String normalizeContexto(String contextoKey) {
    if (contextoKey == null) {
      return null;
    }
    String value = contextoKey.trim();
    if (value.isEmpty()) {
      return null;
    }
    value = value.length() > 120 ? value.substring(0, 120) : value;
    return value.toUpperCase(Locale.ROOT);
  }

  private String normalizeDuplicateName(String requestedName, String baseName) {
    String source = requestedName == null || requestedName.isBlank()
      ? baseName + " - Copia"
      : requestedName.trim();
    return source.length() > 120 ? source.substring(0, 120) : source;
  }

  private List<Long> collectEmpresaIds(MovimentoConfig entity) {
    return entity.getEmpresas().stream()
      .map(item -> item.getEmpresaId())
      .filter(id -> id != null && id > 0)
      .distinct()
      .sorted()
      .toList();
  }

  private List<Long> collectTipoEntidadeIds(MovimentoConfig entity) {
    return entity.getTiposEntidadePermitidos().stream()
      .map(item -> item.getTipoEntidadeId())
      .filter(id -> id != null && id > 0)
      .distinct()
      .sorted()
      .toList();
  }

  private MovimentoConfigResponse toResponse(MovimentoConfig entity) {
    return new MovimentoConfigResponse(
      entity.getId(),
      entity.getTipoMovimento(),
      entity.getNome(),
      entity.getContextoKey(),
      entity.isAtivo(),
      entity.getVersion(),
      collectEmpresaIds(entity),
      collectTipoEntidadeIds(entity),
      entity.getTipoEntidadePadraoId(),
      entity.getCreatedAt(),
      entity.getUpdatedAt());
  }

  private RuntimeException mapIntegrity(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    String normalized = message.toLowerCase(Locale.ROOT);
    if (normalized.contains("ux_movimento_config_empresa_scope")
      || normalized.contains("ux_movimento_config_tipo_entidade_scope")
      || normalized.contains("ux_movimento_config_id_tenant")) {
      return new IllegalArgumentException("movimento_config_integridade_invalida");
    }
    return ex;
  }

  private String resolveEmpresaNome(Empresa empresa) {
    if (empresa == null) {
      return "";
    }
    String fantasia = empresa.getNomeFantasia();
    if (fantasia != null && !fantasia.isBlank()) {
      return fantasia.trim();
    }
    return empresa.getRazaoSocial();
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private record NormalizedPayload(
    MovimentoTipo tipoMovimento,
    String nome,
    String contextoKey,
    boolean ativo,
    Set<Long> empresaIds,
    Set<Long> tiposEntidadePermitidos,
    Long tipoEntidadePadraoId
  ) {}
}
