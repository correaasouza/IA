package com.ia.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TenantUnitConversionRequest(
  @NotNull UUID unidadeOrigemId,
  @NotNull UUID unidadeDestinoId,
  @NotNull @DecimalMin(value = "0.000000000001", inclusive = true) BigDecimal fator
) {}
