package com.ia.app.security;

import com.ia.app.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("globalScopeGuard")
public class GlobalScopeGuard {

  private static final Long MASTER_TENANT_ID = 1L;

  public boolean isMasterInMasterTenant() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    boolean isMasterRole = authentication.getAuthorities().stream()
      .anyMatch(authority -> "ROLE_MASTER".equals(authority.getAuthority()));
    if (!isMasterRole) {
      return false;
    }
    Long tenantId = TenantContext.getTenantId();
    return MASTER_TENANT_ID.equals(tenantId);
  }
}
