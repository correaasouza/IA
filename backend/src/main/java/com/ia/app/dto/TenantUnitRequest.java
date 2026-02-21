package com.ia.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TenantUnitRequest(
  @NotNull UUID unidadeOficialId,
  @NotBlank @Size(max = 20) String sigla,
  @NotBlank @Size(max = 160) String nome,
  @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal fatorParaOficial
) {}
