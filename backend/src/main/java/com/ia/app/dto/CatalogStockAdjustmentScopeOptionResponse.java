package com.ia.app.dto;

public record CatalogStockAdjustmentScopeOptionResponse(
  Long agrupadorId,
  String agrupadorNome,
  Long estoqueTipoId,
  String estoqueTipoCodigo,
  String estoqueTipoNome,
  Long filialId,
  String filialNome,
  String label
) {}

