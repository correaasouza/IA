package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SalePriceApplyByGroupRequest(
  @NotNull Long priceBookId,
  Long variantId,
  @NotNull CatalogConfigurationType catalogType,
  @NotNull Long catalogGroupId,
  @NotNull BigDecimal percentage,
  Boolean includeChildren,
  Boolean overwriteExisting
) {}
