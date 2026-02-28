package com.ia.app.dto;

public record EntidadeQualificacaoItemRequest(
  Long rhQualificacaoId,
  Boolean completo,
  String tipo,
  Long version
) {}
