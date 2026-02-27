package com.ia.app.web;

import com.ia.app.dto.UsuarioEmpresaPadraoRequest;
import com.ia.app.dto.UsuarioEmpresaPadraoResponse;
import com.ia.app.service.MovimentoConfigFeatureToggle;
import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.service.UsuarioEmpresaPreferenciaService;
import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.service.WorkflowFeatureToggle;
import jakarta.validation.Valid;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
  private final PermissaoUsuarioService permissaoUsuarioService;
  private final UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService;
  private final MovimentoConfigFeatureToggle movimentoConfigFeatureToggle;
  private final WorkflowFeatureToggle workflowFeatureToggle;

  public MeController(
      PermissaoUsuarioService permissaoUsuarioService,
      UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService,
      MovimentoConfigFeatureToggle movimentoConfigFeatureToggle,
      WorkflowFeatureToggle workflowFeatureToggle) {
    this.permissaoUsuarioService = permissaoUsuarioService;
    this.usuarioEmpresaPreferenciaService = usuarioEmpresaPreferenciaService;
    this.movimentoConfigFeatureToggle = movimentoConfigFeatureToggle;
    this.workflowFeatureToggle = workflowFeatureToggle;
  }

  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> me(Authentication authentication,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String username = jwt.getClaimAsString("preferred_username");
      if (username == null) {
        username = jwt.getClaimAsString("email");
      }
      List<String> roles = List.of();
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null) {
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof java.util.Collection<?> rc) {
          roles = rc.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .toList();
        }
      }
      String tenantFromClaim = jwt.getClaimAsString("tenant_id");
      Long tenant = TenantContext.getTenantId();
      if (tenant == null && tenantFromClaim != null) {
        try {
          tenant = Long.parseLong(tenantFromClaim);
        } catch (NumberFormatException ignored) {
        }
      }
      if (tenant == null && tenantId != null) {
        try {
          tenant = Long.parseLong(tenantId);
        } catch (NumberFormatException ignored) {
        }
      }
      Set<String> permissoes = tenant == null ? Set.of() : permissaoUsuarioService.permissoes(tenant, jwt.getSubject());
      List<String> papeis = tenant == null ? List.of() : permissaoUsuarioService.papeis(tenant, jwt.getSubject());
      if ("master".equalsIgnoreCase(username)) {
        java.util.LinkedHashSet<String> boosted = new java.util.LinkedHashSet<>(papeis);
        boosted.add("MASTER");
        boosted.add("ADMIN");
        papeis = java.util.List.copyOf(boosted);
      }

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("id", jwt.getSubject());
      payload.put("username", username);
      payload.put("roles", roles);
      payload.put("tenantRoles", papeis);
      payload.put("permissions", permissoes);
      payload.put("tenantId", tenantFromClaim != null ? tenantFromClaim : tenantId);
      payload.put("features", Map.of(
        "movementConfigEnabled", movimentoConfigFeatureToggle.isEnabled(),
        "movementConfigStrictEnabled", movimentoConfigFeatureToggle.isStrictEnabled(),
        "workflowEnabled", workflowFeatureToggle.isEnabled()));

      return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(payload);
    }

    return ResponseEntity.status(401).build();
  }

  @GetMapping("/me/empresa-padrao")
  public ResponseEntity<UsuarioEmpresaPadraoResponse> getEmpresaPadrao(Authentication authentication) {
    String usuarioId = resolveUsuarioId(authentication);
    Long empresaId = usuarioEmpresaPreferenciaService.getEmpresaPadraoId(usuarioId);
    return ResponseEntity.ok()
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(new UsuarioEmpresaPadraoResponse(empresaId));
  }

  @PutMapping("/me/empresa-padrao")
  public ResponseEntity<UsuarioEmpresaPadraoResponse> setEmpresaPadrao(
      Authentication authentication,
      @Valid @RequestBody UsuarioEmpresaPadraoRequest request) {
    String usuarioId = resolveUsuarioId(authentication);
    Long empresaId = usuarioEmpresaPreferenciaService.setEmpresaPadraoId(usuarioId, request.empresaId());
    return ResponseEntity.ok()
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(new UsuarioEmpresaPadraoResponse(empresaId));
  }

  @DeleteMapping("/me/empresa-padrao")
  public ResponseEntity<Void> clearEmpresaPadrao(Authentication authentication) {
    String usuarioId = resolveUsuarioId(authentication);
    usuarioEmpresaPreferenciaService.clearEmpresaPadraoId(usuarioId);
    return ResponseEntity.noContent().build();
  }

  private String resolveUsuarioId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getSubject();
    }
    throw new IllegalStateException("unauthorized");
  }
}
