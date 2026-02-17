package com.ia.app.service;

import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoConfigSeedService {

  private final MovimentoConfigRepository movimentoConfigRepository;
  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final MovimentoConfigFeatureToggle featureToggle;

  @Value("${movimento.seed-defaults-enabled:false}")
  private boolean enabled;

  public MovimentoConfigSeedService(
      MovimentoConfigRepository movimentoConfigRepository,
      TipoEntidadeRepository tipoEntidadeRepository,
      MovimentoConfigFeatureToggle featureToggle) {
    this.movimentoConfigRepository = movimentoConfigRepository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.featureToggle = featureToggle;
  }

  public boolean isEnabled() {
    return enabled && featureToggle.isEnabled();
  }

  @Transactional
  public void seedDefaults(Long tenantId) {
    if (tenantId == null || tenantId <= 0 || !isEnabled()) {
      return;
    }

    TipoEntidade tipoEntidadePadrao = tipoEntidadeRepository
      .findFirstByTenantIdAndAtivoTrueOrderByTipoPadraoDescIdAsc(tenantId)
      .orElse(null);
    if (tipoEntidadePadrao == null) {
      return;
    }

    for (MovimentoTipo tipo : MovimentoTipo.values()) {
      if (movimentoConfigRepository.existsByTenantIdAndTipoMovimento(tenantId, tipo)) {
        continue;
      }
      MovimentoConfig config = new MovimentoConfig();
      config.setTenantId(tenantId);
      config.setTipoMovimento(tipo);
      config.setNome("Padrao " + tipo.descricao());
      config.setDescricao("Seed inicial do tipo de movimento.");
      config.setPrioridade(100);
      config.setContextoKey(null);
      config.setTipoEntidadePadraoId(tipoEntidadePadrao.getId());
      config.setAtivo(false);
      config.replaceTiposEntidadePermitidos(List.of(tipoEntidadePadrao.getId()));
      movimentoConfigRepository.save(config);
    }
  }
}
