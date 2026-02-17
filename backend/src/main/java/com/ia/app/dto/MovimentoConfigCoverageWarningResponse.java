package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;

public record MovimentoConfigCoverageWarningResponse(
  Long empresaId,
  String empresaNome,
  MovimentoTipo tipoMovimento,
  String mensagem
) {}
