package com.ia.app.domain;

import java.util.Locale;

public enum CatalogMovementMetricType {
  QUANTIDADE,
  PRECO;

  public static CatalogMovementMetricType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return CatalogMovementMetricType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("catalog_stock_metric_invalid");
    }
  }
}
