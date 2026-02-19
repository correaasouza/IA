package com.ia.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogStockAdjustment;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.CatalogStockAdjustmentResponse;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoEstoqueCreateRequest;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.MovimentoEstoqueItemResponse;
import com.ia.app.dto.MovimentoEstoqueResponse;
import com.ia.app.dto.MovimentoEstoqueTemplateResponse;
import com.ia.app.dto.MovimentoStockAdjustmentOptionResponse;
import com.ia.app.dto.MovimentoEstoqueUpdateRequest;
import com.ia.app.dto.MovimentoTemplateRequest;
import com.ia.app.dto.MovimentoTipoItemTemplateResponse;
import com.ia.app.repository.CatalogStockAdjustmentRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.service.WorkflowRuntimeService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MovimentoEstoqueOperacaoHandler implements MovimentoOperacaoHandler {

  private final MovimentoEstoqueRepository repository;
  private final MovimentoEstoqueItemRepository itemRepository;
  private final MovimentoConfigService movimentoConfigService;
  private final ObjectProvider<MovimentoConfigItemTipoService> movimentoConfigItemTipoServiceProvider;
  private final ObjectProvider<MovimentoEstoqueItemCatalogService> movimentoEstoqueItemCatalogServiceProvider;
  private final ObjectProvider<MovimentoItemTipoService> movimentoItemTipoServiceProvider;
  private final ObjectProvider<CatalogStockAdjustmentConfigurationService> stockAdjustmentConfigurationServiceProvider;
  private final CatalogStockAdjustmentRepository stockAdjustmentRepository;
  private final ObjectProvider<WorkflowRuntimeService> workflowRuntimeServiceProvider;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public MovimentoEstoqueOperacaoHandler(
      MovimentoEstoqueRepository repository,
      MovimentoEstoqueItemRepository itemRepository,
      MovimentoConfigService movimentoConfigService,
      ObjectProvider<MovimentoConfigItemTipoService> movimentoConfigItemTipoServiceProvider,
      ObjectProvider<MovimentoEstoqueItemCatalogService> movimentoEstoqueItemCatalogServiceProvider,
      ObjectProvider<MovimentoItemTipoService> movimentoItemTipoServiceProvider,
      ObjectProvider<CatalogStockAdjustmentConfigurationService> stockAdjustmentConfigurationServiceProvider,
      CatalogStockAdjustmentRepository stockAdjustmentRepository,
      ObjectProvider<WorkflowRuntimeService> workflowRuntimeServiceProvider,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.itemRepository = itemRepository;
    this.movimentoConfigService = movimentoConfigService;
    this.movimentoConfigItemTipoServiceProvider = movimentoConfigItemTipoServiceProvider;
    this.movimentoEstoqueItemCatalogServiceProvider = movimentoEstoqueItemCatalogServiceProvider;
    this.movimentoItemTipoServiceProvider = movimentoItemTipoServiceProvider;
    this.stockAdjustmentConfigurationServiceProvider = stockAdjustmentConfigurationServiceProvider;
    this.stockAdjustmentRepository = stockAdjustmentRepository;
    this.workflowRuntimeServiceProvider = workflowRuntimeServiceProvider;
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

    List<MovimentoTipoItemTemplateResponse> tiposItens = List.of();
    MovimentoConfigItemTipoService itemTipoService = movimentoConfigItemTipoServiceProvider.getIfAvailable();
    if (itemTipoService != null) {
      tiposItens = itemTipoService.listAtivosForConfig(resolver.configuracaoId())
        .stream()
        .map(item -> new MovimentoTipoItemTemplateResponse(
          item.movimentoItemTipoId(),
          item.nome(),
          item.catalogType(),
          item.cobrar()))
        .toList();
    }

    List<MovimentoStockAdjustmentOptionResponse> adjustments = listStockAdjustments();

    return new MovimentoEstoqueTemplateResponse(
      MovimentoTipo.MOVIMENTO_ESTOQUE,
      empresaId,
      resolver.configuracaoId(),
      resolver.tipoEntidadePadraoId(),
      null,
      adjustments,
      resolver.tiposEntidadePermitidos(),
      tiposItens,
      "");
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
    Long tipoEntidadeId = resolveTipoEntidadeId(resolver, request.tipoEntidadeId());
    Long stockAdjustmentId = resolveStockAdjustmentId(tenantId, request.stockAdjustmentId());

    MovimentoEstoque entity = new MovimentoEstoque();
    entity.setTenantId(tenantId);
    entity.setEmpresaId(empresaId);
    entity.setNome(nome);
    entity.setMovimentoConfigId(resolver.configuracaoId());
    entity.setTipoEntidadePadraoId(tipoEntidadeId);
    entity.setStockAdjustmentId(stockAdjustmentId);

    MovimentoEstoque saved = repository.saveAndFlush(entity);
    replaceItens(saved, request.itens());
    initializeWorkflowInstances(saved);

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
      String nome) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    String normalizedNome = normalizeOptionalNomeFilter(nome);
    Pageable normalizedPageable = normalizePageable(pageable);
    Specification<MovimentoEstoque> spec = buildListSpec(tenantId, empresaId, normalizedNome);
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
    Long tipoEntidadeId = resolveTipoEntidadeId(resolver, request.tipoEntidadeId());
    Long stockAdjustmentId = resolveStockAdjustmentId(tenantId, request.stockAdjustmentId());

    entity.setNome(nome);
    entity.setMovimentoConfigId(resolver.configuracaoId());
    entity.setTipoEntidadePadraoId(tipoEntidadeId);
    entity.setStockAdjustmentId(stockAdjustmentId);

    MovimentoEstoque saved = repository.saveAndFlush(entity);
    replaceItens(saved, request.itens());
    initializeWorkflowInstances(saved);

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
    itemRepository.deleteAllByTenantIdAndMovimentoEstoqueId(tenantId, entity.getId());
    repository.delete(entity);
    auditService.log(
      tenantId,
      "MOVIMENTO_ESTOQUE_EXCLUIDO",
      "movimento_estoque",
      String.valueOf(entity.getId()),
      "empresaId=" + entity.getEmpresaId() + ";nome=" + entity.getNome());
  }

  private void replaceItens(MovimentoEstoque movimento, List<MovimentoEstoqueItemRequest> itens) {
    Long tenantId = movimento.getTenantId();
    itemRepository.deleteAllByTenantIdAndMovimentoEstoqueId(tenantId, movimento.getId());
    if (itens == null || itens.isEmpty()) {
      return;
    }
    List<MovimentoEstoqueItem> entities = new ArrayList<>();
    int pos = 0;
    MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService = requireCatalogService();
    for (MovimentoEstoqueItemRequest request : itens) {
      MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolved = movimentoEstoqueItemCatalogService
        .resolveItem(movimento.getMovimentoConfigId(), request, pos);

      MovimentoEstoqueItem entity = new MovimentoEstoqueItem();
      entity.setTenantId(tenantId);
      entity.setMovimentoEstoqueId(movimento.getId());
      entity.setMovimentoItemTipoId(resolved.movimentoItemTipoId());
      entity.setCatalogType(resolved.catalogType());
      entity.setCatalogItemId(resolved.catalogItemId());
      entity.setCatalogCodigoSnapshot(resolved.catalogCodigoSnapshot());
      entity.setCatalogNomeSnapshot(resolved.catalogNomeSnapshot());
      entity.setQuantidade(resolved.quantidade());
      entity.setValorUnitario(resolved.valorUnitario());
      entity.setValorTotal(resolved.valorTotal());
      entity.setCobrar(resolved.cobrar());
      entity.setOrdem(resolved.ordem());
      entity.setObservacao(resolved.observacao());
      entities.add(entity);
      pos += 1;
    }
    itemRepository.saveAll(entities);
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
      Sort.by(Sort.Order.desc("id")));
  }

  private Specification<MovimentoEstoque> buildListSpec(
      Long tenantId,
      Long empresaId,
      String nome) {
    Specification<MovimentoEstoque> spec = (root, query, cb) -> cb.and(
      cb.equal(root.get("tenantId"), tenantId),
      cb.equal(root.get("empresaId"), empresaId));
    if (nome != null) {
      spec = spec.and((root, query, cb) -> cb.like(
        cb.lower(root.get("nome")),
        "%" + nome.toLowerCase() + "%"));
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
    List<MovimentoEstoqueItemResponse> itens = itemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(entity.getTenantId(), entity.getId())
      .stream()
      .map(item -> new MovimentoEstoqueItemResponse(
        item.getId(),
        item.getMovimentoItemTipoId(),
        resolveTipoItemNome(item.getMovimentoItemTipoId()),
        item.getCatalogType(),
        item.getCatalogItemId(),
        item.getCatalogCodigoSnapshot(),
        item.getCatalogNomeSnapshot(),
        item.getQuantidade(),
        item.getValorUnitario(),
        item.getValorTotal(),
        item.isCobrar(),
        item.getStatus(),
        item.getOrdem(),
        item.getObservacao()))
      .toList();

    BigDecimal totalCobrado = itens.stream()
      .map(MovimentoEstoqueItemResponse::valorTotal)
      .filter(Objects::nonNull)
      .reduce(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), BigDecimal::add);

    return new MovimentoEstoqueResponse(
      entity.getId(),
      entity.getTipoMovimento(),
      entity.getEmpresaId(),
      entity.getNome(),
      entity.getMovimentoConfigId(),
      entity.getTipoEntidadePadraoId(),
      entity.getStockAdjustmentId(),
      entity.getStatus(),
      itens,
      itens.size(),
      totalCobrado,
      entity.getVersion(),
      entity.getCreatedAt(),
      entity.getUpdatedAt());
  }

  private MovimentoEstoqueItemCatalogService requireCatalogService() {
    MovimentoEstoqueItemCatalogService service = movimentoEstoqueItemCatalogServiceProvider.getIfAvailable();
    if (service == null) {
      throw new IllegalStateException("movimento_estoque_item_service_required");
    }
    return service;
  }

  private List<MovimentoStockAdjustmentOptionResponse> listStockAdjustments() {
    CatalogStockAdjustmentConfigurationService stockAdjustmentConfigurationService = stockAdjustmentConfigurationServiceProvider.getIfAvailable();
    if (stockAdjustmentConfigurationService == null) {
      return List.of();
    }
    List<CatalogStockAdjustmentResponse> products = stockAdjustmentConfigurationService.listByType(CatalogConfigurationType.PRODUCTS);
    List<CatalogStockAdjustmentResponse> services = stockAdjustmentConfigurationService.listByType(CatalogConfigurationType.SERVICES);
    return Stream.concat(
      products.stream().map(item -> new MovimentoStockAdjustmentOptionResponse(
        item.id(),
        item.codigo(),
        item.nome(),
        item.tipo(),
        CatalogConfigurationType.PRODUCTS.name())),
      services.stream().map(item -> new MovimentoStockAdjustmentOptionResponse(
        item.id(),
        item.codigo(),
        item.nome(),
        item.tipo(),
        CatalogConfigurationType.SERVICES.name())))
      .distinct()
      .toList();
  }

  private Long resolveStockAdjustmentId(Long tenantId, Long requestedStockAdjustmentId) {
    if (requestedStockAdjustmentId == null) {
      return null;
    }
    CatalogStockAdjustment adjustment = stockAdjustmentRepository
      .findByIdAndTenantIdAndActiveTrue(requestedStockAdjustmentId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_stock_adjustment_invalid"));
    return adjustment.getId();
  }

  private void initializeWorkflowInstances(MovimentoEstoque movimento) {
    WorkflowRuntimeService workflowRuntimeService = workflowRuntimeServiceProvider.getIfAvailable();
    if (workflowRuntimeService == null || movimento == null || movimento.getId() == null) {
      return;
    }
    workflowRuntimeService.ensureInstanceForOrigin(WorkflowOrigin.MOVIMENTO_ESTOQUE, movimento.getId());
    List<MovimentoEstoqueItem> items = itemRepository.findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(
      movimento.getTenantId(),
      movimento.getId());
    for (MovimentoEstoqueItem item : items) {
      if (item == null || item.getId() == null) {
        continue;
      }
      workflowRuntimeService.ensureInstanceForOrigin(WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE, item.getId());
    }
  }

  private Long resolveTipoEntidadeId(MovimentoConfigResolverResponse resolver, Long requestedTipoEntidadeId) {
    List<Long> allowedList = resolver.tiposEntidadePermitidos() == null ? List.of() : resolver.tiposEntidadePermitidos();
    Set<Long> allowed = new LinkedHashSet<>();
    for (Long value : allowedList) {
      if (value != null && value > 0) {
        allowed.add(value);
      }
    }
    if (requestedTipoEntidadeId != null) {
      if (requestedTipoEntidadeId <= 0) {
        throw new IllegalArgumentException("movimento_estoque_tipo_entidade_invalid");
      }
      if (!allowed.contains(requestedTipoEntidadeId)) {
        throw new IllegalArgumentException("movimento_estoque_tipo_entidade_invalid");
      }
      return requestedTipoEntidadeId;
    }
    Long defaultTipoEntidadeId = resolver.tipoEntidadePadraoId();
    if (defaultTipoEntidadeId != null) {
      if (defaultTipoEntidadeId <= 0) {
        throw new IllegalArgumentException("movimento_estoque_tipo_entidade_invalid");
      }
      if (allowed.isEmpty() || allowed.contains(defaultTipoEntidadeId)) {
        return defaultTipoEntidadeId;
      }
    }
    if (allowed.size() == 1) {
      return allowed.iterator().next();
    }
    if (allowed.size() > 1) {
      throw new IllegalArgumentException("movimento_estoque_tipo_entidade_required");
    }
    return null;
  }

  private String resolveTipoItemNome(Long tipoItemId) {
    MovimentoItemTipoService service = movimentoItemTipoServiceProvider.getIfAvailable();
    if (service == null || tipoItemId == null) {
      return "-";
    }
    try {
      return service.requireById(tipoItemId).getNome();
    } catch (Exception ex) {
      return "-";
    }
  }
}
