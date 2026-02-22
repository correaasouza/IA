package com.ia.app.web;

import com.ia.app.dto.PriceVariantRequest;
import com.ia.app.dto.PriceVariantResponse;
import com.ia.app.service.PriceVariantService;
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
@RequestMapping("/api/catalog/pricing/variants")
public class PriceVariantController {

  private final PriceVariantService service;

  public PriceVariantController(PriceVariantService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<List<PriceVariantResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<PriceVariantResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<PriceVariantResponse> create(@Valid @RequestBody PriceVariantRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<PriceVariantResponse> update(@PathVariable Long id, @Valid @RequestBody PriceVariantRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
