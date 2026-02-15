package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TipoEntidadeRequest(
  @NotBlank String nome,
  @NotNull Boolean ativo
) {}
