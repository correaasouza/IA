package com.ia.app.dto;

import jakarta.validation.constraints.Size;

public record RhQualificacaoRequest(
  String nome,
  Boolean completo,
  @Size(max = 1) String tipo,
  Boolean ativo
) {}

