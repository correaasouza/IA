package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampoDefinicaoRequest(
  @NotNull Long tipoEntidadeId,
  @NotBlank String nome,
  String label,
  @NotBlank String tipo,
  @NotNull Boolean obrigatorio,
  Integer tamanho
) {}
