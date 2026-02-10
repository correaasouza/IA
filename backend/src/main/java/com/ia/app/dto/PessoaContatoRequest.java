package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PessoaContatoRequest(
  @NotBlank String tipo,
  @NotBlank String valor,
  boolean principal
) {}
