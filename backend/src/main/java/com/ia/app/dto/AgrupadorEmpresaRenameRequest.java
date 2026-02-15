package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgrupadorEmpresaRenameRequest(
  @NotBlank @Size(max = 120) String nome
) {}
