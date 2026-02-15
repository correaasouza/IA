package com.ia.app.service;

import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoEntidadeConfigAgrupadorSyncService {

  private final TipoEntidadeConfigPorAgrupadorRepository repository;

  public TipoEntidadeConfigAgrupadorSyncService(TipoEntidadeConfigPorAgrupadorRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void onAgrupadorCreated(Long tenantId, Long tipoEntidadeId, Long agrupadorId) {
    if (tenantId == null || tipoEntidadeId == null || agrupadorId == null) {
      return;
    }
    if (repository.findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupadorId).isPresent()) {
      return;
    }
    TipoEntidadeConfigPorAgrupador config = new TipoEntidadeConfigPorAgrupador();
    config.setTenantId(tenantId);
    config.setTipoEntidadeId(tipoEntidadeId);
    config.setAgrupadorId(agrupadorId);
    config.setObrigarUmTelefone(false);
    config.setAtivo(true);
    try {
      repository.save(config);
    } catch (DataIntegrityViolationException ex) {
      if (repository.findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupadorId).isEmpty()) {
        throw ex;
      }
    }
  }

  @Transactional
  public void onAgrupadorRemoved(Long tenantId, Long tipoEntidadeId, Long agrupadorId) {
    repository.findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupadorId)
      .ifPresent(config -> {
        config.setAtivo(false);
        repository.save(config);
      });
  }
}
