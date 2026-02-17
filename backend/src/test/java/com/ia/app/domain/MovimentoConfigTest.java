package com.ia.app.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MovimentoConfigTest {

  @Test
  void replaceEmpresasShouldKeepExistingLinksWhenIdsUnchanged() {
    MovimentoConfig config = new MovimentoConfig();
    config.setTenantId(1L);
    config.replaceEmpresas(List.of(3L, 4L));

    config.replaceEmpresas(List.of(3L, 4L));

    assertEquals(2, config.getEmpresas().size());
    assertEquals(2, config.getEmpresas().stream().map(MovimentoConfigEmpresa::getEmpresaId).distinct().count());
    assertTrue(config.getEmpresas().stream().anyMatch(item -> Long.valueOf(3L).equals(item.getEmpresaId())));
    assertTrue(config.getEmpresas().stream().anyMatch(item -> Long.valueOf(4L).equals(item.getEmpresaId())));
  }

  @Test
  void replaceTiposEntidadeShouldKeepExistingLinksWhenIdsUnchanged() {
    MovimentoConfig config = new MovimentoConfig();
    config.setTenantId(1L);
    config.replaceTiposEntidadePermitidos(List.of(10L, 11L));

    config.replaceTiposEntidadePermitidos(List.of(10L, 11L));

    assertEquals(2, config.getTiposEntidadePermitidos().size());
    assertEquals(2, config.getTiposEntidadePermitidos().stream()
      .map(MovimentoConfigTipoEntidade::getTipoEntidadeId)
      .distinct()
      .count());
    assertTrue(config.getTiposEntidadePermitidos().stream()
      .anyMatch(item -> Long.valueOf(10L).equals(item.getTipoEntidadeId())));
    assertTrue(config.getTiposEntidadePermitidos().stream()
      .anyMatch(item -> Long.valueOf(11L).equals(item.getTipoEntidadeId())));
  }
}
