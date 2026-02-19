package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import java.time.Instant;

public record MovimentoItemTipoResponse(
  Long id,
  String nome,
  CatalogConfigurationType catalogType,
  boolean ativo,
  Long version,
  Instant createdAt,
  Instant updatedAt
) {}
