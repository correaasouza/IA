package com.ia.app.dto;

import com.ia.app.domain.CatalogMovementMetricType;
import java.math.BigDecimal;

public record CatalogMovementLineResponse(
  Long id,
  Long agrupadorEmpresaId,
  Long estoqueTipoId,
  String estoqueTipoCodigo,
  String estoqueTipoNome,
  Long filialId,
  String filialNome,
  CatalogMovementMetricType metricType,
  BigDecimal beforeValue,
  BigDecimal delta,
  BigDecimal afterValue
) {}
