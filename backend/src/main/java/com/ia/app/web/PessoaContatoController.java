package com.ia.app.web;

import com.ia.app.dto.PessoaContatoRequest;
import com.ia.app.dto.PessoaContatoResponse;
import com.ia.app.mapper.PessoaContatoMapper;
import com.ia.app.service.PessoaContatoService;
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
@RequestMapping("/api/pessoas")
public class PessoaContatoController {

  private final PessoaContatoService service;

  public PessoaContatoController(PessoaContatoService service) {
    this.service = service;
  }

  @GetMapping("/{pessoaId}/contatos")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<List<PessoaContatoResponse>> list(@PathVariable Long pessoaId) {
    return ResponseEntity.ok(service.list(pessoaId).stream().map(PessoaContatoMapper::toResponse).toList());
  }

  @PostMapping("/{pessoaId}/contatos")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<PessoaContatoResponse> create(@PathVariable Long pessoaId,
      @Valid @RequestBody PessoaContatoRequest request) {
    return ResponseEntity.ok(PessoaContatoMapper.toResponse(service.create(pessoaId, request)));
  }

  @PutMapping("/contatos/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<PessoaContatoResponse> update(@PathVariable Long id,
      @Valid @RequestBody PessoaContatoRequest request) {
    return ResponseEntity.ok(PessoaContatoMapper.toResponse(service.update(id, request)));
  }

  @DeleteMapping("/contatos/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
