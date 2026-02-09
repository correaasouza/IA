package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContatoRequest(
  @NotNull Long entidadeRegistroId,
  @NotBlank String tipo,
  @NotBlank String valor,
  @NotNull Boolean principal
) {}
