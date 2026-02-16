package com.ia.app.dto;

import com.ia.app.domain.CatalogNumberingMode;

public record CatalogConfigurationByGroupResponse(
  Long agrupadorId,
  String agrupadorNome,
  CatalogNumberingMode numberingMode,
  boolean active
) {}
