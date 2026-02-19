package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MovimentoEstoqueItemRequest(
  @NotNull Long movimentoItemTipoId,
  @NotNull Long catalogItemId,
  @NotNull BigDecimal quantidade,
  BigDecimal valorUnitario,
  Integer ordem,
  String observacao
) {}
