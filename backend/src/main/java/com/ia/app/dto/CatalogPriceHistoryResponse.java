package com.ia.app.dto;

import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceChangeAction;
import com.ia.app.domain.PriceChangeOriginType;
import com.ia.app.domain.PriceChangeSourceType;
import java.math.BigDecimal;
import java.time.Instant;

public record CatalogPriceHistoryResponse(
  Long id,
  PriceChangeAction action,
  PriceChangeSourceType sourceType,
  PriceChangeOriginType originType,
  Long originId,
  CatalogPriceType priceType,
  BigDecimal oldPriceFinal,
  BigDecimal newPriceFinal,
  Long priceBookId,
  String priceBookName,
  Long variantId,
  String changedBy,
  Instant changedAt
) {}
