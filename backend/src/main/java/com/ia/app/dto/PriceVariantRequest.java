package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriceVariantRequest(
  @NotBlank @Size(max = 120) String name,
  @NotNull Boolean active
) {}
