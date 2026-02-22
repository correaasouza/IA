package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogItemContextResponse;
import com.ia.app.dto.CatalogItemPricePreviewRequest;
import com.ia.app.dto.CatalogItemPriceResponse;
import com.ia.app.dto.CatalogItemRequest;
import com.ia.app.dto.CatalogItemResponse;
import com.ia.app.service.CatalogItemContextService;
import com.ia.app.service.CatalogProductService;
import com.ia.app.service.CatalogServiceCrudService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/{type}")
public class CatalogItemController {

  private final CatalogItemContextService contextService;
  private final CatalogProductService productService;
  private final CatalogServiceCrudService serviceCrudService;

  public CatalogItemController(
      CatalogItemContextService contextService,
      CatalogProductService productService,
      CatalogServiceCrudService serviceCrudService) {
    this.contextService = contextService;
    this.productService = productService;
    this.serviceCrudService = serviceCrudService;
  }

  @GetMapping("/contexto-empresa")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogItemContextResponse> contextoEmpresa(@PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(contextService.contexto(parsedType));
  }

  @GetMapping("/items")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Page<CatalogItemResponse>> list(
      @PathVariable String type,
      @RequestParam(required = false) Long codigo,
      @RequestParam(required = false) String text,
      @RequestParam(required = false) Long grupoId,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(switch (parsedType) {
      case PRODUCTS -> productService.list(codigo, text, grupoId, ativo, pageable);
      case SERVICES -> serviceCrudService.list(codigo, text, grupoId, ativo, pageable);
    });
  }

  @GetMapping("/items/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogItemResponse> get(
      @PathVariable String type,
      @PathVariable Long id) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(switch (parsedType) {
      case PRODUCTS -> productService.get(id);
      case SERVICES -> serviceCrudService.get(id);
    });
  }

  @PostMapping("/items/prices/preview")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogItemPriceResponse>> previewPrices(
      @PathVariable String type,
      @Valid @RequestBody CatalogItemPricePreviewRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(switch (parsedType) {
      case PRODUCTS -> productService.previewPrices(request);
      case SERVICES -> serviceCrudService.previewPrices(request);
    });
  }

  @PostMapping("/items")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogItemResponse> create(
      @PathVariable String type,
      @Valid @RequestBody CatalogItemRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(switch (parsedType) {
      case PRODUCTS -> productService.create(request);
      case SERVICES -> serviceCrudService.create(request);
    });
  }

  @PutMapping("/items/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogItemResponse> update(
      @PathVariable String type,
      @PathVariable Long id,
      @Valid @RequestBody CatalogItemRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(switch (parsedType) {
      case PRODUCTS -> productService.update(id, request);
      case SERVICES -> serviceCrudService.update(id, request);
    });
  }

  @DeleteMapping("/items/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> delete(
      @PathVariable String type,
      @PathVariable Long id) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    if (parsedType == CatalogConfigurationType.PRODUCTS) {
      productService.delete(id);
    } else {
      serviceCrudService.delete(id);
    }
    return ResponseEntity.noContent().build();
  }
}
