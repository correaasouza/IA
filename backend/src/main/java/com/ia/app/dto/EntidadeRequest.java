package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record EntidadeRequest(
  @NotNull Long tipoEntidadeId,
  @NotNull Long pessoaId,
  String alerta,
  boolean ativo
) {}
