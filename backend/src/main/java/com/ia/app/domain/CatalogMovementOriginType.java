package com.ia.app.domain;

import java.util.Locale;

public enum CatalogMovementOriginType {
  MUDANCA_GRUPO,
  MOVIMENTO_ESTOQUE,
  SYSTEM,
  WORKFLOW_ACTION;

  public static CatalogMovementOriginType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return CatalogMovementOriginType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("catalog_stock_origin_invalid");
    }
  }
}
