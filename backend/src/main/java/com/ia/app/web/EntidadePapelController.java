package com.ia.app.web;

import com.ia.app.dto.EntidadeRequest;
import com.ia.app.dto.EntidadeResponse;
import com.ia.app.mapper.EntidadeMapper;
import com.ia.app.service.EntidadeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/entidades-papel")
public class EntidadePapelController {

  private final EntidadeService service;

  public EntidadePapelController(EntidadeService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public Page<EntidadeResponse> list(
      @RequestParam(required = false) Long tipoEntidadeId,
      Pageable pageable) {
    return service.list(tipoEntidadeId, pageable).map(EntidadeMapper::toResponse);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<EntidadeResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(EntidadeMapper.toResponse(service.get(id)));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeResponse> create(@Valid @RequestBody EntidadeRequest request) {
    return ResponseEntity.ok(EntidadeMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeResponse> update(@PathVariable Long id,
      @Valid @RequestBody EntidadeRequest request) {
    return ResponseEntity.ok(EntidadeMapper.toResponse(service.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
