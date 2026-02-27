package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.dto.UsuarioPapelResponse;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioPapelServiceTest {

  @Mock
  private UsuarioPapelRepository repository;

  @Mock
  private UsuarioRepository usuarioRepository;

  @Mock
  private PapelRepository papelRepository;

  @Mock
  private AuditService auditService;

  @InjectMocks
  private UsuarioPapelService service;

  @AfterEach
  void cleanup() {
    TenantContext.clear();
  }

  @Test
  void shouldBlockMasterRoleForNonMasterUsernameOnSetPapeis() {
    TenantContext.setTenantId(10L);
    Usuario usuario = usuario("joao", "kc-joao", 10L);
    Papel master = papel(7L, 10L, "MASTER");
    when(usuarioRepository.findByIdAndTenantId(22L, 10L)).thenReturn(Optional.of(usuario));
    when(papelRepository.findById(7L)).thenReturn(Optional.of(master));

    assertThatThrownBy(() -> service.setByUsuario(22L, List.of(7L)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("usuario_master_role_restrito");

    verify(repository, never()).deleteAllByTenantIdAndUsuarioId(any(), any());
    verify(auditService, never()).log(any(), any(), any(), any(), any());
  }

  @Test
  void shouldAllowMasterRoleForMasterUsernameOnSetPapeis() {
    TenantContext.setTenantId(10L);
    Usuario usuario = usuario("master", "kc-master", 10L);
    Papel master = papel(7L, 10L, "MASTER");
    when(usuarioRepository.findByIdAndTenantId(22L, 10L)).thenReturn(Optional.of(usuario));
    when(papelRepository.findById(7L)).thenReturn(Optional.of(master));
    when(repository.existsByTenantIdAndUsuarioIdAndPapelId(10L, "kc-master", 7L)).thenReturn(false);
    when(repository.findPapelIdsByUsuario(10L, "kc-master")).thenReturn(List.of(7L));
    when(papelRepository.findAllById(List.of(7L))).thenReturn(List.of(master));

    UsuarioPapelResponse response = service.setByUsuario(22L, List.of(7L));

    assertThat(response.papelIds()).containsExactly(7L);
    assertThat(response.papeis()).containsExactly("MASTER");
    verify(repository).deleteAllByTenantIdAndUsuarioId(10L, "kc-master");
    verify(repository).save(any());
    verify(auditService).log(10L, "USUARIO_PAPEIS_ATUALIZADOS", "usuario", "22", "papeis=7");
  }

  private Usuario usuario(String username, String keycloakId, Long tenantId) {
    Usuario usuario = new Usuario();
    usuario.setTenantId(tenantId);
    usuario.setUsername(username);
    usuario.setKeycloakId(keycloakId);
    return usuario;
  }

  private Papel papel(Long id, Long tenantId, String nome) {
    Papel papel = new Papel();
    org.springframework.test.util.ReflectionTestUtils.setField(papel, "id", id);
    papel.setTenantId(tenantId);
    papel.setNome(nome);
    papel.setAtivo(true);
    return papel;
  }
}
