package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.dto.CatalogStockTypeResponse;
import com.ia.app.dto.CatalogStockTypeUpsertRequest;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogStockTypeConfigurationService {

  private final CatalogConfigurationService catalogConfigurationService;
  private final CatalogStockTypeRepository stockTypeRepository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final CatalogStockTypeSyncService stockTypeSyncService;
  private final AuditService auditService;

  public CatalogStockTypeConfigurationService(
      CatalogConfigurationService catalogConfigurationService,
      CatalogStockTypeRepository stockTypeRepository,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      CatalogStockTypeSyncService stockTypeSyncService,
      AuditService auditService) {
    this.catalogConfigurationService = catalogConfigurationService;
    this.stockTypeRepository = stockTypeRepository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.stockTypeSyncService = stockTypeSyncService;
    this.auditService = auditService;
  }

  @Transactional
  public List<CatalogStockTypeResponse> listByGroup(CatalogConfigurationType type, Long agrupadorId) {
    Scope scope = resolveScope(type, agrupadorId);
    stockTypeSyncService.ensureDefaultForGroup(scope.tenantId(), scope.configId(), scope.agrupadorId());
    return stockTypeRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdOrderByOrdemAscNomeAsc(
        scope.tenantId(),
        scope.configId(),
        scope.agrupadorId())
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional
  public CatalogStockTypeResponse createByGroup(
      CatalogConfigurationType type,
      Long agrupadorId,
      CatalogStockTypeUpsertRequest request) {
    Scope scope = resolveScope(type, agrupadorId);
    String codigo = normalizeCodigo(request.codigo());
    String nome = normalizeNome(request.nome());
    int ordem = normalizeOrdem(scope, request.ordem());
    boolean active = request.active() == null || request.active();

    CatalogStockType entity = new CatalogStockType();
    entity.setTenantId(scope.tenantId());
    entity.setCatalogConfigurationId(scope.configId());
    entity.setAgrupadorEmpresaId(scope.agrupadorId());
    entity.setCodigo(codigo);
    entity.setNome(nome);
    entity.setOrdem(ordem);
    entity.setActive(active);

    try {
      CatalogStockType saved = stockTypeRepository.saveAndFlush(entity);
      auditService.log(
        scope.tenantId(),
        "CATALOG_STOCK_TYPE_CREATED",
        "catalog_stock_type",
        String.valueOf(saved.getId()),
        "type=" + type + ";agrupadorId=" + scope.agrupadorId() + ";codigo=" + saved.getCodigo() + ";active=" + saved.isActive());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  @Transactional
  public CatalogStockTypeResponse updateByGroup(
      CatalogConfigurationType type,
      Long agrupadorId,
      Long stockTypeId,
      CatalogStockTypeUpsertRequest request) {
    if (stockTypeId == null || stockTypeId <= 0) {
      throw new IllegalArgumentException("catalog_stock_type_id_invalid");
    }

    Scope scope = resolveScope(type, agrupadorId);
    CatalogStockType entity = stockTypeRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
        stockTypeId,
        scope.tenantId(),
        scope.configId(),
        scope.agrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("catalog_stock_type_not_found"));

    boolean targetActive = request.active() == null ? entity.isActive() : request.active();
    if (!targetActive && entity.isActive()) {
      long activeCount = stockTypeRepository.countByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrue(
        scope.tenantId(),
        scope.configId(),
        scope.agrupadorId());
      if (activeCount <= 1) {
        throw new IllegalArgumentException("catalog_stock_type_last_active");
      }
    }

    entity.setCodigo(normalizeCodigo(request.codigo()));
    entity.setNome(normalizeNome(request.nome()));
    entity.setOrdem(normalizeOrdem(scope, request.ordem()));
    entity.setActive(targetActive);

    try {
      CatalogStockType saved = stockTypeRepository.saveAndFlush(entity);
      auditService.log(
        scope.tenantId(),
        "CATALOG_STOCK_TYPE_UPDATED",
        "catalog_stock_type",
        String.valueOf(saved.getId()),
        "type=" + type + ";agrupadorId=" + scope.agrupadorId() + ";codigo=" + saved.getCodigo() + ";active=" + saved.isActive());
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrity(ex);
    }
  }

  private CatalogStockTypeResponse toResponse(CatalogStockType entity) {
    return new CatalogStockTypeResponse(
      entity.getId(),
      entity.getCodigo(),
      entity.getNome(),
      entity.getOrdem(),
      entity.isActive(),
      entity.getVersion());
  }

  private Scope resolveScope(CatalogConfigurationType type, Long agrupadorId) {
    if (agrupadorId == null || agrupadorId <= 0) {
      throw new IllegalArgumentException("agrupador_id_invalid");
    }
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }

    CatalogConfiguration catalogConfig = catalogConfigurationService.getEntityOrCreate(type);
    AgrupadorEmpresa agrupador = agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        agrupadorId,
        tenantId,
        ConfiguracaoScopeService.TYPE_CATALOGO,
        catalogConfig.getId())
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    return new Scope(tenantId, catalogConfig.getId(), agrupador.getId());
  }

  private String normalizeCodigo(String value) {
    String raw = value == null ? "" : value.trim();
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("catalog_stock_type_codigo_required");
    }
    raw = raw
      .replace(' ', '_')
      .replace('-', '_')
      .toUpperCase(Locale.ROOT);
    return raw.length() > 40 ? raw.substring(0, 40) : raw;
  }

  private String normalizeNome(String value) {
    String raw = value == null ? "" : value.trim();
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("catalog_stock_type_nome_required");
    }
    return raw.length() > 120 ? raw.substring(0, 120) : raw;
  }

  private int normalizeOrdem(Scope scope, Integer ordem) {
    if (ordem != null && ordem > 0) {
      return ordem;
    }
    Integer max = stockTypeRepository.maxOrdemByScope(scope.tenantId(), scope.configId(), scope.agrupadorId());
    return (max == null ? 0 : max) + 1;
  }

  private RuntimeException mapIntegrity(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    if (message.toLowerCase().contains("ux_catalog_stock_type_scope_codigo_active")) {
      return new IllegalArgumentException("catalog_stock_type_codigo_duplicado");
    }
    return ex;
  }

  private record Scope(Long tenantId, Long configId, Long agrupadorId) {}
}
