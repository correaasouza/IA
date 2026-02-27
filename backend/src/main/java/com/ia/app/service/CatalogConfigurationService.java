package com.ia.app.service;

import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.dto.CatalogConfigurationResponse;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.tenant.TenantContext;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogConfigurationService {

  private final CatalogConfigurationRepository repository;
  private final AuditService auditService;

  public CatalogConfigurationService(
      CatalogConfigurationRepository repository,
      AuditService auditService) {
    this.repository = repository;
    this.auditService = auditService;
  }

  @Transactional
  public CatalogConfigurationResponse getOrCreate(CatalogConfigurationType type) {
    CatalogConfiguration config = getEntityOrCreate(type);
    return toResponse(config);
  }

  @Transactional
  public CatalogConfigurationResponse update(CatalogConfigurationType type, CatalogNumberingMode numberingMode) {
    if (numberingMode == null) {
      throw new IllegalArgumentException("catalog_configuration_numbering_required");
    }
    Long tenantId = requireTenant();
    CatalogConfiguration entity = getEntityOrCreate(type);
    entity.setNumberingMode(numberingMode);
    CatalogConfiguration saved = repository.save(entity);
    auditService.log(tenantId,
      "CATALOG_CONFIGURATION_UPDATED",
      "catalog_configuration",
      String.valueOf(saved.getId()),
      "type=" + saved.getType() + ";numberingMode=" + saved.getNumberingMode());
    return toResponse(saved);
  }

  private CatalogConfiguration createDefault(Long tenantId, CatalogConfigurationType type) {
    CatalogConfiguration entity = new CatalogConfiguration();
    entity.setTenantId(tenantId);
    entity.setType(type);
    entity.setNumberingMode(CatalogNumberingMode.AUTOMATICA);
    entity.setActive(true);
    try {
      CatalogConfiguration saved = repository.saveAndFlush(entity);
      auditService.log(tenantId,
        "CATALOG_CONFIGURATION_CREATED",
        "catalog_configuration",
        String.valueOf(saved.getId()),
        "type=" + saved.getType() + ";numberingMode=" + saved.getNumberingMode());
      return saved;
    } catch (DataIntegrityViolationException ex) {
      return repository.findByTenantIdAndType(tenantId, type)
        .orElseThrow(() -> ex);
    }
  }

  @Transactional
  CatalogConfiguration getEntityOrCreate(CatalogConfigurationType type) {
    Long tenantId = requireTenant();
    return repository.findByTenantIdAndType(tenantId, type)
      .orElseGet(() -> createDefault(tenantId, type));
  }

  @Transactional(readOnly = true)
  Optional<CatalogConfiguration> findEntity(CatalogConfigurationType type) {
    Long tenantId = requireTenant();
    return repository.findByTenantIdAndType(tenantId, type);
  }

  private CatalogConfigurationResponse toResponse(CatalogConfiguration entity) {
    return new CatalogConfigurationResponse(
      entity.getId(),
      entity.getType(),
      entity.getNumberingMode(),
      entity.isActive(),
      entity.getVersion(),
      entity.getCreatedAt(),
      entity.getUpdatedAt());
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
