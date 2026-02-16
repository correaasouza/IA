package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationByGroup;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.CatalogItemContextResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemContextService {

  public record CatalogItemScope(
    Long tenantId,
    Long empresaId,
    CatalogConfigurationType type,
    Long catalogConfigurationId,
    Long agrupadorId,
    String agrupadorNome,
    CatalogNumberingMode numberingMode
  ) {}

  private final CatalogConfigurationService catalogConfigurationService;
  private final EmpresaRepository empresaRepository;
  private final AgrupadorEmpresaItemRepository agrupadorItemRepository;
  private final AgrupadorEmpresaRepository agrupadorRepository;
  private final CatalogConfigurationByGroupRepository configByGroupRepository;
  private final CatalogConfigurationGroupSyncService groupSyncService;

  public CatalogItemContextService(
      CatalogConfigurationService catalogConfigurationService,
      EmpresaRepository empresaRepository,
      AgrupadorEmpresaItemRepository agrupadorItemRepository,
      AgrupadorEmpresaRepository agrupadorRepository,
      CatalogConfigurationByGroupRepository configByGroupRepository,
      CatalogConfigurationGroupSyncService groupSyncService) {
    this.catalogConfigurationService = catalogConfigurationService;
    this.empresaRepository = empresaRepository;
    this.agrupadorItemRepository = agrupadorItemRepository;
    this.agrupadorRepository = agrupadorRepository;
    this.configByGroupRepository = configByGroupRepository;
    this.groupSyncService = groupSyncService;
  }

  @Transactional(readOnly = true)
  public CatalogItemContextResponse contexto(CatalogConfigurationType type) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();

    CatalogConfiguration catalogConfig = catalogConfigurationService.getEntityOrCreate(type);
    Empresa empresa = empresaRepository.findByIdAndTenantId(empresaId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_context_not_found"));

    var itemOpt = agrupadorItemRepository.findByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(
      tenantId, ConfiguracaoScopeService.TYPE_CATALOGO, catalogConfig.getId(), empresaId);

    if (itemOpt.isEmpty()) {
      return new CatalogItemContextResponse(
        empresaId,
        empresa.getRazaoSocial(),
        type,
        catalogConfig.getId(),
        null,
        null,
        CatalogNumberingMode.AUTOMATICA,
        false,
        "EMPRESA_SEM_GRUPO_NO_CATALOGO",
        "A empresa selecionada nao esta vinculada a nenhum Grupo de Empresas para este catalogo.");
    }

    AgrupadorEmpresaItem item = itemOpt.get();
    AgrupadorEmpresa agrupador = agrupadorRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        item.getAgrupador().getId(),
        tenantId,
        ConfiguracaoScopeService.TYPE_CATALOGO,
        catalogConfig.getId())
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    groupSyncService.onAgrupadorCreated(tenantId, catalogConfig.getId(), agrupador.getId());
    CatalogNumberingMode numberingMode = configByGroupRepository
      .findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
        tenantId, catalogConfig.getId(), agrupador.getId())
      .map(CatalogConfigurationByGroup::getNumberingMode)
      .orElse(CatalogNumberingMode.AUTOMATICA);

    return new CatalogItemContextResponse(
      empresaId,
      empresa.getRazaoSocial(),
      type,
      catalogConfig.getId(),
      agrupador.getId(),
      agrupador.getNome(),
      numberingMode,
      true,
      null,
      null);
  }

  @Transactional(readOnly = true)
  public CatalogItemScope resolveObrigatorio(CatalogConfigurationType type) {
    CatalogItemContextResponse contexto = contexto(type);
    if (!contexto.vinculado()) {
      throw new IllegalArgumentException("catalog_context_sem_grupo");
    }
    return new CatalogItemScope(
      requireTenant(),
      contexto.empresaId(),
      type,
      contexto.catalogConfigurationId(),
      contexto.agrupadorId(),
      contexto.agrupadorNome(),
      contexto.numberingMode());
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Long requireEmpresaContext() {
    Long empresaId = EmpresaContext.getEmpresaId();
    if (empresaId == null) {
      throw new IllegalArgumentException("catalog_context_required");
    }
    return empresaId;
  }
}
