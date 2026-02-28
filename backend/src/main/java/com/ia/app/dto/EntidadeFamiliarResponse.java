package com.ia.app.dto;

public record EntidadeFamiliarResponse(
  Long id,
  Long registroEntidadeId,
  Long entidadeParenteId,
  String entidadeParenteNome,
  boolean dependente,
  String parentesco,
  Long version
) {}
