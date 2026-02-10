package com.ia.app.dto;

public record TipoEntidadeResponse(
  Long id,
  String codigo,
  String nome,
  boolean ativo,
  Integer versao
) {}
