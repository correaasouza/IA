package com.ia.app.dto;

import java.util.List;

public record CatalogStockBalanceViewResponse(
  Long catalogoId,
  Long agrupadorEmpresaId,
  List<CatalogStockBalanceRowResponse> rows,
  List<CatalogStockConsolidatedResponse> consolidado
) {}
