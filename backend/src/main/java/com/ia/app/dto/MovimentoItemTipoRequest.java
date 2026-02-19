package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimentoItemTipoRequest(
  @NotBlank @Size(max = 120) String nome,
  @NotNull CatalogConfigurationType catalogType,
  Boolean ativo
) {}
