package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissaoRequest(
  @NotBlank String codigo,
  @NotBlank String label,
  boolean ativo
) {}
