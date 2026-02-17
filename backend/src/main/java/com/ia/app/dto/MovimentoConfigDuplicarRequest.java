package com.ia.app.dto;

import jakarta.validation.constraints.Size;

public record MovimentoConfigDuplicarRequest(
  @Size(max = 120) String nome,
  @Size(max = 120) String contextoKey,
  Boolean ativo
) {}
