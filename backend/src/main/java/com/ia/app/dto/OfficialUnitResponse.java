package com.ia.app.dto;

import com.ia.app.domain.OfficialUnitOrigin;
import java.util.UUID;

public record OfficialUnitResponse(
  UUID id,
  String codigoOficial,
  String descricao,
  boolean ativo,
  OfficialUnitOrigin origem
) {}
