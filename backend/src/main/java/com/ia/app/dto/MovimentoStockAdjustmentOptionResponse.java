package com.ia.app.dto;

public record MovimentoStockAdjustmentOptionResponse(
  Long id,
  String codigo,
  String nome,
  String adjustmentType,
  String catalogType
) {}
