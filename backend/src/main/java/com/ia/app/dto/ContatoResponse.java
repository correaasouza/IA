package com.ia.app.dto;

public record ContatoResponse(
  Long id,
  Long entidadeRegistroId,
  String tipo,
  String valor,
  boolean principal
) {}
