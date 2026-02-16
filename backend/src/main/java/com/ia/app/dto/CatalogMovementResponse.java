package com.ia.app.dto;

import com.ia.app.domain.CatalogMovementOriginType;
import java.time.Instant;
import java.util.List;

public record CatalogMovementResponse(
  Long id,
  Long catalogoId,
  Long agrupadorEmpresaId,
  CatalogMovementOriginType origemMovimentacaoTipo,
  String origemMovimentacaoCodigo,
  String origemMovimentoItemCodigo,
  Instant dataHoraMovimentacao,
  String observacao,
  List<CatalogMovementLineResponse> lines
) {}
