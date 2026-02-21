package com.ia.app.dto;

public record CatalogGroupTreeNodeDTO(
  Long id,
  String nome,
  Long parentId,
  Integer nivel,
  String path,
  Boolean hasChildren,
  String breadcrumb
) {}