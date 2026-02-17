package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({
  AuditingConfig.class,
  MovimentoConfigFeatureToggle.class,
  MovimentoConfigSeedService.class
})
@TestPropertySource(properties = {
  "movimento.config.enabled=true",
  "movimento.seed-defaults-enabled=true"
})
class MovimentoConfigSeedServiceTest {

  @Autowired
  private MovimentoConfigSeedService seedService;

  @Autowired
  private MovimentoConfigRepository repository;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @Test
  void shouldSeedDefaultInactiveConfigsByType() {
    Long tenantId = 901L;
    TipoEntidade tipoPadrao = new TipoEntidade();
    tipoPadrao.setTenantId(tenantId);
    tipoPadrao.setNome("Cliente");
    tipoPadrao.setTipoPadrao(true);
    tipoPadrao.setCodigoSeed("CLIENTE");
    tipoPadrao.setAtivo(true);
    tipoEntidadeRepository.save(tipoPadrao);

    seedService.seedDefaults(tenantId);

    for (MovimentoTipo tipo : MovimentoTipo.values()) {
      assertThat(repository.existsByTenantIdAndTipoMovimento(tenantId, tipo)).isTrue();
    }
  }
}
