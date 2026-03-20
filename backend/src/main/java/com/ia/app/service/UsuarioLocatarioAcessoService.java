package com.ia.app.service;

import com.ia.app.domain.Usuario;
import com.ia.app.dto.UsuarioLocatarioAcessoResponse;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.security.AuthorizationService;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioLocatarioAcessoService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioLocatarioAcessoRepository acessoRepository;
  private final LocatarioRepository locatarioRepository;
  private final AuthorizationService authorizationService;

  public UsuarioLocatarioAcessoService(
      UsuarioRepository usuarioRepository,
      UsuarioLocatarioAcessoRepository acessoRepository,
      LocatarioRepository locatarioRepository,
      AuthorizationService authorizationService) {
    this.usuarioRepository = usuarioRepository;
    this.acessoRepository = acessoRepository;
    this.locatarioRepository = locatarioRepository;
    this.authorizationService = authorizationService;
  }

  public UsuarioLocatarioAcessoResponse listByUsuario(Long usuarioLocalId) {
    Usuario usuario = getUsuarioDoTenantAtual(usuarioLocalId);
    List<Long> locatarioIds = acessoRepository.findLocatarioIdsByUsuarioId(usuario.getKeycloakId());
    return new UsuarioLocatarioAcessoResponse(locatarioIds);
  }

  @Transactional
  public UsuarioLocatarioAcessoResponse setByUsuario(Long usuarioLocalId, List<Long> locatarioIds) {
    authorizationService.assertCanManageTenants(authorizationService.currentUserId(), TenantContext.getTenantId());
    Usuario usuario = getUsuarioDoTenantAtual(usuarioLocalId);
    LinkedHashSet<Long> cleaned = new LinkedHashSet<>(locatarioIds == null ? List.of() : locatarioIds);
    for (Long locatarioId : cleaned) {
      if (locatarioId == null || !locatarioRepository.existsById(locatarioId)) {
        throw new EntityNotFoundException("locatario_not_found");
      }
    }
    acessoRepository.deleteAllByUsuarioIdDirect(usuario.getKeycloakId());
    cleaned.forEach(locatarioId -> acessoRepository.insertIgnore(usuario.getKeycloakId(), locatarioId));
    return new UsuarioLocatarioAcessoResponse(List.copyOf(cleaned));
  }

  @Transactional
  public UsuarioLocatarioAcessoResponse grantByUsuario(Long usuarioLocalId, Long locatarioId) {
    if (locatarioId == null) {
      throw new IllegalArgumentException("locatario_not_found");
    }
    Usuario usuario = getUsuarioDoTenantAtual(usuarioLocalId);
    if (!locatarioRepository.existsById(locatarioId)) {
      throw new EntityNotFoundException("locatario_not_found");
    }
    List<Long> current = acessoRepository.findLocatarioIdsByUsuarioId(usuario.getKeycloakId());
    LinkedHashSet<Long> next = new LinkedHashSet<>(current);
    next.add(locatarioId);
    return setByUsuario(usuarioLocalId, List.copyOf(next));
  }

  private Usuario getUsuarioDoTenantAtual(Long usuarioLocalId) {
    if (authorizationService.isCurrentGlobalMaster()) {
      return usuarioRepository.findById(usuarioLocalId)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    Usuario usuario = usuarioRepository.findById(usuarioLocalId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    if (!authorizationService.canAccessTenant(usuario.getKeycloakId(), tenantId)) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    return usuario;
  }
}
