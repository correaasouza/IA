package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
  "movimento.config.enabled=true",
  "movimento.config.strict-enabled=true"
})
@Import({
  AuditingConfig.class,
  CatalogStockTypeSyncService.class,
  CatalogMovementEngine.class
})
class CatalogMovementEngineStrictModeTest {

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
  private MovimentoConfigRepository movimentoConfigRepository;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @Test
  void shouldBlockStockMovementWhenStrictModeEnabledAndCoverageMissing() {
    Long tenantId = 701L;
    CatalogConfiguration config = createCatalogConfig(tenantId, CatalogConfigurationType.PRODUCTS);
    AgrupadorEmpresa agrupador = createCatalogGroup(tenantId, config.getId(), "Grupo Strict");
    Empresa filial = createEmpresa(tenantId, "70100000000001");
    CatalogStockType stockType = stockTypeSyncService.ensureDefaultForGroup(tenantId, config.getId(), agrupador.getId());

    CatalogMovementEngine.Command command = new CatalogMovementEngine.Command(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      2001L,
      config.getId(),
      agrupador.getId(),
      CatalogMovementOriginType.SYSTEM,
      "STRICT",
      null,
      null,
      "ITEM:2001",
      null,
      null,
      null,
      "Teste modo estrito",
      "idem-engine-strict-701",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      List.of(new CatalogMovementEngine.Impact(
        agrupador.getId(),
        CatalogMovementMetricType.QUANTIDADE,
        stockType.getId(),
        filial.getId(),
        new BigDecimal("5.000000"))));

    assertThatThrownBy(() -> engine.apply(command))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_nao_encontrada");
  }

  @Test
  void shouldAllowStockMovementWhenStrictModeEnabledAndCoverageExists() {
    Long tenantId = 702L;
    CatalogConfiguration config = createCatalogConfig(tenantId, CatalogConfigurationType.PRODUCTS);
    AgrupadorEmpresa agrupador = createCatalogGroup(tenantId, config.getId(), "Grupo Strict OK");
    Empresa filial = createEmpresa(tenantId, "70200000000001");
    CatalogStockType stockType = stockTypeSyncService.ensureDefaultForGroup(tenantId, config.getId(), agrupador.getId());
    createMovimentoConfigEstoqueGlobal(tenantId, filial.getId());

    CatalogMovementEngine.Command command = new CatalogMovementEngine.Command(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      2002L,
      config.getId(),
      agrupador.getId(),
      CatalogMovementOriginType.SYSTEM,
      "STRICT",
      null,
      null,
      "ITEM:2002",
      null,
      null,
      null,
      "Teste modo estrito com cobertura",
      "idem-engine-strict-702",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      List.of(new CatalogMovementEngine.Impact(
        agrupador.getId(),
        CatalogMovementMetricType.QUANTIDADE,
        stockType.getId(),
        filial.getId(),
        new BigDecimal("7.000000"))));

    CatalogMovementEngine.Result result = engine.apply(command);

    assertThat(result.reused()).isFalse();
    assertThat(movementRepository.count()).isEqualTo(1);
    assertThat(balanceRepository.listByFilters(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      2002L,
      agrupador.getId(),
      null,
      filial.getId())).hasSize(1);
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

  private void createMovimentoConfigEstoqueGlobal(Long tenantId, Long empresaId) {
    TipoEntidade tipoEntidade = new TipoEntidade();
    tipoEntidade.setTenantId(tenantId);
    tipoEntidade.setNome("Cliente Strict " + empresaId);
    tipoEntidade.setTipoPadrao(true);
    tipoEntidade.setAtivo(true);
    tipoEntidade = tipoEntidadeRepository.saveAndFlush(tipoEntidade);

    MovimentoConfig config = new MovimentoConfig();
    config.setTenantId(tenantId);
    config.setTipoMovimento(MovimentoTipo.MOVIMENTO_ESTOQUE);
    config.setNome("Config Estoque Strict " + empresaId);
    config.setDescricao("Configuracao para teste de modo estrito");
    config.setPrioridade(100);
    config.setContextoKey(null);
    config.setTipoEntidadePadraoId(tipoEntidade.getId());
    config.setAtivo(true);
    config.replaceEmpresas(List.of(empresaId));
    config.replaceTiposEntidadePermitidos(List.of(tipoEntidade.getId()));
    movimentoConfigRepository.saveAndFlush(config);
  }
}
