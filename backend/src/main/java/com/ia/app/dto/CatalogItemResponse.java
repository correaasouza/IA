package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;

public record CatalogItemResponse(
  Long id,
  CatalogConfigurationType type,
  Long catalogConfigurationId,
  Long agrupadorEmpresaId,
  String agrupadorEmpresaNome,
  Long catalogGroupId,
  String catalogGroupNome,
  Long codigo,
  String nome,
  String descricao,
  boolean ativo
) {}
