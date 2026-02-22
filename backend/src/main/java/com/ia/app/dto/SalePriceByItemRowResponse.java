package com.ia.app.dto;

import com.ia.app.domain.SalePriceSource;
import java.math.BigDecimal;

public record SalePriceByItemRowResponse(
  Long priceBookId,
  String priceBookName,
  boolean priceBookActive,
  Long variantId,
  String variantName,
  Boolean variantActive,
  BigDecimal priceFinal,
  Long salePriceId,
  Long resolvedVariantId,
  SalePriceSource source
) {}
