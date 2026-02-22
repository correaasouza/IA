package com.ia.app.dto;

import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import java.math.BigDecimal;

public record CatalogItemPriceResponse(
  CatalogPriceType priceType,
  BigDecimal priceFinal,
  PriceAdjustmentKind adjustmentKind,
  BigDecimal adjustmentValue
) {}
