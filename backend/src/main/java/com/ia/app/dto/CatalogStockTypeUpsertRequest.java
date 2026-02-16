package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogStockTypeUpsertRequest(
  @NotBlank @Size(max = 40) String codigo,
  @NotBlank @Size(max = 120) String nome,
  Integer ordem,
  Boolean active
) {}
