package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import java.math.BigDecimal;
import java.util.UUID;

public record SalePriceGridRowResponse(
  Long id,
  Long priceBookId,
  Long variantId,
  CatalogConfigurationType catalogType,
  Long catalogItemId,
  UUID tenantUnitId,
  BigDecimal priceFinal
) {}
