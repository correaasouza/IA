package com.ia.app.web;

import com.ia.app.dto.GrupoEntidadeRequest;
import com.ia.app.dto.GrupoEntidadeResponse;
import com.ia.app.dto.GrupoEntidadeUpdateRequest;
import com.ia.app.service.GrupoEntidadeService;
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
@RequestMapping("/api/tipos-entidade/{tipoEntidadeId}/grupos-entidade")
public class GrupoEntidadeController {

  private final GrupoEntidadeService service;

  public GrupoEntidadeController(GrupoEntidadeService service) {
    this.service = service;
  }

  @GetMapping("/tree")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<GrupoEntidadeResponse>> tree(@PathVariable Long tipoEntidadeId) {
    return ResponseEntity.ok(service.tree(tipoEntidadeId));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<GrupoEntidadeResponse> create(
      @PathVariable Long tipoEntidadeId,
      @Valid @RequestBody GrupoEntidadeRequest request) {
    return ResponseEntity.ok(service.create(tipoEntidadeId, request));
  }

  @PutMapping("/{grupoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<GrupoEntidadeResponse> update(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long grupoId,
      @Valid @RequestBody GrupoEntidadeUpdateRequest request) {
    return ResponseEntity.ok(service.update(tipoEntidadeId, grupoId, request));
  }

  @DeleteMapping("/{grupoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> delete(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long grupoId) {
    service.delete(tipoEntidadeId, grupoId);
    return ResponseEntity.noContent().build();
  }
}
