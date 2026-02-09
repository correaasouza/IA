package com.ia.app.dto;

public record ContatoTipoResponse(
  Long id,
  String codigo,
  String nome,
  boolean ativo,
  boolean obrigatorio,
  boolean principalUnico,
  String mascara,
  String regexValidacao
) {}
