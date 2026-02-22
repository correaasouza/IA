package com.ia.app.dto;

import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBaseMode;
import com.ia.app.domain.PriceUiLockMode;
import java.math.BigDecimal;

public record CatalogPriceRuleResponse(
  Long id,
  CatalogPriceType priceType,
  String customName,
  PriceBaseMode baseMode,
  CatalogPriceType basePriceType,
  PriceAdjustmentKind adjustmentKindDefault,
  BigDecimal adjustmentDefault,
  PriceUiLockMode uiLockMode,
  boolean active
) {}
