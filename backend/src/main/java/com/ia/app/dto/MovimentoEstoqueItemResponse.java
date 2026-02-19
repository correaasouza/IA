package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import java.math.BigDecimal;

public record MovimentoEstoqueItemResponse(
  Long id,
  Long movimentoItemTipoId,
  String movimentoItemTipoNome,
  CatalogConfigurationType catalogType,
  Long catalogItemId,
  Long catalogCodigoSnapshot,
  String catalogNomeSnapshot,
  BigDecimal quantidade,
  BigDecimal valorUnitario,
  BigDecimal valorTotal,
  boolean cobrar,
  Integer ordem,
  String observacao
) {}
