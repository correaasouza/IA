package com.ia.app.domain;

import java.util.Locale;

public enum CatalogStockAdjustmentType {
  ENTRADA,
  SAIDA,
  TRANSFERENCIA;

  public static CatalogStockAdjustmentType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("catalog_stock_adjustment_tipo_required");
    }
    try {
      return CatalogStockAdjustmentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("catalog_stock_adjustment_tipo_invalid");
    }
  }
}

