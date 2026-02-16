package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogConfigurationByGroupResponse;
import com.ia.app.dto.CatalogConfigurationResponse;
import com.ia.app.dto.CatalogConfigurationUpdateRequest;
import com.ia.app.service.CatalogConfigurationByGroupService;
import com.ia.app.service.CatalogConfigurationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  public CatalogConfigurationController(
      CatalogConfigurationService service,
      CatalogConfigurationByGroupService byGroupService) {
    this.service = service;
    this.byGroupService = byGroupService;
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
}
