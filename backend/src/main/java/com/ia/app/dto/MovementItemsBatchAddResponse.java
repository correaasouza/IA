package com.ia.app.dto;

import java.math.BigDecimal;
import java.util.List;

public record MovementItemsBatchAddResponse(
  Long movementId,
  Integer addedCount,
  List<MovimentoEstoqueItemResponse> itemsAdded,
  Integer totalItens,
  BigDecimal totalCobrado
) {}