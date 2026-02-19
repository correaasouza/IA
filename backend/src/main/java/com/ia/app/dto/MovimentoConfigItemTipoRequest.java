package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record MovimentoConfigItemTipoRequest(
  @NotNull Long movimentoItemTipoId,
  Boolean cobrar
) {}
