package com.ia.app.security;

import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.tenant.TenantContext;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("permissaoGuard")
public class PermissaoGuard {

  private final PermissaoUsuarioService permissaoUsuarioService;

  public PermissaoGuard(PermissaoUsuarioService permissaoUsuarioService) {
    this.permissaoUsuarioService = permissaoUsuarioService;
  }

  public boolean hasPermissao(String codigo) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    if (isGlobalMaster(auth)) {
      return true;
    }
    if (hasAuthority(auth, "ROLE_MASTER") || hasAuthority(auth, "ROLE_ADMIN")) {
      return true;
    }
    if (hasAuthority(auth, "ROLE_" + codigo)) {
      return true;
    }
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return false;
    }
    String userId = auth.getName();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      userId = jwtAuth.getToken().getSubject();
    }
    Set<String> permissoes = permissaoUsuarioService.permissoes(tenantId, userId);
    return permissoes.contains(codigo);
  }

  private boolean hasAuthority(Authentication auth, String role) {
    for (GrantedAuthority authority : auth.getAuthorities()) {
      if (role.equals(authority.getAuthority())) return true;
    }
    return false;
  }

  private boolean isGlobalMaster(Authentication auth) {
    if (hasAuthority(auth, "ROLE_MASTER")) {
      return true;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
      return preferredUsername != null && preferredUsername.equalsIgnoreCase("master");
    }
    String name = auth.getName();
    return name != null && name.equalsIgnoreCase("master");
  }
}

