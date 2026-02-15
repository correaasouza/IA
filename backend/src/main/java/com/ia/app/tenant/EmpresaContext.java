package com.ia.app.tenant;

public final class EmpresaContext {
  private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

  private EmpresaContext() {}

  public static void setEmpresaId(Long empresaId) {
    CURRENT.set(empresaId);
  }

  public static Long getEmpresaId() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
