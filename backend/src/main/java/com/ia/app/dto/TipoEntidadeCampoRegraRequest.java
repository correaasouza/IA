package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record TipoEntidadeCampoRegraRequest(
  @NotBlank String campo,
  boolean habilitado,
  boolean requerido,
  boolean visivel,
  boolean editavel,
  String label
) {}
