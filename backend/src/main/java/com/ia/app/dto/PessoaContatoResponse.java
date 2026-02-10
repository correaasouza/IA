package com.ia.app.dto;

public record PessoaContatoResponse(
  Long id,
  Long pessoaId,
  String tipo,
  String valor,
  boolean principal
) {}
