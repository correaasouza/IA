package com.ia.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TenantUnitResponse(
  UUID id,
  Long tenantId,
  UUID unidadeOficialId,
  String unidadeOficialCodigo,
  String unidadeOficialDescricao,
  boolean unidadeOficialAtiva,
  String sigla,
  String nome,
  BigDecimal fatorParaOficial,
  boolean systemMirror
) {}
