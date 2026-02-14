package com.ia.app.web;

import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

  private final PermissaoUsuarioService permissaoUsuarioService;

  public MeController(PermissaoUsuarioService permissaoUsuarioService) {
    this.permissaoUsuarioService = permissaoUsuarioService;
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
      if (tenant != null && tenant == 1L && "master".equalsIgnoreCase(username)) {
        java.util.LinkedHashSet<String> boosted = new java.util.LinkedHashSet<>(papeis);
        boosted.add("MASTER");
        boosted.add("ADMIN");
        papeis = java.util.List.copyOf(boosted);
      }

      return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(Map.of(
          "id", jwt.getSubject(),
          "username", username,
          "roles", roles,
          "tenantRoles", papeis,
          "permissions", permissoes,
          "tenantId", tenantFromClaim != null ? tenantFromClaim : tenantId
        ));
    }

    return ResponseEntity.status(401).build();
  }
}
