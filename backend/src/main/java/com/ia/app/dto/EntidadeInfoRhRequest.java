package com.ia.app.dto;

import java.math.BigDecimal;

public record EntidadeInfoRhRequest(
  String atividades,
  String habilidades,
  String experiencias,
  Boolean aceitaViajar,
  Boolean possuiCarro,
  Boolean possuiMoto,
  Long metaMediaHorasVendidasDia,
  BigDecimal metaProdutosVendidos,
  Long version
) {}
