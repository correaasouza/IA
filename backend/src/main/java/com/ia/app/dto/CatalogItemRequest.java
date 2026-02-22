package com.ia.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CatalogItemRequest(
  Long codigo,
  @NotBlank @Size(max = 200) String nome,
  @Size(max = 255) String descricao,
  Long catalogGroupId,
  @NotNull UUID tenantUnitId,
  UUID unidadeAlternativaTenantUnitId,
  BigDecimal fatorConversaoAlternativa,
  @NotNull Boolean ativo,
  List<@Valid CatalogItemPriceInput> prices
) {
  public CatalogItemRequest(
      Long codigo,
      String nome,
      String descricao,
      Long catalogGroupId,
      UUID tenantUnitId,
      UUID unidadeAlternativaTenantUnitId,
      BigDecimal fatorConversaoAlternativa,
      Boolean ativo) {
    this(codigo, nome, descricao, catalogGroupId, tenantUnitId, unidadeAlternativaTenantUnitId, fatorConversaoAlternativa, ativo, null);
  }
}
