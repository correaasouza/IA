package com.ia.app.web;

import com.ia.app.dto.CatalogGroupTreeNodeDTO;
import com.ia.app.service.CatalogItemSearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog-groups")
public class CatalogGroupTreeController {

  private final CatalogItemSearchService service;

  public CatalogGroupTreeController(CatalogItemSearchService service) {
    this.service = service;
  }

  @GetMapping("/tree")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_ITEM_OPERAR')")
  public ResponseEntity<List<CatalogGroupTreeNodeDTO>> tree(
      @RequestParam String movementType,
      @RequestParam Long movementConfigId,
      @RequestParam Long movementItemTypeId,
      @RequestParam(required = false) Long parentId) {
    return ResponseEntity.ok(service.tree(
      movementType,
      movementConfigId,
      movementItemTypeId,
      parentId));
  }
}