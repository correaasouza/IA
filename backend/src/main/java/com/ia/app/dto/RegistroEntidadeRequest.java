package com.ia.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistroEntidadeRequest(
  Long grupoEntidadeId,
  Long priceBookId,
  String alerta,
  String observacao,
  String parecer,
  String codigoBarras,
  String textoTermoQuitacao,
  Long tratamentoId,
  Long version,
  @NotNull Boolean ativo,
  @NotNull @Valid PessoaVinculoRequest pessoa
) {
  public RegistroEntidadeRequest(Long grupoEntidadeId, @NotNull Boolean ativo, @NotNull @Valid PessoaVinculoRequest pessoa) {
    this(grupoEntidadeId, null, null, null, null, null, null, null, null, ativo, pessoa);
  }
}
