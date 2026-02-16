package com.ia.app.dto;

import jakarta.validation.constraints.Size;

public record CatalogStockAdjustmentUpsertRequest(
  @Size(max = 120) String nome,
  @Size(max = 20) String tipo,
  Integer ordem,
  Boolean active,
  Long estoqueOrigemAgrupadorId,
  Long estoqueOrigemTipoId,
  Long estoqueOrigemFilialId,
  Long estoqueDestinoAgrupadorId,
  Long estoqueDestinoTipoId,
  Long estoqueDestinoFilialId
) {}
