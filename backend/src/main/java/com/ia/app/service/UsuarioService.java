package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioLocatarioAcesso;
import com.ia.app.domain.UsuarioPapel;
import com.ia.app.dto.PasswordResetRequest;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.dto.UsuarioResponse;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

  private final UsuarioRepository repository;
  private final KeycloakAdminService keycloakAdminService;
  private final UsuarioPapelRepository usuarioPapelRepository;
  private final PapelRepository papelRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;

  public UsuarioService(UsuarioRepository repository,
      KeycloakAdminService keycloakAdminService,
      UsuarioPapelRepository usuarioPapelRepository,
      PapelRepository papelRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository) {
    this.repository = repository;
    this.keycloakAdminService = keycloakAdminService;
    this.usuarioPapelRepository = usuarioPapelRepository;
    this.papelRepository = papelRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
  }

  public Page<Usuario> findAll(Pageable pageable) {
    if (isLocatarioMaster() || isUsuarioMaster()) {
      return repository.findAll(pageable);
    }
    Long tenantId = requireTenant();
    return repository.findAllAccessibleByTenantId(tenantId, pageable);
  }

  public Page<UsuarioResponse> findAllWithPapeis(Pageable pageable) {
    boolean masterScope = isLocatarioMaster() || isUsuarioMaster();
    Long tenantId = requireTenant();
    Page<Usuario> page = masterScope
      ? repository.findAll(pageable)
      : repository.findAllAccessibleByTenantId(tenantId, pageable);
    List<Usuario> usuarios = page.getContent();
    List<String> keycloakIds = usuarios.stream().map(Usuario::getKeycloakId).toList();
    Map<String, List<Long>> userToPapelIds = (masterScope
      ? usuarioPapelRepository.findAllByUsuarioIdIn(keycloakIds)
      : usuarioPapelRepository.findAllByTenantIdAndUsuarioIdIn(tenantId, keycloakIds))
      .stream()
      .collect(Collectors.groupingBy(UsuarioPapel::getUsuarioId,
        Collectors.mapping(UsuarioPapel::getPapelId, Collectors.toList())));
    List<Long> allPapelIds = userToPapelIds.values().stream().flatMap(List::stream).distinct().toList();
    Map<Long, String> papelNomes = papelRepository.findAllById(allPapelIds).stream()
      .filter(Papel::isAtivo)
      .collect(Collectors.toMap(Papel::getId, Papel::getNome));
    return page.map(u -> new UsuarioResponse(
      u.getId(),
      u.getUsername(),
      u.getEmail(),
      u.isAtivo(),
      userToPapelIds.getOrDefault(u.getKeycloakId(), List.of()).stream()
        .map(id -> papelNomes.getOrDefault(id, ""))
        .filter(n -> !n.isEmpty())
        .toList()
    ));
  }

  public Usuario create(UsuarioRequest request) {
    ensureMasterCanManageUsers();
    Long tenantId = requireTenant();
    repository.findByTenantIdAndUsername(tenantId, request.username())
      .ifPresent(u -> {
        throw new IllegalArgumentException("usuario_username_duplicado");
      });

    String keycloakId = keycloakAdminService.createUser(
      request.username(),
      request.email(),
      request.password(),
      request.ativo(),
      request.roles()
    );

    try {
      Usuario usuario = new Usuario();
      usuario.setTenantId(tenantId);
      usuario.setKeycloakId(keycloakId);
      usuario.setUsername(request.username());
      usuario.setEmail(request.email());
      usuario.setAtivo(request.ativo());
      Usuario saved = repository.save(usuario);
      UsuarioLocatarioAcesso acesso = new UsuarioLocatarioAcesso();
      acesso.setUsuarioId(saved.getKeycloakId());
      acesso.setLocatarioId(saved.getTenantId());
      usuarioLocatarioAcessoRepository.save(acesso);
      return saved;
    } catch (RuntimeException ex) {
      try {
        keycloakAdminService.deleteUser(keycloakId);
      } catch (Exception ignored) {
        // best effort cleanup to avoid orphaned Keycloak user
      }
      throw ex;
    }
  }

  public Usuario disable(Long id) {
    ensureMasterCanManageUsers();
    Usuario usuario = findManagedUserById(id);
    keycloakAdminService.disableUser(usuario.getKeycloakId());
    usuario.setAtivo(false);
    return repository.save(usuario);
  }

  public Usuario getById(Long id) {
    if (isLocatarioMaster() || isUsuarioMaster()) {
      return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
  }

  public Usuario update(Long id, com.ia.app.dto.UsuarioUpdateRequest request) {
    Usuario usuario = findManagedUserById(id);
    boolean changingStatus = request.ativo() != null && request.ativo() != usuario.isAtivo();
    if (changingStatus) {
      ensureMasterCanManageUsers();
    }
    java.util.Map<String, Object> fields = new java.util.HashMap<>();
    if (request.username() != null && !request.username().isBlank()) {
      fields.put("username", request.username());
      usuario.setUsername(request.username());
    }
    if (request.email() != null) {
      fields.put("email", request.email());
      usuario.setEmail(request.email());
    }
    fields.put("enabled", request.ativo());
    keycloakAdminService.updateUser(usuario.getKeycloakId(), fields);
    usuario.setAtivo(request.ativo());
    return repository.save(usuario);
  }

  public void delete(Long id) {
    ensureMasterCanManageUsers();
    Usuario usuario = findManagedUserById(id);
    usuarioPapelRepository.deleteAllByUsuarioId(usuario.getKeycloakId());
    usuarioLocatarioAcessoRepository.deleteAllByUsuarioId(usuario.getKeycloakId());
    keycloakAdminService.deleteUser(usuario.getKeycloakId());
    repository.delete(usuario);
  }

  public void resetPassword(Long id, PasswordResetRequest request) {
    Usuario usuario = findManagedUserById(id);
    keycloakAdminService.setPassword(usuario.getKeycloakId(), request.newPassword(), false);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private void ensureMasterCanManageUsers() {
    if (isLocatarioMaster() || isUsuarioMaster()) {
      return;
    }
    throw new IllegalStateException("role_required_user_master");
  }

  private boolean isLocatarioMaster() {
    Long tenantId = TenantContext.getTenantId();
    return tenantId != null && tenantId == 1L;
  }

  private boolean isUsuarioMaster() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
      return preferredUsername != null && preferredUsername.equalsIgnoreCase("master");
    }
    String name = auth.getName();
    return name != null && name.equalsIgnoreCase("master");
  }

  private Usuario findManagedUserById(Long id) {
    if (isLocatarioMaster() || isUsuarioMaster()) {
      return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
  }
}
