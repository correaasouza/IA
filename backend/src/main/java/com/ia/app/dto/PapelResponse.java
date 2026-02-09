package com.ia.app.dto;

public record PapelResponse(
  Long id,
  String nome,
  String descricao,
  boolean ativo
) {}
