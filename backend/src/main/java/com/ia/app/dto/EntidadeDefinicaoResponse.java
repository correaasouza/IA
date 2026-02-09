package com.ia.app.dto;

public record EntidadeDefinicaoResponse(
  Long id,
  String codigo,
  String nome,
  boolean ativo,
  String roleRequired
) {}
