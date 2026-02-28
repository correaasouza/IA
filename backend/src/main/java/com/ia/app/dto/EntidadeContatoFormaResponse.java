package com.ia.app.dto;

public record EntidadeContatoFormaResponse(
  Long id,
  Long contatoId,
  String tipoContato,
  String valor,
  String valorNormalizado,
  boolean preferencial,
  Long version
) {}
