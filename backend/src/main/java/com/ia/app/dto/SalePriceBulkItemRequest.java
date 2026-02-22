package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SalePriceBulkItemRequest(
  @NotNull CatalogConfigurationType catalogType,
  @NotNull Long catalogItemId,
  UUID tenantUnitId,
  BigDecimal priceFinal
) {}
