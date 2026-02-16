package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.EmpresaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  CatalogStockTypeSyncService.class,
  CatalogMovementEngine.class
})
class CatalogMovementEngineTest {

  @Autowired
  private CatalogConfigurationRepository configurationRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private CatalogStockTypeSyncService stockTypeSyncService;

  @Autowired
  private CatalogMovementEngine engine;

  @Autowired
  private CatalogStockBalanceRepository balanceRepository;

  @Autowired
  private CatalogMovementRepository movementRepository;

  @Autowired
  private CatalogMovementLineRepository lineRepository;

  @Test
  void shouldBeIdempotentForSameMovementCommand() {
    Long tenantId = 401L;
    CatalogConfiguration config = createCatalogConfig(tenantId, CatalogConfigurationType.PRODUCTS);
    AgrupadorEmpresa agrupador = createCatalogGroup(tenantId, config.getId(), "Grupo Teste");
    Empresa filial = createEmpresa(tenantId, "40100000000001");
    CatalogStockType stockType = stockTypeSyncService.ensureDefaultForGroup(tenantId, config.getId(), agrupador.getId());

    CatalogMovementEngine.Command command = new CatalogMovementEngine.Command(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      1001L,
      config.getId(),
      agrupador.getId(),
      CatalogMovementOriginType.SYSTEM,
      "SEED",
      "ITEM:1001",
      "Carga inicial",
      "idem-engine-401",
      null,
      List.of(new CatalogMovementEngine.Impact(
        agrupador.getId(),
        CatalogMovementMetricType.QUANTIDADE,
        stockType.getId(),
        filial.getId(),
        new BigDecimal("5.000000"))));

    CatalogMovementEngine.Result first = engine.apply(command);
    CatalogMovementEngine.Result second = engine.apply(command);

    assertThat(first.reused()).isFalse();
    assertThat(second.reused()).isTrue();
    assertThat(second.movementId()).isEqualTo(first.movementId());

    var balances = balanceRepository.listByFilters(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      1001L,
      agrupador.getId(),
      null,
      filial.getId());

    assertThat(balances).hasSize(1);
    assertThat(balances.get(0).getQuantidadeAtual()).isEqualByComparingTo("5.000000");
    assertThat(movementRepository.count()).isEqualTo(1);
    assertThat(lineRepository.count()).isEqualTo(1);
  }

  private CatalogConfiguration createCatalogConfig(Long tenantId, CatalogConfigurationType type) {
    CatalogConfiguration config = new CatalogConfiguration();
    config.setTenantId(tenantId);
    config.setType(type);
    config.setActive(true);
    return configurationRepository.save(config);
  }

  private AgrupadorEmpresa createCatalogGroup(Long tenantId, Long configId, String nome) {
    AgrupadorEmpresa group = new AgrupadorEmpresa();
    group.setTenantId(tenantId);
    group.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    group.setConfigId(configId);
    group.setNome(nome);
    group.setAtivo(true);
    return agrupadorRepository.save(group);
  }

  private Empresa createEmpresa(Long tenantId, String cnpj) {
    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(Empresa.TIPO_FILIAL);
    empresa.setRazaoSocial("Filial " + cnpj);
    empresa.setNomeFantasia("Filial " + cnpj);
    empresa.setCnpj(cnpj);
    empresa.setAtivo(true);
    return empresaRepository.save(empresa);
  }
}
