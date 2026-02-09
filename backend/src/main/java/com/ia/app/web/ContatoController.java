package com.ia.app.web;

import com.ia.app.dto.ContatoRequest;
import com.ia.app.dto.ContatoResponse;
import com.ia.app.mapper.ContatoMapper;
import com.ia.app.service.ContatoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contatos")
public class ContatoController {

  private final ContatoService service;

  public ContatoController(ContatoService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<List<ContatoResponse>> list(@RequestParam Long entidadeRegistroId) {
    return ResponseEntity.ok(service.list(entidadeRegistroId).stream().map(ContatoMapper::toResponse).toList());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoResponse> create(@Valid @RequestBody ContatoRequest request) {
    return ResponseEntity.ok(ContatoMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoResponse> update(@PathVariable Long id, @Valid @RequestBody ContatoRequest request) {
    return ResponseEntity.ok(ContatoMapper.toResponse(service.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
