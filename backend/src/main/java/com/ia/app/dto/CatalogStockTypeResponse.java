package com.ia.app.dto;

public record CatalogStockTypeResponse(
  Long id,
  String codigo,
  String nome,
  Integer ordem,
  boolean active,
  Long version
) {}
