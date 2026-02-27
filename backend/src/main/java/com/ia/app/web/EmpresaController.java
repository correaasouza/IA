package com.ia.app.web;

import com.ia.app.dto.EmpresaFilialRequest;
import com.ia.app.dto.EmpresaMatrizRequest;
import com.ia.app.dto.EmpresaResponse;
import com.ia.app.dto.EmpresaUpdateRequest;
import com.ia.app.mapper.EmpresaMapper;
import com.ia.app.service.EmpresaService;
import com.ia.app.service.UsuarioEmpresaPreferenciaService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
@RequestMapping("/api/empresas")
public class EmpresaController {
  private final EmpresaService service;
  private final UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService;

  public EmpresaController(
      EmpresaService service,
      UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService) {
    this.service = service;
    this.usuarioEmpresaPreferenciaService = usuarioEmpresaPreferenciaService;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW') or @permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Page<EmpresaResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String cnpj,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) Long matrizId,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    String usuarioId = resolveUsuarioId(authentication);
    Long empresaPadraoId = usuarioEmpresaPreferenciaService.getEmpresaPadraoId(usuarioId);
    Page<EmpresaResponse> page = service.findAll(nome, cnpj, tipo, matrizId, ativo, pageable)
      .map(e -> EmpresaMapper.toResponse(e, empresaPadraoId != null && empresaPadraoId.equals(e.getId())));
    return ResponseEntity.ok()
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(page);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW') or @permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EmpresaResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(EmpresaMapper.toResponse(service.getById(id)));
  }

  @GetMapping("/{id}/filiais")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW') or @permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<java.util.List<EmpresaResponse>> listFiliais(@PathVariable Long id) {
    return ResponseEntity.ok(service.listFiliais(id).stream().map(EmpresaMapper::toResponse).toList());
  }

  @PostMapping("/matrizes")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EmpresaResponse> createMatriz(@Valid @RequestBody EmpresaMatrizRequest request) {
    return ResponseEntity.ok(EmpresaMapper.toResponse(service.createMatriz(request)));
  }

  @PostMapping("/filiais")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EmpresaResponse> createFilial(@Valid @RequestBody EmpresaFilialRequest request) {
    return ResponseEntity.ok(EmpresaMapper.toResponse(service.createFilial(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EmpresaResponse> update(@PathVariable Long id,
      @Valid @RequestBody EmpresaUpdateRequest request) {
    return ResponseEntity.ok(EmpresaMapper.toResponse(service.update(id, request)));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EmpresaResponse> updateStatus(@PathVariable Long id,
      @RequestBody Map<String, Boolean> body) {
    boolean ativo = body.getOrDefault("ativo", Boolean.TRUE);
    return ResponseEntity.ok(EmpresaMapper.toResponse(service.updateStatus(id, ativo)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  private String resolveUsuarioId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getSubject();
    }
    throw new IllegalStateException("unauthorized");
  }
}
