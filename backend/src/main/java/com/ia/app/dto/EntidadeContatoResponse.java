package com.ia.app.dto;

public record EntidadeContatoResponse(
  Long id,
  Long registroEntidadeId,
  String nome,
  String cargo,
  Long version
) {}
