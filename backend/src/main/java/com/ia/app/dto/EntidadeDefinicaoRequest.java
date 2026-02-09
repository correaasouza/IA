package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EntidadeDefinicaoRequest(
  @NotBlank String codigo,
  @NotBlank String nome,
  @NotNull Boolean ativo,
  String roleRequired
) {}
