package com.ia.app.tenant;

public final class TenantContext {
  private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static void setTenantId(Long tenantId) {
    CURRENT.set(tenantId);
  }

  public static Long getTenantId() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
