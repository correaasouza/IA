package com.ia.app.dto;

import com.ia.app.domain.CatalogPriceEditedField;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CatalogItemPriceInput(
  @NotNull CatalogPriceType priceType,
  BigDecimal priceFinal,
  PriceAdjustmentKind adjustmentKind,
  BigDecimal adjustmentValue,
  CatalogPriceEditedField lastEditedField
) {}
