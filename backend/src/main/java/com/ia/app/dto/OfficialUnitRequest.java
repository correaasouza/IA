package com.ia.app.dto;

import com.ia.app.domain.OfficialUnitOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OfficialUnitRequest(
  @NotBlank @Size(max = 20) String codigoOficial,
  @NotBlank @Size(max = 160) String descricao,
  @NotNull Boolean ativo,
  OfficialUnitOrigin origem
) {}
