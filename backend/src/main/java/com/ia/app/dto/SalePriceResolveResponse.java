package com.ia.app.dto;

import com.ia.app.domain.SalePriceSource;
import java.math.BigDecimal;

public record SalePriceResolveResponse(
  BigDecimal priceFinal,
  Long salePriceId,
  Long resolvedVariantId,
  SalePriceSource source
) {}
