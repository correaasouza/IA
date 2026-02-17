package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import java.time.LocalDate;
import java.util.List;

public record MovimentoEstoqueTemplateResponse(
  MovimentoTipo tipoMovimento,
  Long empresaId,
  Long movimentoConfigId,
  Long tipoEntidadePadraoId,
  List<Long> tiposEntidadePermitidos,
  String nome,
  LocalDate dataMovimento
) {}
