package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record MovimentoEstoqueResponse(
  Long id,
  Long codigo,
  MovimentoTipo tipoMovimento,
  Long empresaId,
  String nome,
  Long movimentoConfigId,
  Long tipoEntidadePadraoId,
  Long stockAdjustmentId,
  boolean finalizado,
  String status,
  List<MovimentoEstoqueItemResponse> itens,
  Integer totalItens,
  BigDecimal totalCobrado,
  Long version,
  Instant createdAt,
  Instant updatedAt
) {}
