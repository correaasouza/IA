package com.ia.app.web;

import com.ia.app.dto.PermissaoCatalogResponse;
import com.ia.app.dto.PermissaoRequest;
import com.ia.app.service.PermissaoCatalogService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissoes")
public class PermissaoController {

  private final PermissaoCatalogService service;

  public PermissaoController(PermissaoCatalogService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public List<PermissaoCatalogResponse> list() {
    return service.listAll();
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public ResponseEntity<PermissaoCatalogResponse> create(@Valid @RequestBody PermissaoRequest request) {
    var p = service.create(request);
    return ResponseEntity.ok(new PermissaoCatalogResponse(p.getId(), p.getCodigo(), p.getLabel()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public ResponseEntity<PermissaoCatalogResponse> update(@PathVariable Long id, @Valid @RequestBody PermissaoRequest request) {
    var p = service.update(id, request);
    return ResponseEntity.ok(new PermissaoCatalogResponse(p.getId(), p.getCodigo(), p.getLabel()));
  }
}
