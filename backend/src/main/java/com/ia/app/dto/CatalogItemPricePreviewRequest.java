package com.ia.app.dto;

import jakarta.validation.Valid;
import java.util.List;

public record CatalogItemPricePreviewRequest(
  Long catalogItemId,
  List<@Valid CatalogItemPriceInput> prices
) {}
