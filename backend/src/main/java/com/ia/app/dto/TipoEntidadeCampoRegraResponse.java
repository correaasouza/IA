package com.ia.app.dto;

public record TipoEntidadeCampoRegraResponse(
  Long id,
  String campo,
  boolean habilitado,
  boolean requerido,
  boolean visivel,
  boolean editavel,
  String label
) {}
