package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioLocatarioAcesso;
import com.ia.app.domain.UsuarioPapel;
import com.ia.app.dto.PasswordResetRequest;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.dto.UsuarioResponse;
import com.ia.app.repository.AtalhoUsuarioRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioEmpresaPreferenciaRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

  private final UsuarioRepository repository;
  private final KeycloakAdminService keycloakAdminService;
  private final UsuarioPapelRepository usuarioPapelRepository;
  private final PapelRepository papelRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;
  private final UsuarioEmpresaPreferenciaRepository usuarioEmpresaPreferenciaRepository;
  private final AtalhoUsuarioRepository atalhoUsuarioRepository;

  public UsuarioService(UsuarioRepository repository,
      KeycloakAdminService keycloakAdminService,
      UsuarioPapelRepository usuarioPapelRepository,
      PapelRepository papelRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository,
      UsuarioEmpresaPreferenciaRepository usuarioEmpresaPreferenciaRepository,
      AtalhoUsuarioRepository atalhoUsuarioRepository) {
    this.repository = repository;
    this.keycloakAdminService = keycloakAdminService;
    this.usuarioPapelRepository = usuarioPapelRepository;
    this.papelRepository = papelRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
    this.usuarioEmpresaPreferenciaRepository = usuarioEmpresaPreferenciaRepository;
    this.atalhoUsuarioRepository = atalhoUsuarioRepository;
  }

  @Transactional
  public Page<Usuario> findAll(Pageable pageable) {
    if (isGlobalMaster()) {
      Page<Usuario> page = repository.findAll(pageable);
      List<Usuario> activeUsers = purgeMissingInKeycloak(page.getContent());
      long removed = page.getContent().size() - activeUsers.size();
      long total = Math.max(0L, page.getTotalElements() - removed);
      return new org.springframework.data.domain.PageImpl<>(activeUsers, pageable, total);
    }
    Long tenantId = requireTenant();
    Page<Usuario> page = repository.findAllAccessibleByTenantId(tenantId, pageable);
    List<Usuario> activeUsers = purgeMissingInKeycloak(page.getContent());
    long removed = page.getContent().size() - activeUsers.size();
    long total = Math.max(0L, page.getTotalElements() - removed);
    return new org.springframework.data.domain.PageImpl<>(activeUsers, pageable, total);
  }

  @Transactional
  public Page<UsuarioResponse> findAllWithPapeis(Pageable pageable) {
    boolean masterScope = isGlobalMaster();
    Long tenantId = masterScope ? null : requireTenant();
    Page<Usuario> page = masterScope
      ? repository.findAll(pageable)
      : repository.findAllAccessibleByTenantId(tenantId, pageable);
    List<Usuario> pageUsers = page.getContent();
    List<Usuario> usuarios = purgeMissingInKeycloak(pageUsers);
    List<String> keycloakIds = usuarios.stream().map(Usuario::getKeycloakId).toList();
    if (keycloakIds.isEmpty()) {
      long removed = pageUsers.size() - usuarios.size();
      long total = Math.max(0L, page.getTotalElements() - removed);
      return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, total);
    }
    Map<String, List<Long>> userToPapelIds = (masterScope
      ? usuarioPapelRepository.findAllByUsuarioIdIn(keycloakIds)
      : usuarioPapelRepository.findAllByTenantIdAndUsuarioIdIn(tenantId, keycloakIds))
      .stream()
      .collect(Collectors.groupingBy(UsuarioPapel::getUsuarioId,
        Collectors.mapping(UsuarioPapel::getPapelId, Collectors.toList())));
    List<Long> allPapelIds = userToPapelIds.values().stream().flatMap(List::stream).distinct().toList();
    Map<Long, String> papelNomes = allPapelIds.isEmpty()
      ? Map.of()
      : papelRepository.findAllById(allPapelIds).stream()
        .filter(Papel::isAtivo)
        .collect(Collectors.toMap(Papel::getId, Papel::getNome));
    List<UsuarioResponse> content = usuarios.stream().map(u -> new UsuarioResponse(
        u.getId(),
        u.getUsername(),
        u.getEmail(),
        u.isAtivo(),
        userToPapelIds.getOrDefault(u.getKeycloakId(), List.of()).stream()
          .map(id -> papelNomes.getOrDefault(id, ""))
          .filter(n -> !n.isEmpty())
          .toList()
      ))
      .toList();
    long removed = pageUsers.size() - usuarios.size();
    long total = Math.max(0L, page.getTotalElements() - removed);
    return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
  }

  @Transactional
  public Usuario create(UsuarioRequest request) {
    ensureMasterCanManageUsers();
    Long tenantId = requireTenant();
    String username = normalizeUsername(request.username());
    String email = normalizeEmail(request.email());
    ensureUniqueUserIdentity(username, email);
    enforceMasterRoleRestriction(username, request.roles());

    String keycloakId = keycloakAdminService.createUser(
      username,
      email,
      request.password(),
      request.ativo(),
      request.roles()
    );

    try {
      Usuario usuario = new Usuario();
      usuario.setTenantId(tenantId);
      usuario.setKeycloakId(keycloakId);
      usuario.setUsername(username);
      usuario.setEmail(email);
      usuario.setAtivo(request.ativo());
      Usuario saved = repository.save(usuario);
      UsuarioLocatarioAcesso acesso = new UsuarioLocatarioAcesso();
      acesso.setUsuarioId(saved.getKeycloakId());
      acesso.setLocatarioId(saved.getTenantId());
      usuarioLocatarioAcessoRepository.save(acesso);
      syncLocalRoles(saved, request.roles());
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
    Usuario usuario;
    if (isGlobalMaster()) {
      usuario = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    } else {
      Long tenantId = requireTenant();
      usuario = repository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    if (removeIfMissingInKeycloak(usuario)) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    return usuario;
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

  @Transactional
  public void delete(Long id) {
    ensureMasterCanManageUsers();
    Usuario usuario = findManagedUserById(id);
    ensureNotProtectedMasterUser(usuario);
    ensureNotCurrentAuthenticatedUser(usuario);
    deleteLocalReferences(usuario.getKeycloakId());
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
    if (isGlobalMaster()) {
      return;
    }
    throw new IllegalStateException("role_required_user_master");
  }

  private boolean isGlobalMaster() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    boolean hasMasterRole = auth.getAuthorities().stream()
      .anyMatch(a -> "ROLE_MASTER".equals(a.getAuthority()));
    if (hasMasterRole) {
      return true;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
      return preferredUsername != null && preferredUsername.equalsIgnoreCase("master");
    }
    String name = auth.getName();
    return name != null && name.equalsIgnoreCase("master");
  }

  private Usuario findManagedUserById(Long id) {
    Usuario usuario;
    if (isGlobalMaster()) {
      usuario = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    } else {
      Long tenantId = requireTenant();
      usuario = repository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    if (removeIfMissingInKeycloak(usuario)) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    return usuario;
  }

  private void enforceMasterRoleRestriction(String username, List<String> roles) {
    boolean hasMasterRole = (roles == null ? List.<String>of() : roles).stream()
      .anyMatch(this::isMasterRole);
    if (!hasMasterRole) {
      return;
    }
    if (isMasterUsername(username)) {
      return;
    }
    throw new IllegalArgumentException("usuario_master_role_restrito");
  }

  private boolean isMasterRole(String role) {
    return role != null && role.trim().equalsIgnoreCase("MASTER");
  }

  private boolean isMasterUsername(String username) {
    return username != null && username.trim().equalsIgnoreCase("master");
  }

  private void ensureNotProtectedMasterUser(Usuario usuario) {
    if (usuario != null && isMasterUsername(usuario.getUsername())) {
      throw new IllegalArgumentException("usuario_master_protegido");
    }
  }

  private void ensureNotCurrentAuthenticatedUser(Usuario usuario) {
    if (usuario == null) {
      return;
    }
    String currentUserId = resolveAuthenticatedUserId();
    if (currentUserId == null || currentUserId.isBlank()) {
      return;
    }
    if (currentUserId.equals(usuario.getKeycloakId())) {
      throw new IllegalArgumentException("usuario_self_delete_forbidden");
    }
  }

  private String resolveAuthenticatedUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getSubject();
    }
    return auth.getName();
  }

  private List<Usuario> purgeMissingInKeycloak(List<Usuario> users) {
    if (users == null || users.isEmpty()) {
      return List.of();
    }
    List<Usuario> active = new ArrayList<>(users.size());
    for (Usuario user : users) {
      if (!removeIfMissingInKeycloak(user)) {
        active.add(user);
      }
    }
    return active;
  }

  private boolean removeIfMissingInKeycloak(Usuario usuario) {
    if (usuario == null) {
      return false;
    }
    String keycloakId = usuario.getKeycloakId();
    if (keycloakId == null || keycloakId.isBlank()) {
      repository.delete(usuario);
      return true;
    }
    if (keycloakAdminService.userExists(keycloakId)) {
      return false;
    }
    deleteLocalReferences(keycloakId);
    repository.delete(usuario);
    return true;
  }

  private void deleteLocalReferences(String keycloakId) {
    if (keycloakId == null || keycloakId.isBlank()) {
      return;
    }
    usuarioPapelRepository.deleteAllByUsuarioId(keycloakId);
    usuarioLocatarioAcessoRepository.deleteAllByUsuarioId(keycloakId);
    usuarioEmpresaPreferenciaRepository.deleteAllByUsuarioId(keycloakId);
    atalhoUsuarioRepository.deleteAllByUserId(keycloakId);
  }

  private void ensureUniqueUserIdentity(String username, String email) {
    if (repository.existsByUsernameIgnoreCase(username)) {
      throw new IllegalArgumentException("usuario_username_duplicado");
    }
    if (email != null && repository.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("usuario_email_duplicado");
    }
  }

  private String normalizeUsername(String username) {
    return username == null ? "" : username.trim();
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    String normalized = email.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private void syncLocalRoles(Usuario usuario, List<String> roles) {
    if (usuario == null || roles == null || roles.isEmpty()) {
      return;
    }
    Long tenantId = usuario.getTenantId();
    String keycloakId = usuario.getKeycloakId();
    List<String> normalizedRoles = roles.stream()
      .map(this::normalizeRoleName)
      .filter(role -> !role.isEmpty())
      .distinct()
      .toList();
    for (String roleName : normalizedRoles) {
      Papel papel = resolveOrCreatePapel(tenantId, roleName);
      if (usuarioPapelRepository.existsByTenantIdAndUsuarioIdAndPapelId(tenantId, keycloakId, papel.getId())) {
        continue;
      }
      UsuarioPapel up = new UsuarioPapel();
      up.setTenantId(tenantId);
      up.setUsuarioId(keycloakId);
      up.setPapelId(papel.getId());
      usuarioPapelRepository.save(up);
    }
  }

  private Papel resolveOrCreatePapel(Long tenantId, String roleName) {
    return papelRepository.findByTenantIdAndNome(tenantId, roleName)
      .orElseGet(() -> {
        Papel novo = new Papel();
        novo.setTenantId(tenantId);
        novo.setNome(roleName);
        novo.setDescricao("Papel sincronizado com Keycloak");
        novo.setAtivo(true);
        return papelRepository.save(novo);
      });
  }

  private String normalizeRoleName(String roleName) {
    return roleName == null ? "" : roleName.trim().toUpperCase();
  }
}
