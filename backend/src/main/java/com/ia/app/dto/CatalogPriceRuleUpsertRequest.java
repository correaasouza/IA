package com.ia.app.dto;

import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBaseMode;
import com.ia.app.domain.PriceUiLockMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CatalogPriceRuleUpsertRequest(
  @NotNull CatalogPriceType priceType,
  @Size(max = 80) String customName,
  @NotNull PriceBaseMode baseMode,
  CatalogPriceType basePriceType,
  @NotNull PriceAdjustmentKind adjustmentKindDefault,
  BigDecimal adjustmentDefault,
  @NotNull PriceUiLockMode uiLockMode,
  @NotNull Boolean active
) {}
