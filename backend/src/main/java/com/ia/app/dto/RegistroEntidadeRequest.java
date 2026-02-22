package com.ia.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistroEntidadeRequest(
  Long grupoEntidadeId,
  Long priceBookId,
  @NotNull Boolean ativo,
  @NotNull @Valid PessoaVinculoRequest pessoa
) {
  public RegistroEntidadeRequest(Long grupoEntidadeId, @NotNull Boolean ativo, @NotNull @Valid PessoaVinculoRequest pessoa) {
    this(grupoEntidadeId, null, ativo, pessoa);
  }
}
