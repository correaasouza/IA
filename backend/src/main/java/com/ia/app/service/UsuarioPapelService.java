package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioPapel;
import com.ia.app.dto.UsuarioPapelResponse;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioPapelService {

  private final UsuarioPapelRepository repository;
  private final UsuarioRepository usuarioRepository;
  private final PapelRepository papelRepository;
  private final AuditService auditService;

  public UsuarioPapelService(UsuarioPapelRepository repository,
      UsuarioRepository usuarioRepository,
      PapelRepository papelRepository,
      AuditService auditService) {
    this.repository = repository;
    this.usuarioRepository = usuarioRepository;
    this.papelRepository = papelRepository;
    this.auditService = auditService;
  }

  public UsuarioPapelResponse listByUsuario(Long usuarioLocalId) {
    Long tenantId = requireTenant();
    Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioLocalId, tenantId).orElseThrow();
    List<Long> papelIds = repository.findPapelIdsByUsuario(tenantId, usuario.getKeycloakId());
    Map<Long, String> nomes = papelRepository.findAllById(papelIds).stream()
      .collect(Collectors.toMap(Papel::getId, Papel::getNome));
    List<String> papeis = papelIds.stream().map(id -> nomes.getOrDefault(id, "")).toList();
    return new UsuarioPapelResponse(papelIds, papeis);
  }

  @Transactional
  @CacheEvict(value = {"permissoesUsuario", "papeisUsuario"}, allEntries = true)
  public UsuarioPapelResponse setByUsuario(Long usuarioLocalId, List<Long> papelIds) {
    Long tenantId = requireTenant();
    Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioLocalId, tenantId).orElseThrow();
    String keycloakId = usuario.getKeycloakId();
    List<Long> uniquePapelIds = papelIds == null
      ? List.of()
      : papelIds.stream()
        .filter(java.util.Objects::nonNull)
        .distinct()
        .toList();
    for (Long papelId : uniquePapelIds) {
      Papel papel = papelRepository.findById(papelId).orElseThrow();
      if (!papel.getTenantId().equals(tenantId)) {
        throw new IllegalStateException("papel_forbidden");
      }
      if (isMasterRole(papel.getNome()) && !isMasterUsername(usuario.getUsername())) {
        throw new IllegalArgumentException("usuario_master_role_restrito");
      }
    }
    repository.deleteAllByTenantIdAndUsuarioId(tenantId, keycloakId);
    for (Long papelId : uniquePapelIds) {
      if (repository.existsByTenantIdAndUsuarioIdAndPapelId(tenantId, keycloakId, papelId)) {
        continue;
      }
      UsuarioPapel up = new UsuarioPapel();
      up.setTenantId(tenantId);
      up.setUsuarioId(keycloakId);
      up.setPapelId(papelId);
      repository.save(up);
    }
    auditService.log(tenantId, "USUARIO_PAPEIS_ATUALIZADOS", "usuario", String.valueOf(usuarioLocalId),
      "papeis=" + uniquePapelIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
    return listByUsuario(usuarioLocalId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }

  private boolean isMasterRole(String roleName) {
    return roleName != null && roleName.trim().equalsIgnoreCase("MASTER");
  }

  private boolean isMasterUsername(String username) {
    return username != null && username.trim().equalsIgnoreCase("master");
  }
}
