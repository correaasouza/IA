package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Locatario;
import com.ia.app.dto.LocatarioResponse;
import com.ia.app.dto.LocatarioRequest;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocatarioServiceTest {

  @Mock
  private LocatarioRepository repository;

  @Mock
  private UsuarioRepository usuarioRepository;

  @Mock
  private UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;

  @Mock
  private PermissaoCatalogService permissaoCatalogService;

  @Mock
  private PapelSeedService papelSeedService;

  @Mock
  private TenantAdminSeedService tenantAdminSeedService;

  @Mock
  private TipoEntidadeSeedService tipoEntidadeSeedService;

  @Mock
  private MovimentoConfigSeedService movimentoConfigSeedService;

  @Mock
  private TenantUnitService tenantUnitService;

  @InjectMocks
  private LocatarioService service;

  @Test
  void shouldSeedMirrorUnitsWhenTenantIsCreated() {
    when(repository.save(any(Locatario.class))).thenAnswer(invocation -> {
      Locatario saved = invocation.getArgument(0, Locatario.class);
      ReflectionTestUtils.setField(saved, "id", 700L);
      return saved;
    });

    LocatarioRequest request = new LocatarioRequest(
      "Tenant novo",
      LocalDate.now().plusDays(10),
      true);

    Locatario created = service.create(request);

    assertThat(created.getId()).isEqualTo(700L);
    verify(permissaoCatalogService).seedDefaults(700L);
    verify(papelSeedService).seedDefaults(700L);
    verify(tipoEntidadeSeedService).seedDefaults(700L);
    verify(movimentoConfigSeedService).seedDefaults(700L);
    verify(tenantUnitService).seedMissingMirrorsForTenant(700L);
    verify(tenantAdminSeedService).seedDefaultAdmin(created);
  }

  @Test
  void shouldReturnAllTenantsOnlyForGlobalMaster() {
    Locatario l1 = locatario(1L, "Master");
    Locatario l2 = locatario(2L, "Tenant 2");
    when(repository.findAll()).thenReturn(List.of(l1, l2));

    List<LocatarioResponse> result = service.findAllowed(jwtAuth(
      "master-subject",
      "master",
      List.of("ROLE_MASTER")));

    assertThat(result).hasSize(2);
    verify(repository).findAll();
  }

  @Test
  void shouldNotReturnAllTenantsForMasterRoleOutsideGlobalMaster() {
    Locatario l2 = locatario(2L, "Tenant 2");
    when(usuarioLocatarioAcessoRepository.findLocatarioIdsByUsuarioId("user-subject"))
      .thenReturn(List.of(2L));
    when(repository.findAllById(List.of(2L))).thenReturn(List.of(l2));

    List<LocatarioResponse> result = service.findAllowed(jwtAuth(
      "user-subject",
      "admin-local",
      List.of("ROLE_MASTER")));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(2L);
    verify(repository, never()).findAll();
  }

  private Locatario locatario(Long id, String nome) {
    Locatario entity = new Locatario();
    ReflectionTestUtils.setField(entity, "id", id);
    entity.setNome(nome);
    entity.setAtivo(true);
    entity.setDataLimiteAcesso(LocalDate.now().plusDays(30));
    return entity;
  }

  private JwtAuthenticationToken jwtAuth(String subject, String preferredUsername, List<String> roles) {
    Map<String, Object> claims = Map.of(
      "sub", subject,
      "preferred_username", preferredUsername
    );
    Jwt jwt = new Jwt(
      "token",
      Instant.now(),
      Instant.now().plusSeconds(3600),
      Map.of("alg", "none"),
      claims);
    List<GrantedAuthority> authorities = roles.stream()
      .map(SimpleGrantedAuthority::new)
      .map(GrantedAuthority.class::cast)
      .toList();
    return new JwtAuthenticationToken(jwt, authorities);
  }
}
