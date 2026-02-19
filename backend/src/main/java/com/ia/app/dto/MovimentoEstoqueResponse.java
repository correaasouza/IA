package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record MovimentoEstoqueResponse(
  Long id,
  MovimentoTipo tipoMovimento,
  Long empresaId,
  String nome,
  Long movimentoConfigId,
  Long tipoEntidadePadraoId,
  List<MovimentoEstoqueItemResponse> itens,
  Integer totalItens,
  BigDecimal totalCobrado,
  Long version,
  Instant createdAt,
  Instant updatedAt
) {}
