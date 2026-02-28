package com.ia.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntidadeInfoComercialResponse(
  Long id,
  Long registroEntidadeId,
  LocalDate faturamentoDiaInicial,
  LocalDate faturamentoDiaFinal,
  Integer faturamentoDiasPrazo,
  boolean boletosEnviarEmail,
  Long faturamentoFrequenciaCobrancaId,
  BigDecimal juroTaxaPadrao,
  String ramoAtividade,
  boolean consumidorFinal,
  Long version
) {}
