package com.ia.app.web;

import com.ia.app.dto.PasswordResetRequest;
import com.ia.app.dto.UsuarioPapelRequest;
import com.ia.app.dto.UsuarioPapelResponse;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.dto.UsuarioResponse;
import com.ia.app.dto.UsuarioUpdateRequest;
import com.ia.app.service.UsuarioPapelService;
import com.ia.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

  private final UsuarioService service;
  private final UsuarioPapelService usuarioPapelService;

  public UsuarioController(UsuarioService service, UsuarioPapelService usuarioPapelService) {
    this.service = service;
    this.usuarioPapelService = usuarioPapelService;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public Page<UsuarioResponse> list(Pageable pageable) {
    return service.findAllWithPapeis(pageable);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<UsuarioResponse> get(@PathVariable Long id) {
    var u = service.getById(id);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
    var u = service.create(request);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<UsuarioResponse> update(@PathVariable Long id,
      @Valid @RequestBody UsuarioUpdateRequest request) {
    var u = service.update(id, request);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PatchMapping("/{id}/disable")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<UsuarioResponse> disable(@PathVariable Long id) {
    var u = service.disable(id);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/reset-password")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public ResponseEntity<Void> resetPassword(@PathVariable Long id,
      @Valid @RequestBody PasswordResetRequest request) {
    service.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/papeis")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public UsuarioPapelResponse listPapeis(@PathVariable Long id) {
    return usuarioPapelService.listByUsuario(id);
  }

  @PostMapping("/{id}/papeis")
  @PreAuthorize("@permissaoGuard.hasPermissao('USUARIO_MANAGE')")
  public UsuarioPapelResponse setPapeis(@PathVariable Long id,
      @Valid @RequestBody UsuarioPapelRequest request) {
    return usuarioPapelService.setByUsuario(id, request.papelIds());
  }
}
