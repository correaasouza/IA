package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogStockAdjustment;
import com.ia.app.domain.CatalogStockAdjustmentType;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.CatalogStockAdjustmentResponse;
import com.ia.app.dto.CatalogStockAdjustmentScopeOptionResponse;
import com.ia.app.dto.CatalogStockAdjustmentUpsertRequest;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogStockAdjustmentRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogStockAdjustmentConfigurationService {

  private final CatalogConfigurationService catalogConfigurationService;
  private final CatalogStockAdjustmentRepository repository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final AgrupadorEmpresaItemRepository agrupadorEmpresaItemRepository;
  private final CatalogStockTypeRepository stockTypeRepository;
  private final EmpresaRepository empresaRepository;
  private final AuditService auditService;

  public CatalogStockAdjustmentConfigurationService(
      CatalogConfigurationService catalogConfigurationService,
      CatalogStockAdjustmentRepository repository,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      AgrupadorEmpresaItemRepository agrupadorEmpresaItemRepository,
      CatalogStockTypeRepository stockTypeRepository,
      EmpresaRepository empresaRepository,
      AuditService auditService) {
    this.catalogConfigurationService = catalogConfigurationService;
    this.repository = repository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.agrupadorEmpresaItemRepository = agrupadorEmpresaItemRepository;
    this.stockTypeRepository = stockTypeRepository;
    this.empresaRepository = empresaRepository;
    this.auditService = auditService;
  }

  @Transactional
  public List<CatalogStockAdjustmentResponse> listByType(CatalogConfigurationType type) {
    Scope scope = resolveScopeForRead(type);
    if (scope == null) {
      return List.of();
    }
    return repository
      .findAllByTenantIdAndCatalogConfigurationIdOrderByOrdemAscNomeAsc(
        scope.tenantId(),
        scope.configId())
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<CatalogStockAdjustmentScopeOptionResponse> listScopeOptions(CatalogConfigurationType type) {
    Scope scope = resolveScope(type);
    List<AgrupadorEmpresa> grupos = agrupadorEmpresaRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
        scope.tenantId(),
        ConfiguracaoScopeService.TYPE_CATALOGO,
        scope.configId());

    Collection<Long> empresaIds = grupos.stream()
      .flatMap(g -> g.getItens().stream().map(item -> item.getEmpresaId()))
      .collect(Collectors.toSet());
    Map<Long, Empresa> empresaById = new HashMap<>();
    if (!empresaIds.isEmpty()) {
      empresaById = empresaRepository.findAllByTenantIdAndIdIn(scope.tenantId(), empresaIds)
        .stream()
        .collect(Collectors.toMap(Empresa::getId, e -> e));
    }

    List<CatalogStockAdjustmentScopeOptionResponse> options = new ArrayList<>();
    for (AgrupadorEmpresa grupo : grupos) {
      List<CatalogStockType> stockTypes = stockTypeRepository
        .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
          scope.tenantId(),
          scope.configId(),
          grupo.getId());
      if (stockTypes.isEmpty()) {
        continue;
      }
      for (var item : grupo.getItens()) {
        Empresa empresa = empresaById.get(item.getEmpresaId());
        String filialNome = empresa == null ? ("Empresa #" + item.getEmpresaId()) : empresa.getRazaoSocial();
        for (CatalogStockType stockType : stockTypes) {
          String label = grupo.getNome()
            + " | "
            + stockType.getCodigo()
            + " - "
            + stockType.getNome()
            + " | "
            + filialNome;
          options.add(new CatalogStockAdjustmentScopeOptionResponse(
            grupo.getId(),
            grupo.getNome(),
            stockType.getId(),
            stockType.getCodigo(),
            stockType.getNome(),
            item.getEmpresaId(),
            filialNome,
            label));
        }
      }
    }
    return options;
  }

  @Transactional
  public CatalogStockAdjustmentResponse createByType(
      CatalogConfigurationType type,
      CatalogStockAdjustmentUpsertRequest request) {
    Scope scope = resolveScope(type);
    CatalogStockAdjustmentType adjustmentType = CatalogStockAdjustmentType.from(request.tipo());
    StockRef origem = parseStockRef(
      request.estoqueOrigemAgrupadorId(),
      request.estoqueOrigemTipoId(),
      request.estoqueOrigemFilialId(),
      "catalog_stock_adjustment_origem_incompleto");
    StockRef destino = parseStockRef(
      request.estoqueDestinoAgrupadorId(),
      request.estoqueDestinoTipoId(),
      request.estoqueDestinoFilialId(),
      "catalog_stock_adjustment_destino_incompleto");

    validateByType(scope, adjustmentType, origem, destino);

    CatalogStockAdjustment entity = new CatalogStockAdjustment();
    entity.setTenantId(scope.tenantId());
    entity.setCatalogConfigurationId(scope.configId());
    entity.setNome(normalizeNome(request.nome()));
    entity.setTipo(adjustmentType.name());
    entity.setOrdem(normalizeOrdem(scope, request.ordem()));
    entity.setActive(request.active() == null || request.active());
    applyRefs(entity, origem, destino);

    for (int attempt = 0; attempt < 5; attempt++) {
      entity.setCodigo(nextCodigoByTenant(scope.tenantId()));
      try {
        CatalogStockAdjustment saved = repository.saveAndFlush(entity);
        auditService.log(
          scope.tenantId(),
          "CATALOG_STOCK_ADJUSTMENT_CREATED",
          "catalog_stock_adjustment",
          String.valueOf(saved.getId()),
          "type=" + type + ";codigo=" + saved.getCodigo() + ";adjustmentType=" + saved.getTipo() + ";active=" + saved.isActive());
        return toResponse(saved);
      } catch (DataIntegrityViolationException ex) {
        RuntimeException mapped = mapIntegrity(ex);
        if (isCodigoDuplicado(mapped)) {
          continue;
        }
        throw mapped;
      }
    }
    throw new IllegalStateException("catalog_stock_adjustment_codigo_auto_fail");
  }

  @Transactional
  public CatalogStockAdjustmentResponse updateByType(
      CatalogConfigurationType type,
      Long adjustmentId,
      CatalogStockAdjustmentUpsertRequest request) {
    if (adjustmentId == null || adjustmentId <= 0) {
      throw new IllegalArgumentException("catalog_stock_adjustment_id_invalid");
    }

    Scope scope = resolveScope(type);
    CatalogStockAdjustment entity = repository
      .findByIdAndTenantIdAndCatalogConfigurationId(adjustmentId, scope.tenantId(), scope.configId())
      .orElseThrow(() -> new EntityNotFoundException("catalog_stock_adjustment_not_found"));

    CatalogStockAdjustmentType adjustmentType = CatalogStockAdjustmentType.from(request.tipo());
    StockRef origem = parseStockRef(
      request.estoqueOrigemAgrupadorId(),
      request.estoqueOrigemTipoId(),
      request.estoqueOrigemFilialId(),
      "catalog_stock_adjustment_origem_incompleto");
    StockRef destino = parseStockRef(
      request.estoqueDestinoAgrupadorId(),
      request.estoqueDestinoTipoId(),
      request.estoqueDestinoFilialId(),
      "catalog_stock_adjustment_destino_incompleto");
    validateByType(scope, adjustmentType, origem, destino);

    entity.setNome(normalizeNome(request.nome()));
    entity.setTipo(adjustmentType.name());
    entity.setOrdem(normalizeOrdem(scope, request.ordem()));
    entity.setActive(request.active() == null ? entity.isActive() : request.active());
    applyRefs(entity, origem, destino);

    try {
      CatalogStockAdjustment saved = repository.saveAndFlush(entity);
      auditService.log(
        scope.tenantId(),
        "CATALOG_STOCK_ADJUSTMENT_UPDATED",
        "catalog_stock_adjustment",
        String.valueOf(saved.getId()),
        "type=" + type + ";codigo=" + saved.getCodigo() + ";adjustmentType=" + saved.getTipo() + ";active=" + saved.isActive());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  @Transactional
  public void deleteByType(CatalogConfigurationType type, Long adjustmentId) {
    if (adjustmentId == null || adjustmentId <= 0) {
      throw new IllegalArgumentException("catalog_stock_adjustment_id_invalid");
    }
    Scope scope = resolveScope(type);
    CatalogStockAdjustment entity = repository
      .findByIdAndTenantIdAndCatalogConfigurationId(adjustmentId, scope.tenantId(), scope.configId())
      .orElseThrow(() -> new EntityNotFoundException("catalog_stock_adjustment_not_found"));

    entity.setActive(false);
    repository.saveAndFlush(entity);
    auditService.log(
      scope.tenantId(),
      "CATALOG_STOCK_ADJUSTMENT_DEACTIVATED",
      "catalog_stock_adjustment",
      String.valueOf(entity.getId()),
      "type=" + type + ";codigo=" + entity.getCodigo());
  }

  private void validateByType(
      Scope scope,
      CatalogStockAdjustmentType type,
      StockRef origem,
      StockRef destino) {
    boolean hasOrigem = origem != null;
    boolean hasDestino = destino != null;

    if (type == CatalogStockAdjustmentType.ENTRADA) {
      if (hasOrigem) throw new IllegalArgumentException("catalog_stock_adjustment_origem_not_allowed");
      if (!hasDestino) throw new IllegalArgumentException("catalog_stock_adjustment_destino_required");
      validateStockRef(scope, destino);
      return;
    }

    if (type == CatalogStockAdjustmentType.SAIDA) {
      if (!hasOrigem) throw new IllegalArgumentException("catalog_stock_adjustment_origem_required");
      if (hasDestino) throw new IllegalArgumentException("catalog_stock_adjustment_destino_not_allowed");
      validateStockRef(scope, origem);
      return;
    }

    if (!hasOrigem || !hasDestino) {
      throw new IllegalArgumentException("catalog_stock_adjustment_transferencia_requer_origem_destino");
    }
    validateStockRef(scope, origem);
    validateStockRef(scope, destino);
    if (Objects.equals(origem.agrupadorId(), destino.agrupadorId())
      && Objects.equals(origem.estoqueTipoId(), destino.estoqueTipoId())
      && Objects.equals(origem.filialId(), destino.filialId())) {
      throw new IllegalArgumentException("catalog_stock_adjustment_same_origin_destination");
    }
  }

  private void validateStockRef(Scope scope, StockRef ref) {
    agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        ref.agrupadorId(),
        scope.tenantId(),
        ConfiguracaoScopeService.TYPE_CATALOGO,
        scope.configId())
      .orElseThrow(() -> new IllegalArgumentException("catalog_stock_adjustment_agrupador_not_found"));

    stockTypeRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrue(
        ref.estoqueTipoId(),
        scope.tenantId(),
        scope.configId(),
        ref.agrupadorId())
      .orElseThrow(() -> new IllegalArgumentException("catalog_stock_adjustment_stock_type_not_found"));

    agrupadorEmpresaItemRepository
      .findByTenantIdAndConfigTypeAndConfigIdAndAgrupadorIdAndEmpresaId(
        scope.tenantId(),
        ConfiguracaoScopeService.TYPE_CATALOGO,
        scope.configId(),
        ref.agrupadorId(),
        ref.filialId())
      .orElseThrow(() -> new IllegalArgumentException("catalog_stock_adjustment_filial_not_found"));
  }

  private StockRef parseStockRef(Long agrupadorId, Long estoqueTipoId, Long filialId, String incompleteCode) {
    boolean allNull = agrupadorId == null && estoqueTipoId == null && filialId == null;
    if (allNull) {
      return null;
    }
    if (agrupadorId == null || estoqueTipoId == null || filialId == null) {
      throw new IllegalArgumentException(incompleteCode);
    }
    if (agrupadorId <= 0 || estoqueTipoId <= 0 || filialId <= 0) {
      throw new IllegalArgumentException(incompleteCode);
    }
    return new StockRef(agrupadorId, estoqueTipoId, filialId);
  }

  private void applyRefs(CatalogStockAdjustment entity, StockRef origem, StockRef destino) {
    entity.setEstoqueOrigemAgrupadorId(origem == null ? null : origem.agrupadorId());
    entity.setEstoqueOrigemTipoId(origem == null ? null : origem.estoqueTipoId());
    entity.setEstoqueOrigemFilialId(origem == null ? null : origem.filialId());
    entity.setEstoqueDestinoAgrupadorId(destino == null ? null : destino.agrupadorId());
    entity.setEstoqueDestinoTipoId(destino == null ? null : destino.estoqueTipoId());
    entity.setEstoqueDestinoFilialId(destino == null ? null : destino.filialId());
  }

  private CatalogStockAdjustmentResponse toResponse(CatalogStockAdjustment entity) {
    return new CatalogStockAdjustmentResponse(
      entity.getId(),
      entity.getCodigo(),
      entity.getNome(),
      entity.getTipo(),
      entity.getOrdem(),
      entity.isActive(),
      entity.getVersion(),
      entity.getEstoqueOrigemAgrupadorId(),
      entity.getEstoqueOrigemTipoId(),
      entity.getEstoqueOrigemFilialId(),
      entity.getEstoqueDestinoAgrupadorId(),
      entity.getEstoqueDestinoTipoId(),
      entity.getEstoqueDestinoFilialId());
  }

  private Scope resolveScope(CatalogConfigurationType type) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    CatalogConfiguration config = catalogConfigurationService.getEntityOrCreate(type);
    return new Scope(tenantId, config.getId());
  }

  private Scope resolveScopeForRead(CatalogConfigurationType type) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return catalogConfigurationService.findEntity(type)
      .map(config -> new Scope(tenantId, config.getId()))
      .orElse(null);
  }

  private String normalizeNome(String value) {
    String raw = value == null ? "" : value.trim();
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("catalog_stock_adjustment_nome_required");
    }
    return raw.length() > 120 ? raw.substring(0, 120) : raw;
  }

  private int normalizeOrdem(Scope scope, Integer ordem) {
    if (ordem != null && ordem > 0) {
      return ordem;
    }
    Integer max = repository.maxOrdemByScope(scope.tenantId(), scope.configId());
    return (max == null ? 0 : max) + 1;
  }

  private RuntimeException mapIntegrity(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
    if (message.contains("ux_catalog_stock_adjustment_scope_codigo_active")
      || message.contains("ux_catalog_stock_adjustment_tenant_codigo")) {
      return new IllegalArgumentException("catalog_stock_adjustment_codigo_duplicado");
    }
    return ex;
  }

  private boolean isCodigoDuplicado(RuntimeException ex) {
    return ex instanceof IllegalArgumentException
      && "catalog_stock_adjustment_codigo_duplicado".equals(ex.getMessage());
  }

  private String nextCodigoByTenant(Long tenantId) {
    Long maxCodigo = repository.maxNumericCodigoByTenant(tenantId);
    long nextValue = (maxCodigo == null ? 0L : maxCodigo) + 1L;
    return String.valueOf(nextValue);
  }

  private record Scope(Long tenantId, Long configId) {}
  private record StockRef(Long agrupadorId, Long estoqueTipoId, Long filialId) {}
}
