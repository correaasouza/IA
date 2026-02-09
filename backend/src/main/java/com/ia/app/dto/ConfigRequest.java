package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigRequest(
  @NotBlank String screenId,
  @NotBlank String scopeTipo,
  String scopeValor,
  @NotNull String configJson
) {}
