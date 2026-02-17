package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.time.Instant;
import java.util.List;

public record MovimentoConfigResponse(
  Long id,
  MovimentoTipo tipoMovimento,
  String nome,
  String contextoKey,
  boolean ativo,
  Long version,
  List<Long> empresaIds,
  List<Long> tiposEntidadePermitidos,
  Long tipoEntidadePadraoId,
  Instant createdAt,
  Instant updatedAt
) {}
