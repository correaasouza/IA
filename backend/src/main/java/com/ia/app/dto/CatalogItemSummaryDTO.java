package com.ia.app.dto;

import java.math.BigDecimal;

public record CatalogItemSummaryDTO(
  Long id,
  String catalogType,
  Long codigo,
  String nome,
  String descricao,
  Long groupId,
  String groupPath,
  String groupBreadcrumb,
  Boolean ativo,
  String unidade,
  BigDecimal precoBase,
  BigDecimal estoqueDisponivel
) {}