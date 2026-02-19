package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;

public record MovimentoConfigItemTipoResponse(
  Long movimentoItemTipoId,
  String nome,
  CatalogConfigurationType catalogType,
  boolean cobrar,
  boolean ativo
) {}
