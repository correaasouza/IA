package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.PriceAdjustmentKind;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SalePriceApplyByGroupRequest(
  @NotNull Long priceBookId,
  Long variantId,
  @NotNull CatalogConfigurationType catalogType,
  Long catalogGroupId,
  String text,
  Long catalogItemId,
  PriceAdjustmentKind adjustmentKind,
  @NotNull BigDecimal adjustmentValue,
  Boolean includeChildren,
  Boolean overwriteExisting
) {}
