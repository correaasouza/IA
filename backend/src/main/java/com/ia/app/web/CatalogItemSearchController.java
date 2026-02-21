package com.ia.app.web;

import com.ia.app.dto.CatalogItemSummaryDTO;
import com.ia.app.service.CatalogItemSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog-items")
public class CatalogItemSearchController {

  private final CatalogItemSearchService service;

  public CatalogItemSearchController(CatalogItemSearchService service) {
    this.service = service;
  }

  @GetMapping("/search")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_ITEM_OPERAR')")
  public ResponseEntity<Page<CatalogItemSummaryDTO>> search(
      @RequestParam String movementType,
      @RequestParam Long movementConfigId,
      @RequestParam Long movementItemTypeId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long groupId,
      @RequestParam(defaultValue = "true") boolean includeDescendants,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    return ResponseEntity.ok(service.search(
      movementType,
      movementConfigId,
      movementItemTypeId,
      q,
      groupId,
      includeDescendants,
      ativo,
      pageable));
  }
}