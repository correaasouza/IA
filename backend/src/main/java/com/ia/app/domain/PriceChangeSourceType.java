package com.ia.app.domain;

import java.util.Locale;

public enum PriceChangeSourceType {
  SALE_PRICE,
  CATALOG_ITEM_PRICE;

  public static PriceChangeSourceType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return PriceChangeSourceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("price_change_source_invalid");
    }
  }
}
