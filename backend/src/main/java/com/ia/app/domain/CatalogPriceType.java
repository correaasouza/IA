package com.ia.app.domain;

import java.util.Locale;

public enum CatalogPriceType {
  PURCHASE,
  COST,
  AVERAGE_COST,
  SALE_BASE;

  public static CatalogPriceType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return CatalogPriceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("catalog_price_type_invalid");
    }
  }
}
