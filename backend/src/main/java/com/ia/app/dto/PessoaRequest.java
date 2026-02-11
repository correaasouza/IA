package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PessoaRequest(
  @NotBlank String nome,
  String apelido,
  String cpf,
  String cnpj,
  String idEstrangeiro,
  String tipoPessoa,
  boolean ativo
) {}
