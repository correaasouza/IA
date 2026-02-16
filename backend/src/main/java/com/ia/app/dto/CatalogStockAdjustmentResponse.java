package com.ia.app.dto;

public record CatalogStockAdjustmentResponse(
  Long id,
  String codigo,
  String nome,
  String tipo,
  Integer ordem,
  boolean active,
  Long version,
  Long estoqueOrigemAgrupadorId,
  Long estoqueOrigemTipoId,
  Long estoqueOrigemFilialId,
  Long estoqueDestinoAgrupadorId,
  Long estoqueDestinoTipoId,
  Long estoqueDestinoFilialId
) {}
