package com.ia.app.dto;

public record RegistroEntidadeResponse(
  Long id,
  Long tipoEntidadeConfigAgrupadorId,
  Long codigo,
  Long grupoEntidadeId,
  String grupoEntidadeNome,
  boolean ativo,
  PessoaVinculoResponse pessoa
) {}
