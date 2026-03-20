package com.ia.app.web;

import com.ia.app.dto.PasswordResetRequest;
import com.ia.app.dto.EmpresaResponse;
import com.ia.app.dto.GrantCompanyAccessRequest;
import com.ia.app.dto.GrantTenantAccessRequest;
import com.ia.app.dto.SetDefaultCompanyRequest;
import com.ia.app.dto.UserCompanyAccessSummaryResponse;
import com.ia.app.dto.UsuarioEmpresaAcessoRequest;
import com.ia.app.dto.UsuarioEmpresaAcessoResponse;
import com.ia.app.dto.UsuarioEmpresaPadraoRequest;
import com.ia.app.dto.UsuarioEmpresaPadraoResponse;
import com.ia.app.dto.PapelResponse;
import com.ia.app.dto.UsuarioLocatarioAcessoRequest;
import com.ia.app.dto.UsuarioLocatarioAcessoResponse;
import com.ia.app.dto.UsuarioPapelRequest;
import com.ia.app.dto.UsuarioPapelResponse;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.dto.UsuarioResponse;
import com.ia.app.dto.UsuarioUpdateRequest;
import com.ia.app.service.UsuarioLocatarioAcessoService;
import com.ia.app.service.UsuarioEmpresaAcessoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

  private final UsuarioService service;
  private final UsuarioPapelService usuarioPapelService;
  private final UsuarioLocatarioAcessoService usuarioLocatarioAcessoService;
  private final UsuarioEmpresaAcessoService usuarioEmpresaAcessoService;

  public UsuarioController(UsuarioService service,
      UsuarioPapelService usuarioPapelService,
      UsuarioLocatarioAcessoService usuarioLocatarioAcessoService,
      UsuarioEmpresaAcessoService usuarioEmpresaAcessoService) {
    this.service = service;
    this.usuarioPapelService = usuarioPapelService;
    this.usuarioLocatarioAcessoService = usuarioLocatarioAcessoService;
    this.usuarioEmpresaAcessoService = usuarioEmpresaAcessoService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public Page<UsuarioResponse> list(@RequestParam(required = false) Long tenantId, Pageable pageable) {
    return service.findAllWithPapeis(tenantId, pageable);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<UsuarioResponse> get(@PathVariable Long id) {
    var u = service.getById(id);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PostMapping
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
    var u = service.create(request);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<UsuarioResponse> update(@PathVariable Long id,
      @Valid @RequestBody UsuarioUpdateRequest request) {
    var u = service.update(id, request);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @PatchMapping("/{id}/disable")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<UsuarioResponse> disable(@PathVariable Long id) {
    var u = service.disable(id);
    return ResponseEntity.ok(new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.isAtivo(), java.util.List.of()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/reset-password")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public ResponseEntity<Void> resetPassword(@PathVariable Long id,
      @Valid @RequestBody PasswordResetRequest request) {
    service.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/papeis")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioPapelResponse listPapeis(@PathVariable Long id) {
    return usuarioPapelService.listByUsuario(id);
  }

  @GetMapping("/{id}/papeis-disponiveis")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public java.util.List<PapelResponse> listPapeisDisponiveis(@PathVariable Long id) {
    return usuarioPapelService.listPapeisDisponiveisByUsuario(id).stream()
      .map(p -> new PapelResponse(p.getId(), p.getNome(), p.getDescricao(), p.isAtivo()))
      .toList();
  }

  @PostMapping("/{id}/papeis")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioPapelResponse setPapeis(@PathVariable Long id,
      @Valid @RequestBody UsuarioPapelRequest request) {
    return usuarioPapelService.setByUsuario(id, request.papelIds());
  }

  @GetMapping("/{id}/locatarios")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioLocatarioAcessoResponse listLocatarios(@PathVariable Long id) {
    return usuarioLocatarioAcessoService.listByUsuario(id);
  }

  @PostMapping("/{id}/locatarios")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioLocatarioAcessoResponse setLocatarios(@PathVariable Long id,
      @Valid @RequestBody UsuarioLocatarioAcessoRequest request) {
    return usuarioLocatarioAcessoService.setByUsuario(id, request.locatarioIds());
  }

  @GetMapping("/{id}/empresas-acesso")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioEmpresaAcessoResponse listEmpresasAcesso(@PathVariable Long id) {
    return usuarioEmpresaAcessoService.listByUsuario(id);
  }

  @PostMapping("/{id}/empresas-acesso")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioEmpresaAcessoResponse setEmpresasAcesso(@PathVariable Long id,
      @Valid @RequestBody UsuarioEmpresaAcessoRequest request) {
    return usuarioEmpresaAcessoService.setByUsuario(id, request.empresaIds());
  }

  @GetMapping("/{id}/empresas-disponiveis")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public java.util.List<EmpresaResponse> listEmpresasDisponiveis(@PathVariable Long id) {
    return usuarioEmpresaAcessoService.listDisponiveis(id);
  }

  @GetMapping("/{id}/empresa-padrao")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioEmpresaPadraoResponse getEmpresaPadrao(@PathVariable Long id) {
    Long empresaId = usuarioEmpresaAcessoService.getEmpresaPadraoByUsuario(id);
    return new UsuarioEmpresaPadraoResponse(empresaId);
  }

  @PutMapping("/{id}/empresa-padrao")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioEmpresaPadraoResponse setEmpresaPadrao(@PathVariable Long id,
      @Valid @RequestBody UsuarioEmpresaPadraoRequest request) {
    Long empresaId = usuarioEmpresaAcessoService.setEmpresaPadraoByUsuario(id, request.empresaId());
    return new UsuarioEmpresaPadraoResponse(empresaId);
  }

  @GetMapping("/{id}/companies-summary")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UserCompanyAccessSummaryResponse companySummary(@PathVariable Long id) {
    return usuarioEmpresaAcessoService.summaryByUsuario(id);
  }

  @PostMapping("/grant-tenant")
  @PreAuthorize("@globalScopeGuard.isGlobalMaster()")
  public UsuarioLocatarioAcessoResponse grantTenant(@Valid @RequestBody GrantTenantAccessRequest request) {
    return usuarioLocatarioAcessoService.grantByUsuario(request.userId(), request.tenantId());
  }

  @PostMapping("/grant-company")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UserCompanyAccessSummaryResponse grantCompany(@Valid @RequestBody GrantCompanyAccessRequest request) {
    return usuarioEmpresaAcessoService.grantCompanyAccess(request.userId(), request.companyId());
  }

  @PutMapping("/{id}/default-company")
  @PreAuthorize("@authorizationService.canManageUsersInCurrentTenant()")
  public UsuarioEmpresaPadraoResponse setDefaultCompany(@PathVariable Long id,
      @Valid @RequestBody SetDefaultCompanyRequest request) {
    Long empresaId = usuarioEmpresaAcessoService.setEmpresaPadraoByUsuario(id, request.companyId());
    return new UsuarioEmpresaPadraoResponse(empresaId);
  }
}
