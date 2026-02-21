package com.ia.app.web;

import com.ia.app.dto.TenantUnitRequest;
import com.ia.app.dto.TenantUnitResponse;
import com.ia.app.service.TenantUnitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/tenant/units")
public class TenantUnitController {

  private final TenantUnitService service;

  public TenantUnitController(TenantUnitService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<TenantUnitResponse>> list(@RequestParam(required = false) String text) {
    return ResponseEntity.ok(service.list(text));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitResponse> create(@Valid @RequestBody TenantUnitRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody TenantUnitRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reconcile")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitService.ReconcileResult> reconcile() {
    return ResponseEntity.ok(service.reconcileCurrentTenant());
  }
}
