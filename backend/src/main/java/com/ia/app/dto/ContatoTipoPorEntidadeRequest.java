package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record ContatoTipoPorEntidadeRequest(
  @NotNull Long entidadeDefinicaoId,
  @NotNull Long contatoTipoId,
  @NotNull Boolean obrigatorio,
  @NotNull Boolean principalUnico
) {}
