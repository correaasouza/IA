package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContatoTipoRequest(
  @NotBlank String codigo,
  @NotBlank String nome,
  @NotNull Boolean ativo,
  @NotNull Boolean obrigatorio,
  @NotNull Boolean principalUnico,
  String mascara,
  String regexValidacao
) {}
