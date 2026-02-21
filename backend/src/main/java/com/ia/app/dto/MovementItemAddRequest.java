package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record MovementItemAddRequest(
  @NotNull @Positive Long movementItemTypeId,
  @NotNull @Positive Long catalogItemId,
  @NotNull @Positive BigDecimal quantidade,
  @PositiveOrZero BigDecimal valorUnitario,
  String observacao
) {}