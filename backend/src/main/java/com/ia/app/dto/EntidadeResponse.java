package com.ia.app.dto;

public record EntidadeResponse(
  Long id,
  Long tipoEntidadeId,
  Long pessoaId,
  String alerta,
  boolean ativo
) {}
