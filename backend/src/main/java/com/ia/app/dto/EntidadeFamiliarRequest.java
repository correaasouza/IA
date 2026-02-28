package com.ia.app.dto;

public record EntidadeFamiliarRequest(
  Long entidadeParenteId,
  Boolean dependente,
  String parentesco,
  Long version
) {}
