package com.ia.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Empresa;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioEmpresaAcessoRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthorizationServiceTest {

  private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository = mock(UsuarioLocatarioAcessoRepository.class);
  private final UsuarioEmpresaAcessoRepository usuarioEmpresaAcessoRepository = mock(UsuarioEmpresaAcessoRepository.class);
  private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
  private final PapelRepository papelRepository = mock(PapelRepository.class);
  private final PermissaoUsuarioService permissaoUsuarioService = mock(PermissaoUsuarioService.class);
  private AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    authorizationService = new AuthorizationService(
      usuarioRepository,
      usuarioLocatarioAcessoRepository,
      usuarioEmpresaAcessoRepository,
      empresaRepository,
      papelRepository,
      permissaoUsuarioService
    );
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  @Test
  void shouldRecognizeGlobalMaster() {
    var auth = jwtAuth("master", List.of("ROLE_MASTER"));
    assertThat(authorizationService.isGlobalMaster(auth, 1L)).isTrue();
    assertThat(authorizationService.isGlobalMaster(auth, 2L)).isTrue();
  }

  @Test
  void shouldAllowAdminToGrantCompanyAccess() {
    TenantContext.setTenantId(2L);
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("admin", List.of("ROLE_USER")));
    when(permissaoUsuarioService.papeis(2L, "u-1")).thenReturn(List.of("ADMIN"));
    assertThat(authorizationService.canGrantCompanyAccess("u-1", 2L)).isTrue();
  }

  @Test
  void shouldReturnAllCompaniesForAdmin() {
    TenantContext.setTenantId(2L);
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("admin", List.of("ROLE_USER")));
    when(permissaoUsuarioService.papeis(2L, "u-1")).thenReturn(List.of("ADMIN"));
    when(empresaRepository.findAllByTenantIdOrderByRazaoSocialAsc(2L)).thenReturn(List.of(empresa(10L), empresa(11L)));
    assertThat(authorizationService.getAccessibleCompanies("u-1", 2L)).containsExactly(10L, 11L);
  }

  @Test
  void shouldReturnExplicitCompaniesForUser() {
    TenantContext.setTenantId(2L);
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("user1", List.of("ROLE_USER")));
    when(permissaoUsuarioService.papeis(2L, "u-2")).thenReturn(List.of("USER"));
    when(usuarioEmpresaAcessoRepository.findEmpresaIdsByTenantIdAndUsuarioId(2L, "u-2")).thenReturn(List.of(99L));
    assertThat(authorizationService.getAccessibleCompanies("u-2", 2L)).containsExactly(99L);
  }

  @Test
  void shouldBlockDefaultCompanyOutsideScope() {
    TenantContext.setTenantId(2L);
    SecurityContextHolder.getContext().setAuthentication(jwtAuth("user1", List.of("ROLE_USER")));
    when(permissaoUsuarioService.papeis(2L, "u-2")).thenReturn(List.of("USER"));
    when(empresaRepository.existsByIdAndTenantId(50L, 2L)).thenReturn(true);
    when(usuarioEmpresaAcessoRepository.findEmpresaIdsByTenantIdAndUsuarioId(2L, "u-2")).thenReturn(List.of(51L));
    assertThat(authorizationService.canSetDefaultCompany("u-2", 2L, 50L)).isFalse();
  }

  private Empresa empresa(Long id) {
    Empresa empresa = new Empresa();
    empresa.setTenantId(2L);
    empresa.setTipo(Empresa.TIPO_MATRIZ);
    empresa.setRazaoSocial("Empresa " + id);
    empresa.setCnpj("00000000000000");
    empresa.setAtivo(true);
    try {
      var field = Empresa.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(empresa, id);
    } catch (Exception ignored) {
    }
    return empresa;
  }

  private JwtAuthenticationToken jwtAuth(String username, List<String> roles) {
    Jwt jwt = new Jwt(
      "token",
      Instant.now(),
      Instant.now().plusSeconds(120),
      Map.of("alg", "none"),
      Map.of(
        "sub", "u-1",
        "preferred_username", username
      )
    );
    return new JwtAuthenticationToken(
      jwt,
      roles.stream().map(SimpleGrantedAuthority::new).toList()
    );
  }
}
