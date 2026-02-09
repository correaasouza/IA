package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record AtalhoUsuarioOrdemRequest(
  @NotNull Long id,
  @NotNull Integer ordem
) {}
