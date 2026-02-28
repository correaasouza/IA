package com.ia.app.dto;

public record EntidadeContatoFormaRequest(
  String tipoContato,
  String valor,
  Boolean preferencial,
  Long version
) {}
