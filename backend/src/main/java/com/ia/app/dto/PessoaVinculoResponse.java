package com.ia.app.dto;

public record PessoaVinculoResponse(
  Long id,
  String nome,
  String apelido,
  String tipoRegistro,
  String registroFederal,
  String tipoPessoa,
  String genero,
  String nacionalidade,
  String naturalidade,
  String estadoCivil,
  String dataNascimento
) {}
