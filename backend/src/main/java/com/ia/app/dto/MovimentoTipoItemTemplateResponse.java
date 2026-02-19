package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;

public record MovimentoTipoItemTemplateResponse(
  Long tipoItemId,
  String nome,
  CatalogConfigurationType catalogType,
  boolean cobrar
) {}
