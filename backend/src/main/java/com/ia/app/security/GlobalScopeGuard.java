package com.ia.app.security;

import com.ia.app.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("globalScopeGuard")
public class GlobalScopeGuard {

  private static final Long MASTER_TENANT_ID = 1L;
  private final AuthorizationService authorizationService;

  public GlobalScopeGuard(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public boolean isGlobalMaster() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authorizationService.isGlobalMaster(authentication, TenantContext.getTenantId());
  }

  public boolean isMasterInMasterTenant() {
    if (!isGlobalMaster()) {
      return false;
    }
    Long tenantId = TenantContext.getTenantId();
    return MASTER_TENANT_ID.equals(tenantId);
  }
}
