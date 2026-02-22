package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriceBookRequest(
  @NotBlank @Size(max = 120) String name,
  @NotNull Boolean active,
  Boolean defaultBook
) {}
