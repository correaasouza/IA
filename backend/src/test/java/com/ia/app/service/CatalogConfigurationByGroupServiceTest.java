package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.dto.CatalogConfigurationByGroupResponse;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  CatalogConfigurationService.class,
  CatalogConfigurationByGroupService.class,
  CatalogConfigurationGroupSyncService.class,
  CatalogStockTypeSyncService.class,
  AuditService.class
})
class CatalogConfigurationByGroupServiceTest {

  @Autowired
  private CatalogConfigurationService catalogConfigurationService;

  @Autowired
  private CatalogConfigurationByGroupService byGroupService;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @Autowired
  private CatalogConfigurationByGroupRepository byGroupRepository;

  @AfterEach
  void cleanTenantContext() {
    TenantContext.clear();
  }

  @Test
  void shouldListAndLazyCreateGroupRowsForCatalog() {
    TenantContext.setTenantId(71L);
    Long catalogConfigId = catalogConfigurationService.getOrCreate(CatalogConfigurationType.PRODUCTS).id();

    AgrupadorEmpresa g1 = createGroup(71L, catalogConfigId, "Grupo A");
    createGroup(71L, catalogConfigId, "Grupo B");

    List<CatalogConfigurationByGroupResponse> rows = byGroupService.list(CatalogConfigurationType.PRODUCTS);

    assertThat(rows).hasSize(2);
    assertThat(rows).anyMatch(r -> r.agrupadorId().equals(g1.getId()) && r.numberingMode() == CatalogNumberingMode.AUTOMATICA);
    assertThat(byGroupRepository.findAllByTenantIdAndCatalogConfigurationIdAndActiveTrue(71L, catalogConfigId)).hasSize(2);
  }

  @Test
  void shouldUpdateNumberingModeByGroup() {
    TenantContext.setTenantId(81L);
    Long catalogConfigId = catalogConfigurationService.getOrCreate(CatalogConfigurationType.SERVICES).id();
    AgrupadorEmpresa group = createGroup(81L, catalogConfigId, "Grupo Servicos");

    CatalogConfigurationByGroupResponse updated = byGroupService.update(
      CatalogConfigurationType.SERVICES,
      group.getId(),
      CatalogNumberingMode.MANUAL);

    assertThat(updated.agrupadorId()).isEqualTo(group.getId());
    assertThat(updated.numberingMode()).isEqualTo(CatalogNumberingMode.MANUAL);
    assertThat(byGroupRepository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
      81L, catalogConfigId, group.getId()))
      .get()
      .extracting(item -> item.getNumberingMode())
      .isEqualTo(CatalogNumberingMode.MANUAL);
  }

  private AgrupadorEmpresa createGroup(Long tenantId, Long catalogConfigId, String nome) {
    AgrupadorEmpresa group = new AgrupadorEmpresa();
    group.setTenantId(tenantId);
    group.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    group.setConfigId(catalogConfigId);
    group.setNome(nome);
    group.setAtivo(true);
    return agrupadorRepository.save(group);
  }
}
