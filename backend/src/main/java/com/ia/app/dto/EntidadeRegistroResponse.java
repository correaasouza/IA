package com.ia.app.dto;

public record EntidadeRegistroResponse(
  Long id,
  Long entidadeDefinicaoId,
  String nome,
  String apelido,
  String cpfCnpj,
  String tipoPessoa,
  boolean ativo
) {}
