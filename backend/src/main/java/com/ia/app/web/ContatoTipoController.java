package com.ia.app.web;

import com.ia.app.dto.ContatoTipoRequest;
import com.ia.app.dto.ContatoTipoResponse;
import com.ia.app.mapper.ContatoTipoMapper;
import com.ia.app.service.ContatoTipoService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/contatos/tipos")
public class ContatoTipoController {

  private final ContatoTipoService service;

  public ContatoTipoController(ContatoTipoService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<List<ContatoTipoResponse>> list() {
    return ResponseEntity.ok(service.list().stream().map(ContatoTipoMapper::toResponse).toList());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoTipoResponse> create(@Valid @RequestBody ContatoTipoRequest request) {
    return ResponseEntity.ok(ContatoTipoMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoTipoResponse> update(@PathVariable Long id,
      @Valid @RequestBody ContatoTipoRequest request) {
    return ResponseEntity.ok(ContatoTipoMapper.toResponse(service.update(id, request)));
  }
}
