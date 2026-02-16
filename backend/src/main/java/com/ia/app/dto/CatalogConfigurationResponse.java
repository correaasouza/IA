package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import java.time.Instant;

public record CatalogConfigurationResponse(
  Long id,
  CatalogConfigurationType type,
  CatalogNumberingMode numberingMode,
  boolean active,
  Long version,
  Instant createdAt,
  Instant updatedAt
) {}
