package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogConfigurationByGroupResponse;
import com.ia.app.dto.CatalogConfigurationResponse;
import com.ia.app.dto.CatalogStockAdjustmentResponse;
import com.ia.app.dto.CatalogStockAdjustmentScopeOptionResponse;
import com.ia.app.dto.CatalogStockAdjustmentUpsertRequest;
import com.ia.app.dto.CatalogStockTypeResponse;
import com.ia.app.dto.CatalogStockTypeUpsertRequest;
import com.ia.app.dto.CatalogConfigurationUpdateRequest;
import com.ia.app.service.CatalogConfigurationByGroupService;
import com.ia.app.service.CatalogConfigurationService;
import com.ia.app.service.CatalogStockAdjustmentConfigurationService;
import com.ia.app.service.CatalogStockTypeConfigurationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/configuration")
public class CatalogConfigurationController {

  private final CatalogConfigurationService service;
  private final CatalogConfigurationByGroupService byGroupService;
  private final CatalogStockTypeConfigurationService stockTypeConfigurationService;
  private final CatalogStockAdjustmentConfigurationService stockAdjustmentConfigurationService;

  public CatalogConfigurationController(
      CatalogConfigurationService service,
      CatalogConfigurationByGroupService byGroupService,
      CatalogStockTypeConfigurationService stockTypeConfigurationService,
      CatalogStockAdjustmentConfigurationService stockAdjustmentConfigurationService) {
    this.service = service;
    this.byGroupService = byGroupService;
    this.stockTypeConfigurationService = stockTypeConfigurationService;
    this.stockAdjustmentConfigurationService = stockAdjustmentConfigurationService;
  }

  @GetMapping("/{type}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogConfigurationResponse> get(@PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(service.getOrCreate(parsedType));
  }

  @PutMapping("/{type}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogConfigurationResponse> update(
      @PathVariable String type,
      @Valid @RequestBody CatalogConfigurationUpdateRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(service.update(parsedType, request.numberingMode()));
  }

  @GetMapping("/{type}/group-config")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogConfigurationByGroupResponse>> listByGroup(@PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(byGroupService.list(parsedType));
  }

  @PutMapping("/{type}/group-config/{agrupadorId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogConfigurationByGroupResponse> updateByGroup(
      @PathVariable String type,
      @PathVariable Long agrupadorId,
      @Valid @RequestBody CatalogConfigurationUpdateRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(byGroupService.update(parsedType, agrupadorId, request.numberingMode()));
  }

  @GetMapping("/{type}/group-config/{agrupadorId}/stock-types")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogStockTypeResponse>> listStockTypesByGroup(
      @PathVariable String type,
      @PathVariable Long agrupadorId) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockTypeConfigurationService.listByGroup(parsedType, agrupadorId));
  }

  @PutMapping("/{type}/group-config/{agrupadorId}/stock-types")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogStockTypeResponse> createStockTypeByGroup(
      @PathVariable String type,
      @PathVariable Long agrupadorId,
      @Valid @RequestBody CatalogStockTypeUpsertRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockTypeConfigurationService.createByGroup(parsedType, agrupadorId, request));
  }

  @PutMapping("/{type}/group-config/{agrupadorId}/stock-types/{stockTypeId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogStockTypeResponse> updateStockTypeByGroup(
      @PathVariable String type,
      @PathVariable Long agrupadorId,
      @PathVariable Long stockTypeId,
      @Valid @RequestBody CatalogStockTypeUpsertRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockTypeConfigurationService.updateByGroup(parsedType, agrupadorId, stockTypeId, request));
  }

  @GetMapping("/{type}/stock-adjustments")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogStockAdjustmentResponse>> listStockAdjustmentsByType(
      @PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockAdjustmentConfigurationService.listByType(parsedType));
  }

  @GetMapping("/{type}/stock-adjustments/options")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogStockAdjustmentScopeOptionResponse>> listStockAdjustmentScopeOptionsByType(
      @PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockAdjustmentConfigurationService.listScopeOptions(parsedType));
  }

  @PutMapping("/{type}/stock-adjustments")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogStockAdjustmentResponse> createStockAdjustmentByType(
      @PathVariable String type,
      @Valid @RequestBody CatalogStockAdjustmentUpsertRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockAdjustmentConfigurationService.createByType(parsedType, request));
  }

  @PutMapping("/{type}/stock-adjustments/{adjustmentId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogStockAdjustmentResponse> updateStockAdjustmentByType(
      @PathVariable String type,
      @PathVariable Long adjustmentId,
      @Valid @RequestBody CatalogStockAdjustmentUpsertRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(stockAdjustmentConfigurationService.updateByType(parsedType, adjustmentId, request));
  }

  @DeleteMapping("/{type}/stock-adjustments/{adjustmentId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> deleteStockAdjustmentByType(
      @PathVariable String type,
      @PathVariable Long adjustmentId) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    stockAdjustmentConfigurationService.deleteByType(parsedType, adjustmentId);
    return ResponseEntity.noContent().build();
  }
}
