package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioPapel;
import com.ia.app.dto.PasswordResetRequest;
import com.ia.app.dto.UsuarioRequest;
import com.ia.app.dto.UsuarioResponse;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

  private final UsuarioRepository repository;
  private final KeycloakAdminService keycloakAdminService;
  private final UsuarioPapelRepository usuarioPapelRepository;
  private final PapelRepository papelRepository;

  public UsuarioService(UsuarioRepository repository,
      KeycloakAdminService keycloakAdminService,
      UsuarioPapelRepository usuarioPapelRepository,
      PapelRepository papelRepository) {
    this.repository = repository;
    this.keycloakAdminService = keycloakAdminService;
    this.usuarioPapelRepository = usuarioPapelRepository;
    this.papelRepository = papelRepository;
  }

  public Page<Usuario> findAll(Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public Page<UsuarioResponse> findAllWithPapeis(Pageable pageable) {
    Long tenantId = requireTenant();
    Page<Usuario> page = repository.findAllByTenantId(tenantId, pageable);
    List<Usuario> usuarios = page.getContent();
    List<String> keycloakIds = usuarios.stream().map(Usuario::getKeycloakId).toList();
    Map<String, List<Long>> userToPapelIds = usuarioPapelRepository
      .findAllByTenantIdAndUsuarioIdIn(tenantId, keycloakIds)
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
    Long tenantId = requireTenant();
    String keycloakId = keycloakAdminService.createUser(
      request.username(),
      request.email(),
      request.password(),
      request.ativo(),
      request.roles()
    );

    Usuario usuario = new Usuario();
    usuario.setTenantId(tenantId);
    usuario.setKeycloakId(keycloakId);
    usuario.setUsername(request.username());
    usuario.setEmail(request.email());
    usuario.setAtivo(request.ativo());
    return repository.save(usuario);
  }

  public Usuario disable(Long id) {
    Long tenantId = requireTenant();
    Usuario usuario = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    keycloakAdminService.disableUser(usuario.getKeycloakId());
    usuario.setAtivo(false);
    return repository.save(usuario);
  }

  public Usuario getById(Long id) {
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
  }

  public Usuario update(Long id, com.ia.app.dto.UsuarioUpdateRequest request) {
    Long tenantId = requireTenant();
    Usuario usuario = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
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
    Long tenantId = requireTenant();
    Usuario usuario = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    usuarioPapelRepository.deleteAllByTenantIdAndUsuarioId(tenantId, usuario.getKeycloakId());
    keycloakAdminService.deleteUser(usuario.getKeycloakId());
    repository.delete(usuario);
  }

  public void resetPassword(Long id, PasswordResetRequest request) {
    Long tenantId = requireTenant();
    Usuario usuario = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    keycloakAdminService.setPassword(usuario.getKeycloakId(), request.newPassword(), false);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
