package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Locatario;
import com.ia.app.dto.LocatarioRequest;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
