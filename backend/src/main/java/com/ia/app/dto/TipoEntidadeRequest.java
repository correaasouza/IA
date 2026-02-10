package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record TipoEntidadeRequest(
  @NotBlank String codigo,
  @NotBlank String nome,
  boolean ativo
) {}
