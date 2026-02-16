package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockBalance;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
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
  AuditService.class,
  CatalogStockTypeSyncService.class,
  CatalogMovementEngine.class,
  CatalogGroupTransferService.class
})
class CatalogGroupTransferServiceTest {

  @Autowired
  private CatalogConfigurationRepository configurationRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private CatalogStockTypeSyncService stockTypeSyncService;

  @Autowired
  private CatalogStockTypeRepository stockTypeRepository;

  @Autowired
  private CatalogStockBalanceRepository stockBalanceRepository;

  @Autowired
  private CatalogMovementRepository movementRepository;

  @Autowired
  private CatalogMovementLineRepository movementLineRepository;

  @Autowired
  private CatalogGroupTransferService transferService;

  @Test
  void shouldTransferStockToNewGroupAndRegisterMovement() {
    Long tenantId = 501L;
    CatalogConfiguration config = createCatalogConfig(tenantId, CatalogConfigurationType.PRODUCTS);
    AgrupadorEmpresa from = createCatalogGroup(tenantId, config.getId(), "Grupo Origem");
    AgrupadorEmpresa to = createCatalogGroup(tenantId, config.getId(), "Grupo Destino");
    Empresa filial = createEmpresa(tenantId, "50100000000001");

    CatalogStockType fromStockType = stockTypeSyncService.ensureByCode(
      tenantId,
      config.getId(),
      from.getId(),
      "GERAL",
      "Estoque Geral",
      1);
    stockTypeSyncService.ensureDefaultForGroup(tenantId, config.getId(), to.getId());

    Long catalogoId = 9001L;
    CatalogStockBalance balance = new CatalogStockBalance();
    balance.setTenantId(tenantId);
    balance.setCatalogType(CatalogConfigurationType.PRODUCTS);
    balance.setCatalogConfigurationId(config.getId());
    balance.setCatalogoId(catalogoId);
    balance.setAgrupadorEmpresaId(from.getId());
    balance.setEstoqueTipoId(fromStockType.getId());
    balance.setFilialId(filial.getId());
    balance.setQuantidadeAtual(new BigDecimal("12.000000"));
    balance.setPrecoAtual(new BigDecimal("150.000000"));
    stockBalanceRepository.saveAndFlush(balance);

    transferService.transferOnEmpresaGroupChange(
      tenantId,
      config.getId(),
      filial.getId(),
      from.getId(),
      to.getId(),
      "transfer-501");

    List<CatalogStockBalance> sourceRows = stockBalanceRepository.listByFilters(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      catalogoId,
      from.getId(),
      null,
      filial.getId());
    assertThat(sourceRows).hasSize(1);
    assertThat(sourceRows.get(0).getQuantidadeAtual()).isEqualByComparingTo("0.000000");
    assertThat(sourceRows.get(0).getPrecoAtual()).isEqualByComparingTo("0.000000");

    List<CatalogStockType> targetStockTypes = stockTypeRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
        tenantId,
        config.getId(),
        to.getId());
    assertThat(targetStockTypes).isNotEmpty();
    Long targetStockTypeId = targetStockTypes.get(0).getId();

    List<CatalogStockBalance> targetRows = stockBalanceRepository.listByFilters(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      catalogoId,
      to.getId(),
      targetStockTypeId,
      filial.getId());
    assertThat(targetRows).hasSize(1);
    assertThat(targetRows.get(0).getQuantidadeAtual()).isEqualByComparingTo("12.000000");
    assertThat(targetRows.get(0).getPrecoAtual()).isEqualByComparingTo("150.000000");

    assertThat(movementRepository.count()).isEqualTo(1);
    assertThat(movementLineRepository.count()).isEqualTo(4);
    assertThat(movementRepository.findAll().get(0).getOrigemMovimentacaoTipo())
      .isEqualTo(CatalogMovementOriginType.MUDANCA_GRUPO);
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
