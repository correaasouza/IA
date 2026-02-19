package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;

public record MovimentoItemCatalogOptionResponse(
  Long id,
  CatalogConfigurationType catalogType,
  Long codigo,
  String nome,
  String descricao
) {}
