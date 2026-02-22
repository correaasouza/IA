package com.ia.app.dto;

public record PriceBookResponse(
  Long id,
  String name,
  boolean active,
  boolean defaultBook
) {}
