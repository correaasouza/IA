package com.ia.app.dto;

public record RegistroEntidadeResponse(
  Long id,
  Long empresaId,
  Long tipoEntidadeConfigAgrupadorId,
  Long codigo,
  Long grupoEntidadeId,
  String grupoEntidadeNome,
  Long priceBookId,
  String alerta,
  String observacao,
  String parecer,
  String codigoBarras,
  String textoTermoQuitacao,
  Long tratamentoId,
  Long version,
  boolean ativo,
  PessoaVinculoResponse pessoa
) {}
