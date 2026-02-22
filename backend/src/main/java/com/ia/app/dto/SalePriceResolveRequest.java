package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SalePriceResolveRequest(
  @NotNull Long priceBookId,
  Long variantId,
  @NotNull CatalogConfigurationType catalogType,
  @NotNull Long catalogItemId,
  UUID tenantUnitId
) {}
