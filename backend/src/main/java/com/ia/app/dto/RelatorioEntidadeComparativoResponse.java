package com.ia.app.dto;

public record RelatorioEntidadeComparativoResponse(
  Long entidadeDefinicaoId,
  String nome,
  long totalPeriodo1,
  long totalPeriodo2
) {}
