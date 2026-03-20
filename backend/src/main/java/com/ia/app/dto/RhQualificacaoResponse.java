package com.ia.app.dto;

public record RhQualificacaoResponse(
  Long id,
  String nome,
  boolean completo,
  String tipo,
  boolean ativo
) {}

