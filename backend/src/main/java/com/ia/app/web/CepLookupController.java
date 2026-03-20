package com.ia.app.web;

import com.ia.app.dto.CepLookupResponse;
import com.ia.app.service.CepLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ceps")
public class CepLookupController {

  private final CepLookupService service;

  public CepLookupController(CepLookupService service) {
    this.service = service;
  }

  @GetMapping("/{cep}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<CepLookupResponse> lookup(@PathVariable String cep) {
    return ResponseEntity.ok(service.lookup(cep));
  }
}
