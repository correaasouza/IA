package com.ia.app.dto;

import com.ia.app.domain.ConversionFactorSource;
import java.math.BigDecimal;
import java.util.UUID;

public record MovimentoItemUnitConversionPreviewResponse(
  UUID unidadeOrigemId,
  UUID unidadeDestinoId,
  BigDecimal fatorBaseParaOrigem,
  BigDecimal fatorBaseParaDestino,
  BigDecimal fatorOrigemParaDestino,
  BigDecimal quantidadeDestino,
  BigDecimal valorUnitarioDestino,
  BigDecimal valorTotal,
  BigDecimal quantidadeBase,
  ConversionFactorSource fatorFonte
) {}
