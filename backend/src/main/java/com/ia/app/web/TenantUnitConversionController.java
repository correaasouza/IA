package com.ia.app.web;

import com.ia.app.dto.TenantUnitConversionRequest;
import com.ia.app.dto.TenantUnitConversionResponse;
import com.ia.app.service.TenantUnitConversionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/unit-conversions")
public class TenantUnitConversionController {

  private final TenantUnitConversionService service;

  public TenantUnitConversionController(TenantUnitConversionService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<TenantUnitConversionResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitConversionResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitConversionResponse> create(@Valid @RequestBody TenantUnitConversionRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<TenantUnitConversionResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody TenantUnitConversionRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
