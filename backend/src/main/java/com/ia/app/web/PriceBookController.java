package com.ia.app.web;

import com.ia.app.dto.PriceBookRequest;
import com.ia.app.dto.PriceBookResponse;
import com.ia.app.service.PriceBookService;
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
@RequestMapping("/api/catalog/pricing/books")
public class PriceBookController {

  private final PriceBookService service;

  public PriceBookController(PriceBookService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<List<PriceBookResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_VIEW')")
  public ResponseEntity<PriceBookResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<PriceBookResponse> create(@Valid @RequestBody PriceBookRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<PriceBookResponse> update(@PathVariable Long id, @Valid @RequestBody PriceBookRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CATALOG_PRICES_MANAGE')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
