package com.ia.app.dto;

import com.ia.app.domain.CatalogNumberingMode;
import jakarta.validation.constraints.NotNull;

public record CatalogConfigurationUpdateRequest(
  @NotNull CatalogNumberingMode numberingMode
) {}
