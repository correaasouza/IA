package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.TipoEntidadeRequest;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  TipoEntidadeService.class,
  TipoEntidadeSeedService.class,
  AuditService.class
})
class TipoEntidadeServiceTest {

  private static final long TENANT_ID = 12L;

  @Autowired
  private TipoEntidadeService service;

  @Autowired
  private TipoEntidadeSeedService seedService;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorEmpresaRepository;

  @Autowired
  private TipoEntidadeConfigPorAgrupadorRepository configPorAgrupadorRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldSeedDefaultsIdempotently() {
    seedService.seedDefaults(TENANT_ID);
    seedService.seedDefaults(TENANT_ID);

    assertThat(tipoEntidadeRepository.findByTenantIdAndCodigoSeed(TENANT_ID, "CLIENTE")).isPresent();
    assertThat(tipoEntidadeRepository.findByTenantIdAndCodigoSeed(TENANT_ID, "FORNECEDOR")).isPresent();
    assertThat(tipoEntidadeRepository.findByTenantIdAndCodigoSeed(TENANT_ID, "EQUIPE")).isPresent();
  }

  @Test
  void shouldBlockDeleteForSeededType() {
    seedService.seedDefaults(TENANT_ID);
    Long clienteId = tipoEntidadeRepository.findByTenantIdAndCodigoSeed(TENANT_ID, "CLIENTE")
      .orElseThrow().getId();

    TenantContext.setTenantId(TENANT_ID);
    assertThatThrownBy(() -> service.delete(clienteId))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("tipo_entidade_padrao_nao_excluivel");
  }

  @Test
  void shouldSoftDeleteCustomTypeAndCleanupAgrupadores() {
    TenantContext.setTenantId(TENANT_ID);
    TipoEntidade custom = service.create(new TipoEntidadeRequest("PARCEIRO", true));

    AgrupadorEmpresa agrupador = new AgrupadorEmpresa();
    agrupador.setTenantId(TENANT_ID);
    agrupador.setConfigType(ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE);
    agrupador.setConfigId(custom.getId());
    agrupador.setNome("Grupo A");
    agrupador.setAtivo(true);
    AgrupadorEmpresa savedAgrupador = agrupadorEmpresaRepository.save(agrupador);

    com.ia.app.domain.TipoEntidadeConfigPorAgrupador cfg = new com.ia.app.domain.TipoEntidadeConfigPorAgrupador();
    cfg.setTenantId(TENANT_ID);
    cfg.setTipoEntidadeId(custom.getId());
    cfg.setAgrupadorId(savedAgrupador.getId());
    cfg.setObrigarUmTelefone(false);
    cfg.setAtivo(true);
    configPorAgrupadorRepository.save(cfg);

    service.delete(custom.getId());

    TipoEntidade persisted = tipoEntidadeRepository.findByIdAndTenantId(custom.getId(), TENANT_ID).orElseThrow();
    assertThat(persisted.isAtivo()).isFalse();
    assertThat(agrupadorEmpresaRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
        TENANT_ID, ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE, custom.getId()))
      .isEmpty();
  }
}
