package com.ia.app.dto;

public record EntidadeFamiliarRequest(
  String nome,
  Boolean dependente,
  String parentesco,
  Long version
) {}
