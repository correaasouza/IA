package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.time.Instant;
import java.time.LocalDate;

public record MovimentoEstoqueResponse(
  Long id,
  MovimentoTipo tipoMovimento,
  Long empresaId,
  String nome,
  LocalDate dataMovimento,
  Long movimentoConfigId,
  Long tipoEntidadePadraoId,
  Long version,
  Instant createdAt,
  Instant updatedAt
) {}
