package com.ia.app.dto;

public record PessoaResponse(
  Long id,
  String nome,
  String apelido,
  String cpf,
  String cnpj,
  String idEstrangeiro,
  String tipoPessoa,
  boolean ativo
) {}
