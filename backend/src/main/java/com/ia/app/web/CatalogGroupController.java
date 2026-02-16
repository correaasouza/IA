package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogGroupRequest;
import com.ia.app.dto.CatalogGroupResponse;
import com.ia.app.dto.CatalogGroupUpdateRequest;
import com.ia.app.service.CatalogGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/{type}/groups")
public class CatalogGroupController {

  private final CatalogGroupService service;

  public CatalogGroupController(CatalogGroupService service) {
    this.service = service;
  }

  @GetMapping("/tree")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<CatalogGroupResponse>> tree(@PathVariable String type) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(service.tree(parsedType));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogGroupResponse> create(
      @PathVariable String type,
      @Valid @RequestBody CatalogGroupRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(service.create(parsedType, request));
  }

  @PutMapping("/{groupId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogGroupResponse> update(
      @PathVariable String type,
      @PathVariable Long groupId,
      @Valid @RequestBody CatalogGroupUpdateRequest request) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(service.update(parsedType, groupId, request));
  }

  @DeleteMapping("/{groupId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> delete(
      @PathVariable String type,
      @PathVariable Long groupId) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    service.delete(parsedType, groupId);
    return ResponseEntity.noContent().build();
  }
}
