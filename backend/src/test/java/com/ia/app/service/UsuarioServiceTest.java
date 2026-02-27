package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.repository.AtalhoUsuarioRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioEmpresaPreferenciaRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

  @Mock
  private UsuarioRepository repository;

  @Mock
  private KeycloakAdminService keycloakAdminService;

  @Mock
  private UsuarioPapelRepository usuarioPapelRepository;

  @Mock
  private PapelRepository papelRepository;

  @Mock
  private UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;

  @Mock
  private UsuarioEmpresaPreferenciaRepository usuarioEmpresaPreferenciaRepository;

  @Mock
  private AtalhoUsuarioRepository atalhoUsuarioRepository;

  @InjectMocks
  private UsuarioService service;

  @AfterEach
  void cleanup() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldBlockMasterRoleForNonMasterUsernameOnCreate() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    TenantContext.setTenantId(1L);
    when(repository.existsByUsernameIgnoreCase("joao")).thenReturn(false);
    when(repository.existsByEmailIgnoreCase("joao@local")).thenReturn(false);

    UsuarioRequest request = new UsuarioRequest(
      "joao",
      "joao@local",
      "123456",
      true,
      List.of("USER", "MASTER")
    );

    assertThatThrownBy(() -> service.create(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("usuario_master_role_restrito");

    verify(keycloakAdminService, never()).createUser(any(), any(), any(), any(Boolean.class), any());
  }

  @Test
  void shouldAllowMasterRoleForMasterUsernameOnCreate() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    TenantContext.setTenantId(1L);
    when(repository.existsByUsernameIgnoreCase("master")).thenReturn(false);
    when(repository.existsByEmailIgnoreCase("master@local")).thenReturn(false);
    when(keycloakAdminService.createUser("master", "master@local", "123456", true, List.of("MASTER")))
      .thenReturn("kc-master");
    when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Papel masterPapel = new Papel();
    masterPapel.setTenantId(1L);
    masterPapel.setNome("MASTER");
    ReflectionTestUtils.setField(masterPapel, "id", 99L);
    when(papelRepository.findByTenantIdAndNome(1L, "MASTER")).thenReturn(Optional.of(masterPapel));
    when(usuarioPapelRepository.existsByTenantIdAndUsuarioIdAndPapelId(1L, "kc-master", 99L)).thenReturn(false);

    UsuarioRequest request = new UsuarioRequest(
      "master",
      "master@local",
      "123456",
      true,
      List.of("MASTER")
    );

    Usuario created = service.create(request);

    assertThat(created.getUsername()).isEqualTo("master");
    assertThat(created.getKeycloakId()).isEqualTo("kc-master");
    verify(keycloakAdminService).createUser("master", "master@local", "123456", true, List.of("MASTER"));
    verify(usuarioLocatarioAcessoRepository).save(any());
    verify(usuarioPapelRepository).save(any());
  }

  @Test
  void shouldBlockWhenUsernameAlreadyExistsGlobally() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    TenantContext.setTenantId(2L);
    when(repository.existsByUsernameIgnoreCase("user1")).thenReturn(true);

    UsuarioRequest request = new UsuarioRequest(
      "user1",
      "newmail@local",
      "123456",
      true,
      List.of("USER")
    );

    assertThatThrownBy(() -> service.create(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("usuario_username_duplicado");

    verify(keycloakAdminService, never()).createUser(any(), any(), any(), any(Boolean.class), any());
  }

  @Test
  void shouldPersistRequestedRolesLocallyOnCreate() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    TenantContext.setTenantId(1L);
    when(repository.existsByUsernameIgnoreCase("user3")).thenReturn(false);
    when(repository.existsByEmailIgnoreCase("user3@local")).thenReturn(false);
    when(keycloakAdminService.createUser("user3", "user3@local", "123456", true, List.of("ADMIN", "USER")))
      .thenReturn("kc-user3");
    when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Papel admin = new Papel();
    admin.setTenantId(1L);
    admin.setNome("ADMIN");
    ReflectionTestUtils.setField(admin, "id", 10L);

    Papel user = new Papel();
    user.setTenantId(1L);
    user.setNome("USER");
    ReflectionTestUtils.setField(user, "id", 11L);

    when(papelRepository.findByTenantIdAndNome(1L, "ADMIN")).thenReturn(Optional.of(admin));
    when(papelRepository.findByTenantIdAndNome(1L, "USER")).thenReturn(Optional.of(user));
    when(usuarioPapelRepository.existsByTenantIdAndUsuarioIdAndPapelId(1L, "kc-user3", 10L)).thenReturn(false);
    when(usuarioPapelRepository.existsByTenantIdAndUsuarioIdAndPapelId(1L, "kc-user3", 11L)).thenReturn(false);

    UsuarioRequest request = new UsuarioRequest(
      "user3",
      "user3@local",
      "123456",
      true,
      List.of("ADMIN", "USER")
    );

    service.create(request);

    verify(usuarioPapelRepository, times(2)).save(any());
  }

  @Test
  void shouldBlockDeleteForMasterUser() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    Usuario masterUser = new Usuario();
    masterUser.setUsername("master");
    masterUser.setKeycloakId("kc-master");
    when(repository.findById(1L)).thenReturn(Optional.of(masterUser));
    when(keycloakAdminService.userExists("kc-master")).thenReturn(true);

    assertThatThrownBy(() -> service.delete(1L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("usuario_master_protegido");

    verify(keycloakAdminService, never()).deleteUser(any());
  }

  @Test
  void shouldBlockDeleteForCurrentAuthenticatedUser() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    Usuario currentUser = new Usuario();
    currentUser.setUsername("alex");
    currentUser.setKeycloakId("master-id");
    when(repository.findById(2L)).thenReturn(Optional.of(currentUser));
    when(keycloakAdminService.userExists("master-id")).thenReturn(true);

    assertThatThrownBy(() -> service.delete(2L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("usuario_self_delete_forbidden");

    verify(keycloakAdminService, never()).deleteUser(any());
  }

  @Test
  void shouldAutoDeleteLocalUserWhenMissingInKeycloak() {
    SecurityContextHolder.getContext().setAuthentication(masterAuth());
    Usuario orphanUser = new Usuario();
    orphanUser.setUsername("user2");
    orphanUser.setKeycloakId("kc-orphan");
    when(repository.findById(3L)).thenReturn(Optional.of(orphanUser));
    when(keycloakAdminService.userExists("kc-orphan")).thenReturn(false);

    assertThatThrownBy(() -> service.getById(3L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("usuario_not_found");

    verify(usuarioPapelRepository).deleteAllByUsuarioId("kc-orphan");
    verify(usuarioLocatarioAcessoRepository).deleteAllByUsuarioId("kc-orphan");
    verify(usuarioEmpresaPreferenciaRepository).deleteAllByUsuarioId("kc-orphan");
    verify(atalhoUsuarioRepository).deleteAllByUserId("kc-orphan");
    verify(repository).delete(orphanUser);
  }

  private JwtAuthenticationToken masterAuth() {
    Jwt jwt = new Jwt(
      "token",
      Instant.now(),
      Instant.now().plusSeconds(120),
      Map.of("alg", "none"),
      Map.of(
        "sub", "master-id",
        "preferred_username", "master"
      )
    );
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MASTER")));
  }
}
