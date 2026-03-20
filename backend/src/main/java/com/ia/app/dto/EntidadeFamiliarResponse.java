package com.ia.app.dto;

public record EntidadeFamiliarResponse(
  Long id,
  Long registroEntidadeId,
  String nome,
  boolean dependente,
  String parentesco,
  Long version
) {}
