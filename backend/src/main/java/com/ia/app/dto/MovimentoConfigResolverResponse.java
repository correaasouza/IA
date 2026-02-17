package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.util.List;

public record MovimentoConfigResolverResponse(
  Long configuracaoId,
  MovimentoTipo tipoMovimento,
  Long empresaId,
  String contextoKey,
  Long tipoEntidadePadraoId,
  List<Long> tiposEntidadePermitidos
) {}
