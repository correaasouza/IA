package com.ia.app.dto;

public record EntidadeQualificacaoItemResponse(
  Long id,
  Long registroEntidadeId,
  Long rhQualificacaoId,
  String rhQualificacaoNome,
  boolean completo,
  String tipo,
  Long version
) {}
