package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.SalePriceApplyByGroupRequest;
import com.ia.app.dto.SalePriceApplyByGroupResponse;
import com.ia.app.dto.SalePriceByItemRowResponse;
import com.ia.app.dto.SalePriceBulkUpsertRequest;
import com.ia.app.dto.SalePriceGridRowResponse;
import com.ia.app.dto.SalePriceGroupOptionResponse;
import com.ia.app.dto.SalePriceResolveRequest;
import com.ia.app.dto.SalePriceResolveResponse;
import com.ia.app.service.SalePriceResolverService;
import com.ia.app.service.SalePriceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/catalog/pricing/sale-prices")
public class SalePriceController {

  private final SalePriceService service;
  private final SalePriceResolverService resolverService;

  public SalePriceController(
      SalePriceService service,
      SalePriceResolverService resolverService) {
    this.service = service;
    this.resolverService = resolverService;
  }

  @GetMapping("/grid")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<Page<SalePriceGridRowResponse>> grid(
      @RequestParam Long priceBookId,
      @RequestParam(required = false) Long variantId,
      @RequestParam(required = false) String catalogType,
      @RequestParam(required = false) String text,
      @RequestParam(required = false) Long catalogItemId,
      @RequestParam(required = false) Long catalogGroupId,
      @RequestParam(required = false, defaultValue = "false") Boolean includeGroupChildren,
      Pageable pageable) {
    CatalogConfigurationType parsedType = catalogType == null || catalogType.isBlank()
      ? null
      : CatalogConfigurationType.from(catalogType);
    return ResponseEntity.ok(service.grid(
      priceBookId,
      variantId,
      parsedType,
      text,
      catalogItemId,
      catalogGroupId,
      includeGroupChildren,
      pageable));
  }

  @GetMapping("/group-options")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<List<SalePriceGroupOptionResponse>> listGroupOptions(
      @RequestParam String catalogType) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(catalogType);
    return ResponseEntity.ok(service.listGroupOptions(parsedType));
  }

  @PutMapping("/bulk")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<List<SalePriceGridRowResponse>> bulkUpsert(
      @Valid @RequestBody SalePriceBulkUpsertRequest request) {
    return ResponseEntity.ok(service.bulkUpsert(request));
  }

  @PostMapping("/apply-by-group")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<SalePriceApplyByGroupResponse> applyByGroup(
      @Valid @RequestBody SalePriceApplyByGroupRequest request) {
    return ResponseEntity.ok(service.applyByGroup(request));
  }

  @GetMapping("/by-item")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<List<SalePriceByItemRowResponse>> byItem(
      @RequestParam String catalogType,
      @RequestParam Long catalogItemId,
      @RequestParam(required = false) UUID tenantUnitId) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(catalogType);
    return ResponseEntity.ok(resolverService.listByItem(parsedType, catalogItemId, tenantUnitId));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/resolve")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<SalePriceResolveResponse> resolve(
      @Valid @RequestBody SalePriceResolveRequest request) {
    return ResponseEntity.ok(resolverService.resolve(request));
  }
}
