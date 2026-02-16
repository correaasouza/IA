package com.ia.app.dto;

import java.math.BigDecimal;

public record CatalogStockBalanceRowResponse(
  Long estoqueTipoId,
  String estoqueTipoCodigo,
  String estoqueTipoNome,
  Long filialId,
  String filialNome,
  BigDecimal quantidadeAtual,
  BigDecimal precoAtual
) {}
