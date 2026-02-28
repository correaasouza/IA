package com.ia.app.dto;

import java.math.BigDecimal;

public record EntidadeInfoRhResponse(
  Long id,
  Long registroEntidadeId,
  String atividades,
  String habilidades,
  String experiencias,
  boolean aceitaViajar,
  boolean possuiCarro,
  boolean possuiMoto,
  Long metaMediaHorasVendidasDia,
  BigDecimal metaProdutosVendidos,
  Long version
) {}
