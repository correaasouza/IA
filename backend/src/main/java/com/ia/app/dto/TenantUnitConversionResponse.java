package com.ia.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TenantUnitConversionResponse(
  UUID id,
  Long tenantId,
  UUID unidadeOrigemId,
  String unidadeOrigemSigla,
  UUID unidadeDestinoId,
  String unidadeDestinoSigla,
  BigDecimal fator
) {}
