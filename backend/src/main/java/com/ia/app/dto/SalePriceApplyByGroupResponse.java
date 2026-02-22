package com.ia.app.dto;

public record SalePriceApplyByGroupResponse(
  Long catalogGroupId,
  int totalItemsInScope,
  int processedItems,
  int createdItems,
  int updatedItems,
  int skippedWithoutBasePrice,
  int skippedExisting
) {}
