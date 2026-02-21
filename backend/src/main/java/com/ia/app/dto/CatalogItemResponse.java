package com.ia.app.dto;

import com.ia.app.domain.CatalogConfigurationType;
import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemResponse(
  Long id,
  CatalogConfigurationType type,
  Long catalogConfigurationId,
  Long agrupadorEmpresaId,
  String agrupadorEmpresaNome,
  Long catalogGroupId,
  String catalogGroupNome,
  Long codigo,
  String nome,
  String descricao,
  UUID tenantUnitId,
  String tenantUnitSigla,
  String tenantUnitNome,
  UUID unidadeAlternativaTenantUnitId,
  String unidadeAlternativaSigla,
  String unidadeAlternativaNome,
  BigDecimal fatorConversaoAlternativa,
  boolean hasStockMovements,
  boolean ativo
) {}
