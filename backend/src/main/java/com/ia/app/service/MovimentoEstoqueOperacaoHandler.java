package com.ia.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoEstoqueCreateRequest;
import com.ia.app.dto.MovimentoEstoqueResponse;
import com.ia.app.dto.MovimentoEstoqueTemplateResponse;
import com.ia.app.dto.MovimentoEstoqueUpdateRequest;
import com.ia.app.dto.MovimentoTemplateRequest;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MovimentoEstoqueOperacaoHandler implements MovimentoOperacaoHandler {

  private final MovimentoEstoqueRepository repository;
  private final MovimentoConfigService movimentoConfigService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public MovimentoEstoqueOperacaoHandler(
      MovimentoEstoqueRepository repository,
      MovimentoConfigService movimentoConfigService,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.movimentoConfigService = movimentoConfigService;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Override
  public MovimentoTipo supports() {
    return MovimentoTipo.MOVIMENTO_ESTOQUE;
  }

  @Override
  @Transactional(readOnly = true)
  public MovimentoEstoqueTemplateResponse buildTemplate(MovimentoTemplateRequest request) {
    Long empresaId = requireEmpresaContext(requireEmpresaId(request == null ? null : request.empresaId()));
    MovimentoConfigResolverResponse resolver = movimentoConfigService.resolve(
      MovimentoTipo.MOVIMENTO_ESTOQUE,
      empresaId,
      null);
    return new MovimentoEstoqueTemplateResponse(
      MovimentoTipo.MOVIMENTO_ESTOQUE,
      empresaId,
      resolver.configuracaoId(),
      resolver.tipoEntidadePadraoId(),
      resolver.tiposEntidadePermitidos(),
      "",
      LocalDate.now());
  }

  @Override
  @Transactional
  public MovimentoEstoqueResponse create(JsonNode payload) {
    MovimentoEstoqueCreateRequest request = parseCreate(payload, MovimentoEstoqueCreateRequest.class);
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext(requireEmpresaId(request.empresaId()));
    String nome = normalizeNome(request.nome());

    MovimentoConfigResolverResponse resolver = movimentoConfigService.resolve(
      MovimentoTipo.MOVIMENTO_ESTOQUE,
      empresaId,
      null);

    MovimentoEstoque entity = new MovimentoEstoque();
    entity.setTenantId(tenantId);
    entity.setEmpresaId(empresaId);
    entity.setDataMovimento(request.dataMovimento() == null ? LocalDate.now() : request.dataMovimento());
    entity.setNome(nome);
    entity.setMovimentoConfigId(resolver.configuracaoId());
    entity.setTipoEntidadePadraoId(resolver.tipoEntidadePadraoId());

    MovimentoEstoque saved = repository.saveAndFlush(entity);
    auditService.log(
      tenantId,
      "MOVIMENTO_ESTOQUE_CRIADO",
      "movimento_estoque",
      String.valueOf(saved.getId()),
      "empresaId=" + saved.getEmpresaId() + ";nome=" + saved.getNome());
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MovimentoEstoqueResponse> list(
      Pageable pageable,
      String nome,
      LocalDate dataInicio,
      LocalDate dataFim) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    String normalizedNome = normalizeOptionalNomeFilter(nome);
    Pageable normalizedPageable = normalizePageable(pageable);
    Specification<MovimentoEstoque> spec = buildListSpec(tenantId, empresaId, normalizedNome, dataInicio, dataFim);
    return repository.findAll(spec, normalizedPageable)
      .map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public MovimentoEstoqueResponse get(Long id) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    MovimentoEstoque entity = findByIdForTenant(id, tenantId);
    assertEmpresaOwnership(entity, empresaId);
    return toResponse(entity);
  }

  @Override
  @Transactional
  public MovimentoEstoqueResponse update(Long id, JsonNode payload) {
    MovimentoEstoqueUpdateRequest request = parseCreate(payload, MovimentoEstoqueUpdateRequest.class);
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext(requireEmpresaId(request.empresaId()));
    String nome = normalizeNome(request.nome());

    MovimentoEstoque entity = findByIdForTenant(id, tenantId);
    assertEmpresaOwnership(entity, empresaId);
    if (request.version() == null) {
      throw new IllegalArgumentException("movimento_estoque_version_required");
    }
    if (!Objects.equals(request.version(), entity.getVersion())) {
      throw new ObjectOptimisticLockingFailureException(MovimentoEstoque.class, id);
    }

    MovimentoConfigResolverResponse resolver = movimentoConfigService.resolve(
      MovimentoTipo.MOVIMENTO_ESTOQUE,
      empresaId,
      null);

    entity.setNome(nome);
    entity.setDataMovimento(request.dataMovimento() == null ? LocalDate.now() : request.dataMovimento());
    entity.setMovimentoConfigId(resolver.configuracaoId());
    entity.setTipoEntidadePadraoId(resolver.tipoEntidadePadraoId());

    MovimentoEstoque saved = repository.saveAndFlush(entity);
    auditService.log(
      tenantId,
      "MOVIMENTO_ESTOQUE_ATUALIZADO",
      "movimento_estoque",
      String.valueOf(saved.getId()),
      "empresaId=" + saved.getEmpresaId() + ";nome=" + saved.getNome());
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    MovimentoEstoque entity = findByIdForTenant(id, tenantId);
    assertEmpresaOwnership(entity, empresaId);
    repository.delete(entity);
    auditService.log(
      tenantId,
      "MOVIMENTO_ESTOQUE_EXCLUIDO",
      "movimento_estoque",
      String.valueOf(entity.getId()),
      "empresaId=" + entity.getEmpresaId() + ";nome=" + entity.getNome());
  }

  private MovimentoEstoque findByIdForTenant(Long id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_not_found"));
  }

  private void assertEmpresaOwnership(MovimentoEstoque entity, Long empresaId) {
    if (!Objects.equals(entity.getEmpresaId(), empresaId)) {
      throw new EntityNotFoundException("movimento_estoque_not_found");
    }
  }

  private Long requireEmpresaId(Long empresaId) {
    if (empresaId == null || empresaId <= 0) {
      throw new IllegalArgumentException("movimento_empresa_id_required");
    }
    return empresaId;
  }

  private Long requireEmpresaContext(Long empresaId) {
    Long contextId = requireEmpresaContext();
    if (!Objects.equals(contextId, empresaId)) {
      throw new IllegalArgumentException("movimento_empresa_context_mismatch");
    }
    return contextId;
  }

  private Long requireEmpresaContext() {
    Long empresaContextId = EmpresaContext.getEmpresaId();
    if (empresaContextId == null || empresaContextId <= 0) {
      throw new IllegalArgumentException("movimento_empresa_context_required");
    }
    return empresaContextId;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("movimento_estoque_nome_required");
    }
    String value = nome.trim();
    return value.length() > 120 ? value.substring(0, 120) : value;
  }

  private String normalizeOptionalNomeFilter(String nome) {
    if (nome == null || nome.isBlank()) {
      return null;
    }
    return nome.trim();
  }

  private Pageable normalizePageable(Pageable pageable) {
    int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
    int size = pageable == null ? 20 : Math.max(pageable.getPageSize(), 1);
    return PageRequest.of(
      page,
      size,
      Sort.by(
        Sort.Order.desc("dataMovimento").nullsLast(),
        Sort.Order.desc("id")));
  }

  private Specification<MovimentoEstoque> buildListSpec(
      Long tenantId,
      Long empresaId,
      String nome,
      LocalDate dataInicio,
      LocalDate dataFim) {
    Specification<MovimentoEstoque> spec = (root, query, cb) -> cb.and(
      cb.equal(root.get("tenantId"), tenantId),
      cb.equal(root.get("empresaId"), empresaId));
    if (nome != null) {
      spec = spec.and((root, query, cb) -> cb.like(
        cb.lower(root.get("nome")),
        "%" + nome.toLowerCase() + "%"));
    }
    if (dataInicio != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataMovimento"), dataInicio));
    }
    if (dataFim != null) {
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataMovimento"), dataFim));
    }
    return spec;
  }

  private <T> T parseCreate(JsonNode payload, Class<T> type) {
    if (payload == null || payload.isNull()) {
      throw new IllegalArgumentException("movimento_payload_required");
    }
    try {
      return objectMapper.treeToValue(payload, type);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("movimento_payload_required");
    }
  }

  private MovimentoEstoqueResponse toResponse(MovimentoEstoque entity) {
    return new MovimentoEstoqueResponse(
      entity.getId(),
      entity.getTipoMovimento(),
      entity.getEmpresaId(),
      entity.getNome(),
      entity.getDataMovimento(),
      entity.getMovimentoConfigId(),
      entity.getTipoEntidadePadraoId(),
      entity.getVersion(),
      entity.getCreatedAt(),
      entity.getUpdatedAt());
  }
}
