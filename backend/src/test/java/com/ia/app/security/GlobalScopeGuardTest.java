package com.ia.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class GlobalScopeGuardTest {

  private final GlobalScopeGuard guard = new GlobalScopeGuard();

  @AfterEach
  void cleanup() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAllowGlobalMaster() {
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("master", true));

    assertThat(guard.isGlobalMaster()).isTrue();
  }

  @Test
  void shouldBlockGlobalMasterWhenUsernameIsNotMaster() {
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("admin", true));

    assertThat(guard.isGlobalMaster()).isFalse();
  }

  @Test
  void shouldAllowMasterInMasterTenant() {
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("master", true));
    TenantContext.setTenantId(1L);

    assertThat(guard.isMasterInMasterTenant()).isTrue();
  }

  @Test
  void shouldBlockMasterInNonMasterTenant() {
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("master", true));
    TenantContext.setTenantId(2L);

    assertThat(guard.isMasterInMasterTenant()).isFalse();
  }

  @Test
  void shouldBlockWhenOnlyMasterRoleWithoutMasterUsername() {
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(
        "outro",
        "n/a",
        List.of(new SimpleGrantedAuthority("ROLE_MASTER"))
      )
    );

    assertThat(guard.isGlobalMaster()).isFalse();
  }

  private JwtAuthenticationToken jwtAuth(String preferredUsername, boolean withMasterRole) {
    Jwt jwt = new Jwt(
      "token",
      Instant.now(),
      Instant.now().plusSeconds(120),
      Map.of("alg", "none"),
      Map.of(
        "sub", "user-id",
        "preferred_username", preferredUsername
      )
    );
    List<SimpleGrantedAuthority> authorities = withMasterRole
      ? List.of(new SimpleGrantedAuthority("ROLE_MASTER"))
      : List.of();
    return new JwtAuthenticationToken(jwt, authorities);
  }
}
