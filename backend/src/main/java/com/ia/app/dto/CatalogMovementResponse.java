package com.ia.app.dto;

import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.ConversionFactorSource;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CatalogMovementResponse(
  Long id,
  Long catalogoId,
  Long agrupadorEmpresaId,
  CatalogMovementOriginType origemMovimentacaoTipo,
  String origemMovimentacaoCodigo,
  Long origemMovimentacaoId,
  String movimentoTipo,
  String origemMovimentoItemCodigo,
  String workflowOrigin,
  Long workflowEntityId,
  String workflowTransitionKey,
  Instant dataHoraMovimentacao,
  String observacao,
  UUID tenantUnitId,
  UUID unidadeBaseCatalogoTenantUnitId,
  BigDecimal quantidadeInformada,
  BigDecimal quantidadeConvertidaBase,
  BigDecimal fatorAplicado,
  ConversionFactorSource fatorFonte,
  List<CatalogMovementLineResponse> lines
) {}
