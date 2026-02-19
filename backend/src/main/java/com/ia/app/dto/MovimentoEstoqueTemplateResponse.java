package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.util.List;

public record MovimentoEstoqueTemplateResponse(
  MovimentoTipo tipoMovimento,
  Long empresaId,
  Long movimentoConfigId,
  Long tipoEntidadePadraoId,
  Long stockAdjustmentId,
  java.util.List<MovimentoStockAdjustmentOptionResponse> stockAdjustments,
  List<Long> tiposEntidadePermitidos,
  List<MovimentoTipoItemTemplateResponse> tiposItensPermitidos,
  String nome
) {}
