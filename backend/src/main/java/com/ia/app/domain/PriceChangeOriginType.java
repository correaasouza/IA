package com.ia.app.domain;

import java.util.Locale;

public enum PriceChangeOriginType {
  ALTERACAO_TABELA_PRECO,
  ALTERACAO_PRECO_BASE;

  public static PriceChangeOriginType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return PriceChangeOriginType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("price_change_origin_invalid");
    }
  }
}
