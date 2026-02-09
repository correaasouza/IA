package com.ia.app.web;

import com.ia.app.dto.EntidadeDefinicaoRequest;
import com.ia.app.dto.EntidadeDefinicaoResponse;
import com.ia.app.mapper.EntidadeDefinicaoMapper;
import com.ia.app.service.EntidadeDefinicaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entidades-definicao")
public class EntidadeDefinicaoController {

  private final EntidadeDefinicaoService service;

  public EntidadeDefinicaoController(EntidadeDefinicaoService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public Page<EntidadeDefinicaoResponse> list(Pageable pageable) {
    return service.list(pageable).map(EntidadeDefinicaoMapper::toResponse);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeDefinicaoResponse> create(@Valid @RequestBody EntidadeDefinicaoRequest request) {
    return ResponseEntity.ok(EntidadeDefinicaoMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeDefinicaoResponse> update(@PathVariable Long id,
      @Valid @RequestBody EntidadeDefinicaoRequest request) {
    return ResponseEntity.ok(EntidadeDefinicaoMapper.toResponse(service.update(id, request)));
  }
}
