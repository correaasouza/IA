package com.ia.app.service;

import com.ia.app.repository.LocatarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MovimentoConfigStartupSeedService {

  private final LocatarioRepository locatarioRepository;
  private final MovimentoConfigSeedService movimentoConfigSeedService;

  public MovimentoConfigStartupSeedService(
      LocatarioRepository locatarioRepository,
      MovimentoConfigSeedService movimentoConfigSeedService) {
    this.locatarioRepository = locatarioRepository;
    this.movimentoConfigSeedService = movimentoConfigSeedService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void seedExistingTenants() {
    if (!movimentoConfigSeedService.isEnabled()) {
      return;
    }
    locatarioRepository.findAll().forEach(locatario ->
      movimentoConfigSeedService.seedDefaults(locatario.getId()));
  }
}
