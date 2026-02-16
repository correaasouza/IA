package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogStockTypeResponse;
import com.ia.app.dto.CatalogStockTypeUpsertRequest;
import com.ia.app.repository.AgrupadorEmpresaRepository;
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
  CatalogStockTypeSyncService.class,
  CatalogStockTypeConfigurationService.class,
  AuditService.class
})
class CatalogStockTypeConfigurationServiceTest {

  @Autowired
  private CatalogConfigurationService catalogConfigurationService;

  @Autowired
  private CatalogStockTypeConfigurationService stockTypeConfigurationService;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @AfterEach
  void cleanTenantContext() {
    TenantContext.clear();
  }

  @Test
  void shouldListWithDefaultStockType() {
    TenantContext.setTenantId(91L);
    Long configId = catalogConfigurationService.getOrCreate(CatalogConfigurationType.PRODUCTS).id();
    AgrupadorEmpresa group = createGroup(91L, configId, "Grupo Estoque");

    List<CatalogStockTypeResponse> rows = stockTypeConfigurationService.listByGroup(CatalogConfigurationType.PRODUCTS, group.getId());

    assertThat(rows).isNotEmpty();
    assertThat(rows).anyMatch(item -> "GERAL".equals(item.codigo()) && item.active());
  }

  @Test
  void shouldCreateAndUpdateStockType() {
    TenantContext.setTenantId(92L);
    Long configId = catalogConfigurationService.getOrCreate(CatalogConfigurationType.SERVICES).id();
    AgrupadorEmpresa group = createGroup(92L, configId, "Grupo Servicos");
    stockTypeConfigurationService.listByGroup(CatalogConfigurationType.SERVICES, group.getId());

    CatalogStockTypeResponse created = stockTypeConfigurationService.createByGroup(
      CatalogConfigurationType.SERVICES,
      group.getId(),
      new CatalogStockTypeUpsertRequest("A", "Estoque A", 2, true));

    assertThat(created.codigo()).isEqualTo("A");
    assertThat(created.nome()).isEqualTo("Estoque A");
    assertThat(created.active()).isTrue();

    CatalogStockTypeResponse updated = stockTypeConfigurationService.updateByGroup(
      CatalogConfigurationType.SERVICES,
      group.getId(),
      created.id(),
      new CatalogStockTypeUpsertRequest("A", "Estoque A Ajustado", 3, false));

    assertThat(updated.nome()).isEqualTo("Estoque A Ajustado");
    assertThat(updated.ordem()).isEqualTo(3);
    assertThat(updated.active()).isFalse();
  }

  @Test
  void shouldPreventDisableLastActiveStockType() {
    TenantContext.setTenantId(93L);
    Long configId = catalogConfigurationService.getOrCreate(CatalogConfigurationType.PRODUCTS).id();
    AgrupadorEmpresa group = createGroup(93L, configId, "Grupo Unico");

    CatalogStockTypeResponse defaultRow = stockTypeConfigurationService
      .listByGroup(CatalogConfigurationType.PRODUCTS, group.getId())
      .stream()
      .filter(CatalogStockTypeResponse::active)
      .findFirst()
      .orElseThrow();

    assertThatThrownBy(() -> stockTypeConfigurationService.updateByGroup(
      CatalogConfigurationType.PRODUCTS,
      group.getId(),
      defaultRow.id(),
      new CatalogStockTypeUpsertRequest(defaultRow.codigo(), defaultRow.nome(), defaultRow.ordem(), false)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_stock_type_last_active");
  }

  private AgrupadorEmpresa createGroup(Long tenantId, Long configId, String nome) {
    AgrupadorEmpresa group = new AgrupadorEmpresa();
    group.setTenantId(tenantId);
    group.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    group.setConfigId(configId);
    group.setNome(nome);
    group.setAtivo(true);
    return agrupadorRepository.save(group);
  }
}
