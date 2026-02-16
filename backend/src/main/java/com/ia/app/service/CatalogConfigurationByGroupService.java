package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationByGroup;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.dto.CatalogConfigurationByGroupResponse;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogConfigurationByGroupService {

  private final CatalogConfigurationService catalogConfigurationService;
  private final CatalogConfigurationByGroupRepository repository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final CatalogConfigurationGroupSyncService syncService;
  private final AuditService auditService;

  public CatalogConfigurationByGroupService(
      CatalogConfigurationService catalogConfigurationService,
      CatalogConfigurationByGroupRepository repository,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      CatalogConfigurationGroupSyncService syncService,
      AuditService auditService) {
    this.catalogConfigurationService = catalogConfigurationService;
    this.repository = repository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.syncService = syncService;
    this.auditService = auditService;
  }

  @Transactional
  public List<CatalogConfigurationByGroupResponse> list(CatalogConfigurationType type) {
    Long tenantId = requireTenant();
    CatalogConfiguration catalogConfig = catalogConfigurationService.getEntityOrCreate(type);
    List<AgrupadorEmpresa> agrupadores = agrupadorEmpresaRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
        tenantId, ConfiguracaoScopeService.TYPE_CATALOGO, catalogConfig.getId());

    for (AgrupadorEmpresa agrupador : agrupadores) {
      syncService.onAgrupadorCreated(tenantId, catalogConfig.getId(), agrupador.getId());
    }

    Map<Long, CatalogConfigurationByGroup> rowByAgrupador = new HashMap<>();
    for (CatalogConfigurationByGroup row : repository.findAllByTenantIdAndCatalogConfigurationIdAndActiveTrue(
      tenantId, catalogConfig.getId())) {
      rowByAgrupador.put(row.getAgrupadorId(), row);
    }

    return agrupadores.stream()
      .map(agrupador -> {
        CatalogConfigurationByGroup row = rowByAgrupador.get(agrupador.getId());
        return new CatalogConfigurationByGroupResponse(
          agrupador.getId(),
          agrupador.getNome(),
          row == null ? CatalogNumberingMode.AUTOMATICA : row.getNumberingMode(),
          row != null && row.isActive());
      })
      .sorted(Comparator.comparing(CatalogConfigurationByGroupResponse::agrupadorNome, String.CASE_INSENSITIVE_ORDER))
      .toList();
  }

  @Transactional
  public CatalogConfigurationByGroupResponse update(CatalogConfigurationType type, Long agrupadorId, CatalogNumberingMode numberingMode) {
    if (agrupadorId == null || agrupadorId <= 0) {
      throw new IllegalArgumentException("agrupador_id_invalid");
    }
    if (numberingMode == null) {
      throw new IllegalArgumentException("catalog_configuration_numbering_required");
    }

    Long tenantId = requireTenant();
    CatalogConfiguration catalogConfig = catalogConfigurationService.getEntityOrCreate(type);
    AgrupadorEmpresa agrupador = agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        agrupadorId, tenantId, ConfiguracaoScopeService.TYPE_CATALOGO, catalogConfig.getId())
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    try {
      CatalogConfigurationByGroup row = repository
        .findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(tenantId, catalogConfig.getId(), agrupadorId)
        .orElseGet(() -> {
          CatalogConfigurationByGroup novo = new CatalogConfigurationByGroup();
          novo.setTenantId(tenantId);
          novo.setCatalogConfigurationId(catalogConfig.getId());
          novo.setAgrupadorId(agrupadorId);
          novo.setActive(true);
          novo.setNumberingMode(CatalogNumberingMode.AUTOMATICA);
          return novo;
        });
      row.setNumberingMode(numberingMode);
      CatalogConfigurationByGroup saved = repository.save(row);
      auditService.log(tenantId,
        "CATALOG_CFG_GROUP_UPDATED",
        "catalog_configuration_by_group",
        String.valueOf(saved.getId()),
        "type=" + type + ";catalogConfigurationId=" + catalogConfig.getId() + ";agrupadorId=" + agrupadorId + ";numberingMode=" + numberingMode);

      return new CatalogConfigurationByGroupResponse(
        agrupador.getId(),
        agrupador.getNome(),
        saved.getNumberingMode(),
        saved.isActive());
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
  }

  private RuntimeException mapIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    if (message.toLowerCase().contains("ux_catalog_cfg_group_active")) {
      return new IllegalArgumentException("catalog_configuration_group_duplicated");
    }
    return ex;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
