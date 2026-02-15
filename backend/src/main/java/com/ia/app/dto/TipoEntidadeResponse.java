package com.ia.app.dto;

public record TipoEntidadeResponse(
  Long id,
  String nome,
  String codigoSeed,
  boolean tipoPadrao,
  boolean ativo
) {}
