package com.ia.app.security;

import com.ia.app.service.ConfiguracaoScopeService;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("configuracaoPermissaoGuard")
public class ConfiguracaoPermissaoGuard {

  private final PermissaoGuard permissaoGuard;

  public ConfiguracaoPermissaoGuard(PermissaoGuard permissaoGuard) {
    this.permissaoGuard = permissaoGuard;
  }

  public boolean canGerenciarAgrupadores(String configType) {
    if (configType == null || configType.isBlank()) {
      return false;
    }
    String normalized = configType.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case ConfiguracaoScopeService.TYPE_FORMULARIO,
           ConfiguracaoScopeService.TYPE_COLUNA -> permissaoGuard.hasPermissao("CONFIG_EDITOR");
      case ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE -> permissaoGuard.hasPermissao("ENTIDADE_EDIT");
      default -> false;
    };
  }
}
