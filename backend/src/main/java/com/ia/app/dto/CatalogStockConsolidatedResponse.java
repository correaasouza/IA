package com.ia.app.dto;

import java.math.BigDecimal;

public record CatalogStockConsolidatedResponse(
  Long estoqueTipoId,
  String estoqueTipoCodigo,
  String estoqueTipoNome,
  BigDecimal quantidadeTotal,
  BigDecimal precoTotal
) {}
