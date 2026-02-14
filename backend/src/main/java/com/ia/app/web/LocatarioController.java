package com.ia.app.web;

import com.ia.app.dto.LocatarioRequest;
import com.ia.app.dto.LocatarioResponse;
import com.ia.app.mapper.LocatarioMapper;
import com.ia.app.service.LocatarioService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locatarios")
public class LocatarioController {

  private final LocatarioService service;

  public LocatarioController(LocatarioService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasRole('MASTER')")
  public Page<LocatarioResponse> list(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) Boolean bloqueado,
      Pageable pageable) {
    return service.findAll(nome, ativo, bloqueado, pageable).map(LocatarioMapper::toResponse);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<LocatarioResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(LocatarioMapper.toResponse(service.getById(id)));
  }

  @GetMapping("/allowed")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<java.util.List<LocatarioResponse>> allowed(Authentication authentication) {
    return ResponseEntity.ok(service.findAllowed(authentication));
  }

  @PostMapping
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<LocatarioResponse> create(@Valid @RequestBody LocatarioRequest request) {
    return ResponseEntity.ok(LocatarioMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<LocatarioResponse> update(@PathVariable Long id,
      @Valid @RequestBody LocatarioRequest request) {
    return ResponseEntity.ok(LocatarioMapper.toResponse(service.update(id, request)));
  }

  @PatchMapping("/{id}/data-limite")
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<LocatarioResponse> updateAccessLimit(@PathVariable Long id,
      @RequestBody Map<String, String> body) {
    LocalDate dataLimite = LocalDate.parse(body.get("dataLimiteAcesso"));
    return ResponseEntity.ok(LocatarioMapper.toResponse(service.updateAccessLimit(id, dataLimite)));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<LocatarioResponse> updateStatus(@PathVariable Long id,
      @RequestBody Map<String, Boolean> body) {
    boolean ativo = body.getOrDefault("ativo", Boolean.TRUE);
    return ResponseEntity.ok(LocatarioMapper.toResponse(service.updateStatus(id, ativo)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('MASTER')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}

