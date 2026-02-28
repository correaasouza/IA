package com.ia.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntidadeInfoComercialRequest(
  LocalDate faturamentoDiaInicial,
  LocalDate faturamentoDiaFinal,
  Integer faturamentoDiasPrazo,
  Boolean boletosEnviarEmail,
  Long faturamentoFrequenciaCobrancaId,
  BigDecimal juroTaxaPadrao,
  String ramoAtividade,
  Boolean consumidorFinal,
  Long version
) {}
