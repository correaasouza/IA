package com.ia.app.web;

import com.ia.app.dto.RegistroEntidadeEmpresaContextoResponse;
import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.dto.RegistroEntidadeResponse;
import com.ia.app.service.RegistroEntidadeContextoService;
import com.ia.app.service.RegistroEntidadeService;
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
@RequestMapping("/api/tipos-entidade/{tipoEntidadeId}/entidades")
public class RegistroEntidadeController {

  private final RegistroEntidadeService service;
  private final RegistroEntidadeContextoService contextoService;

  public RegistroEntidadeController(
      RegistroEntidadeService service,
      RegistroEntidadeContextoService contextoService) {
    this.service = service;
    this.contextoService = contextoService;
  }

  @GetMapping("/contexto-empresa")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeEmpresaContextoResponse> contextoEmpresa(
      @PathVariable Long tipoEntidadeId) {
    return ResponseEntity.ok(contextoService.contexto(tipoEntidadeId));
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Page<RegistroEntidadeResponse>> list(
      @PathVariable Long tipoEntidadeId,
      @RequestParam(required = false) Long codigo,
      @RequestParam(required = false) String pessoaNome,
      @RequestParam(required = false) String registroFederal,
      @RequestParam(required = false) Long grupoId,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    return ResponseEntity.ok(service.list(tipoEntidadeId, codigo, pessoaNome, registroFederal, grupoId, ativo, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> get(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(service.get(tipoEntidadeId, id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> create(
      @PathVariable Long tipoEntidadeId,
      @Valid @RequestBody RegistroEntidadeRequest request) {
    return ResponseEntity.ok(service.create(tipoEntidadeId, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> update(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @Valid @RequestBody RegistroEntidadeRequest request) {
    return ResponseEntity.ok(service.update(tipoEntidadeId, id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> delete(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    service.delete(tipoEntidadeId, id);
    return ResponseEntity.noContent().build();
  }
}
