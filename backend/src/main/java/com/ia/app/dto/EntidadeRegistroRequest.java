package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EntidadeRegistroRequest(
  @NotNull Long entidadeDefinicaoId,
  @NotBlank String nome,
  String apelido,
  @NotBlank String cpfCnpj,
  String tipoPessoa,
  @NotNull Boolean ativo
) {}
