package com.ia.app.web;

import com.ia.app.dto.ContatoTipoPorEntidadeRequest;
import com.ia.app.dto.ContatoTipoPorEntidadeResponse;
import com.ia.app.mapper.ContatoTipoPorEntidadeMapper;
import com.ia.app.service.ContatoTipoPorEntidadeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contatos/tipos-por-entidade")
public class ContatoTipoPorEntidadeController {

  private final ContatoTipoPorEntidadeService service;

  public ContatoTipoPorEntidadeController(ContatoTipoPorEntidadeService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<List<ContatoTipoPorEntidadeResponse>> list(@RequestParam Long entidadeDefinicaoId) {
    return ResponseEntity.ok(service.list(entidadeDefinicaoId).stream().map(ContatoTipoPorEntidadeMapper::toResponse).toList());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoTipoPorEntidadeResponse> create(@Valid @RequestBody ContatoTipoPorEntidadeRequest request) {
    return ResponseEntity.ok(ContatoTipoPorEntidadeMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<ContatoTipoPorEntidadeResponse> update(@PathVariable Long id,
      @Valid @RequestBody ContatoTipoPorEntidadeRequest request) {
    return ResponseEntity.ok(ContatoTipoPorEntidadeMapper.toResponse(service.update(id, request)));
  }
}
