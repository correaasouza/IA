package com.ia.app.web;

import com.ia.app.dto.UsuarioResponse;
import com.ia.app.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/users")
public class TenantUserController {

  private final UsuarioService usuarioService;

  public TenantUserController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @GetMapping
  @PreAuthorize("@globalScopeGuard.isGlobalMaster()")
  public Page<UsuarioResponse> listByTenant(@PathVariable Long tenantId, Pageable pageable) {
    return usuarioService.findAllWithPapeis(tenantId, pageable);
  }
}
