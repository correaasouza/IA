package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record AgrupadorEmpresaEmpresaRequest(
  @NotNull Long empresaId
) {}
