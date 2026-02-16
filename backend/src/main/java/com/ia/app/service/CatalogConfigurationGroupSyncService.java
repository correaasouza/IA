package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationByGroup;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogConfigurationGroupSyncService {

  private final CatalogConfigurationByGroupRepository repository;
  private final CatalogStockTypeSyncService stockTypeSyncService;

  public CatalogConfigurationGroupSyncService(
      CatalogConfigurationByGroupRepository repository,
      CatalogStockTypeSyncService stockTypeSyncService) {
    this.repository = repository;
    this.stockTypeSyncService = stockTypeSyncService;
  }

  @Transactional
  public void onAgrupadorCreated(Long tenantId, Long catalogConfigurationId, Long agrupadorId) {
    if (tenantId == null || catalogConfigurationId == null || agrupadorId == null) {
      return;
    }
    if (repository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
      tenantId, catalogConfigurationId, agrupadorId).isPresent()) {
      stockTypeSyncService.ensureDefaultForGroup(tenantId, catalogConfigurationId, agrupadorId);
      return;
    }
    CatalogConfigurationByGroup config = new CatalogConfigurationByGroup();
    config.setTenantId(tenantId);
    config.setCatalogConfigurationId(catalogConfigurationId);
    config.setAgrupadorId(agrupadorId);
    config.setNumberingMode(CatalogNumberingMode.AUTOMATICA);
    config.setActive(true);
    try {
      repository.save(config);
      stockTypeSyncService.ensureDefaultForGroup(tenantId, catalogConfigurationId, agrupadorId);
    } catch (DataIntegrityViolationException ex) {
      if (repository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
        tenantId, catalogConfigurationId, agrupadorId).isEmpty()) {
        throw ex;
      }
      stockTypeSyncService.ensureDefaultForGroup(tenantId, catalogConfigurationId, agrupadorId);
    }
  }

  @Transactional
  public void onAgrupadorRemoved(Long tenantId, Long catalogConfigurationId, Long agrupadorId) {
    repository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
      tenantId, catalogConfigurationId, agrupadorId).ifPresent(config -> {
      config.setActive(false);
      repository.save(config);
    });
    stockTypeSyncService.onGroupRemoved(tenantId, catalogConfigurationId, agrupadorId);
  }
}
