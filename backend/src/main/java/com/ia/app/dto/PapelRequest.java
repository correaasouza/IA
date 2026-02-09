package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PapelRequest(
  @NotBlank String nome,
  String descricao,
  boolean ativo
) {}
