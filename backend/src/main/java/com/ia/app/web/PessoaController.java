package com.ia.app.web;

import com.ia.app.dto.PessoaRequest;
import com.ia.app.dto.PessoaResponse;
import com.ia.app.mapper.PessoaMapper;
import com.ia.app.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

  private final PessoaService service;

  public PessoaController(PessoaService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public Page<PessoaResponse> list(Pageable pageable) {
    return service.list(pageable).map(PessoaMapper::toResponse);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<PessoaResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(PessoaMapper.toResponse(service.get(id)));
  }

  @GetMapping("/busca")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<PessoaResponse> findByDocumento(@RequestParam String documento) {
    return ResponseEntity.ok(PessoaMapper.toResponse(service.findByDocumento(documento)));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<PessoaResponse> create(@Valid @RequestBody PessoaRequest request) {
    return ResponseEntity.ok(PessoaMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<PessoaResponse> update(@PathVariable Long id,
      @Valid @RequestBody PessoaRequest request) {
    return ResponseEntity.ok(PessoaMapper.toResponse(service.update(id, request)));
  }
}
