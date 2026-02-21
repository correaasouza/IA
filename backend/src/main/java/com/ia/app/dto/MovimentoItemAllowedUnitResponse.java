package com.ia.app.dto;

import com.ia.app.domain.ConversionFactorSource;
import java.math.BigDecimal;
import java.util.UUID;

public record MovimentoItemAllowedUnitResponse(
  UUID tenantUnitId,
  String sigla,
  String nome,
  BigDecimal fatorBaseParaUnidade,
  ConversionFactorSource fatorFonte
) {}
