package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimentoTemplateRequest(
  @NotNull @Positive Long empresaId
) {}
