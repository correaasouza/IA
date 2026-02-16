package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;

public record CatalogItemContextResponse(
  Long empresaId,
  String empresaNome,
  CatalogConfigurationType type,
  Long catalogConfigurationId,
  Long agrupadorId,
  String agrupadorNome,
  CatalogNumberingMode numberingMode,
  boolean vinculado,
  String motivo,
  String mensagem
) {}
