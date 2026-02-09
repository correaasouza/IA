package com.ia.app.dto;

import java.util.Map;

public record RegistroEntidadeResponse(
  Long id,
  Long tipoEntidadeId,
  Map<String, Object> valores
) {}
