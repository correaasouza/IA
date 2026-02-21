package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record MovementItemAddRequest(
  @NotNull @Positive Long movementItemTypeId,
  @NotNull @Positive Long catalogItemId,
  UUID tenantUnitId,
  @NotNull @Positive BigDecimal quantidade,
  @PositiveOrZero BigDecimal valorUnitario,
  String observacao
) {}
