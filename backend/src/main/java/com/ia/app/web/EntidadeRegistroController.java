package com.ia.app.web;

import com.ia.app.dto.EntidadeRegistroRequest;
import com.ia.app.dto.EntidadeRegistroResponse;
import com.ia.app.dto.EntidadeRegistroUpdateRequest;
import com.ia.app.mapper.EntidadeRegistroMapper;
import com.ia.app.service.EntidadeRegistroService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entidades")
public class EntidadeRegistroController {

  private final EntidadeRegistroService service;

  public EntidadeRegistroController(EntidadeRegistroService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public Page<EntidadeRegistroResponse> list(
      @RequestParam Long entidadeDefinicaoId,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String cpfCnpj,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    return service.list(entidadeDefinicaoId, nome, cpfCnpj, ativo, pageable)
      .map(EntidadeRegistroMapper::toResponse);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<EntidadeRegistroResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(EntidadeRegistroMapper.toResponse(service.getById(id)));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeRegistroResponse> create(@Valid @RequestBody EntidadeRegistroRequest request) {
    return ResponseEntity.ok(EntidadeRegistroMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<EntidadeRegistroResponse> update(@PathVariable Long id,
      @Valid @RequestBody EntidadeRegistroUpdateRequest request) {
    return ResponseEntity.ok(EntidadeRegistroMapper.toResponse(service.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
