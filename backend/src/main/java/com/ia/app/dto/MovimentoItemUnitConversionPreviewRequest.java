package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record MovimentoItemUnitConversionPreviewRequest(
  @NotNull CatalogConfigurationType catalogType,
  @NotNull @Positive Long catalogItemId,
  @NotNull UUID unidadeOrigemId,
  @NotNull UUID unidadeDestinoId,
  @NotNull @PositiveOrZero BigDecimal quantidadeOrigem,
  @NotNull @PositiveOrZero BigDecimal valorUnitarioOrigem
) {}
