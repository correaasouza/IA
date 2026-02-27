package com.ia.app.security;

import com.ia.app.tenant.TenantContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("globalScopeGuard")
public class GlobalScopeGuard {

  private static final Long MASTER_TENANT_ID = 1L;

  public boolean isGlobalMaster() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    if (!hasMasterRole(authentication)) {
      return false;
    }
    return isMasterUsername(authentication);
  }

  public boolean isMasterInMasterTenant() {
    if (!isGlobalMaster()) {
      return false;
    }
    Long tenantId = TenantContext.getTenantId();
    return MASTER_TENANT_ID.equals(tenantId);
  }

  private boolean hasMasterRole(Authentication authentication) {
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if ("ROLE_MASTER".equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  private boolean isMasterUsername(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
      return preferredUsername != null && preferredUsername.equalsIgnoreCase("master");
    }
    String name = authentication.getName();
    return name != null && name.equalsIgnoreCase("master");
  }
}
