package com.ia.app.security;

import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.tenant.TenantContext;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("permissaoGuard")
public class PermissaoGuard {

  private final PermissaoUsuarioService permissaoUsuarioService;
  private final AuthorizationService authorizationService;

  public PermissaoGuard(
      PermissaoUsuarioService permissaoUsuarioService,
      AuthorizationService authorizationService) {
    this.permissaoUsuarioService = permissaoUsuarioService;
    this.authorizationService = authorizationService;
  }

  public boolean hasPermissao(String codigo) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    if (authorizationService.isCurrentGlobalMaster()) {
      return true;
    }
    Long tenantId = TenantContext.getTenantId();
    String userId = authorizationService.currentUserId();
    if (tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    if (authorizationService.canAccessTenant(userId, tenantId)
        && (hasRole(auth, "ROLE_ADMIN") || hasRole(auth, "ROLE_MASTER"))) {
      return true;
    }
    Set<String> permissoes = permissaoUsuarioService.permissoes(tenantId, userId);
    if (permissoes.contains(codigo)) {
      return true;
    }
    return permissaoUsuarioService.papeis(tenantId, userId).stream()
      .anyMatch(papel -> "ADMIN".equalsIgnoreCase(papel) || "MASTER".equalsIgnoreCase(papel));
  }

  private boolean hasRole(Authentication authentication, String role) {
    if (authentication == null) {
      return false;
    }
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (role.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }
}

