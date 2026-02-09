package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RegistroEntidadeRequest(
  @NotNull Long tipoEntidadeId,
  Map<String, Object> valores
) {}
