package com.ia.app.service;

import com.ia.app.domain.Locatario;
import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioPapel;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAdminSeedService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioPapelRepository usuarioPapelRepository;
  private final PapelRepository papelRepository;
  private final KeycloakAdminService keycloakAdminService;

  @Value("${tenant.seed-admin.enabled:false}")
  private boolean enabled;

  @Value("${tenant.seed-admin.username-template:admin-{tenantId}}")
  private String usernameTemplate;

  @Value("${tenant.seed-admin.email-template:admin{tenantId}@local}")
  private String emailTemplate;

  @Value("${tenant.seed-admin.password:admin123}")
  private String password;

  public TenantAdminSeedService(UsuarioRepository usuarioRepository,
      UsuarioPapelRepository usuarioPapelRepository,
      PapelRepository papelRepository,
      KeycloakAdminService keycloakAdminService) {
    this.usuarioRepository = usuarioRepository;
    this.usuarioPapelRepository = usuarioPapelRepository;
    this.papelRepository = papelRepository;
    this.keycloakAdminService = keycloakAdminService;
  }

  @Transactional
  public void seedDefaultAdmin(Locatario locatario) {
    if (!enabled || locatario == null) {
      return;
    }
    Long tenantId = locatario.getId();
    String username = applyTemplate(usernameTemplate, tenantId);
    String email = applyTemplate(emailTemplate, tenantId);

    if (usuarioRepository.findByTenantIdAndUsername(tenantId, username).isPresent()) {
      return;
    }

    String keycloakId = keycloakAdminService.createUser(
      username,
      email,
      password,
      true,
      List.of("TENANT_ADMIN")
    );

    Usuario usuario = new Usuario();
    usuario.setTenantId(tenantId);
    usuario.setKeycloakId(keycloakId);
    usuario.setUsername(username);
    usuario.setEmail(email);
    usuario.setAtivo(true);
    Usuario saved = usuarioRepository.save(usuario);

    papelRepository.findByTenantIdAndNome(tenantId, "ADMIN").ifPresent(papel -> {
      UsuarioPapel up = new UsuarioPapel();
      up.setTenantId(tenantId);
      up.setUsuarioId(saved.getKeycloakId());
      up.setPapelId(papel.getId());
      usuarioPapelRepository.save(up);
    });
  }

  private String applyTemplate(String template, Long tenantId) {
    return template.replace("{tenantId}", String.valueOf(tenantId));
  }
}
