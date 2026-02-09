package com.ia.app.dto;

public record RelatorioLocatarioStatusResponse(
  long total,
  long ativos,
  long bloqueados
) {}
