package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.ConversionFactorSource;
import java.math.BigDecimal;
import java.util.UUID;

public record MovimentoEstoqueItemResponse(
  Long id,
  Long codigo,
  Long movimentoItemTipoId,
  String movimentoItemTipoNome,
  CatalogConfigurationType catalogType,
  Long catalogItemId,
  Long catalogCodigoSnapshot,
  String catalogNomeSnapshot,
  UUID tenantUnitId,
  String tenantUnitSigla,
  UUID unidadeBaseCatalogoTenantUnitId,
  String unidadeBaseCatalogoSigla,
  BigDecimal quantidade,
  BigDecimal quantidadeConvertidaBase,
  BigDecimal fatorAplicado,
  ConversionFactorSource fatorFonte,
  BigDecimal valorUnitario,
  BigDecimal valorTotal,
  boolean cobrar,
  boolean estoqueMovimentado,
  Long estoqueMovimentacaoId,
  boolean finalizado,
  String status,
  Integer ordem,
  String observacao
) {}
