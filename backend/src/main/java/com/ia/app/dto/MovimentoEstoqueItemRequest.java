package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record MovimentoEstoqueItemRequest(
  @NotNull Long movimentoItemTipoId,
  @NotNull Long catalogItemId,
  UUID tenantUnitId,
  Long priceBookId,
  Long variantId,
  @NotNull BigDecimal quantidade,
  BigDecimal valorUnitario,
  Integer ordem,
  String observacao
) {}
