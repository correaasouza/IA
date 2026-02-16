package com.ia.app.domain;

import java.util.Locale;

public enum CatalogConfigurationType {
  PRODUCTS,
  SERVICES;

  public static CatalogConfigurationType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("catalog_configuration_type_invalid");
    }
    try {
      return CatalogConfigurationType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("catalog_configuration_type_invalid");
    }
  }
}
