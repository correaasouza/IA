package com.ia.app.service;

import com.ia.app.repository.LocatarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class TipoEntidadeStartupSeedService {

  private final LocatarioRepository locatarioRepository;
  private final TipoEntidadeSeedService tipoEntidadeSeedService;

  @Value("${tenant.seed-tipo-entidade-on-startup:true}")
  private boolean enabled;

  public TipoEntidadeStartupSeedService(
      LocatarioRepository locatarioRepository,
      TipoEntidadeSeedService tipoEntidadeSeedService) {
    this.locatarioRepository = locatarioRepository;
    this.tipoEntidadeSeedService = tipoEntidadeSeedService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seedExistingTenants() {
    if (!enabled) {
      return;
    }
    locatarioRepository.findAll().forEach(locatario ->
      tipoEntidadeSeedService.seedDefaults(locatario.getId()));
  }
}
