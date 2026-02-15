package com.ia.app.dto;

public record PessoaVinculoResponse(
  Long id,
  String nome,
  String apelido,
  String tipoRegistro,
  String registroFederal
) {}
