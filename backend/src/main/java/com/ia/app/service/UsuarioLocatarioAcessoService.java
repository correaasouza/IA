package com.ia.app.service;

import com.ia.app.domain.Usuario;
import com.ia.app.dto.UsuarioLocatarioAcessoResponse;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioLocatarioAcessoService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioLocatarioAcessoRepository acessoRepository;
  private final LocatarioRepository locatarioRepository;

  public UsuarioLocatarioAcessoService(
      UsuarioRepository usuarioRepository,
      UsuarioLocatarioAcessoRepository acessoRepository,
      LocatarioRepository locatarioRepository) {
    this.usuarioRepository = usuarioRepository;
    this.acessoRepository = acessoRepository;
    this.locatarioRepository = locatarioRepository;
  }

  public UsuarioLocatarioAcessoResponse listByUsuario(Long usuarioLocalId) {
    Usuario usuario = getUsuarioDoTenantAtual(usuarioLocalId);
    List<Long> locatarioIds = acessoRepository.findLocatarioIdsByUsuarioId(usuario.getKeycloakId());
    return new UsuarioLocatarioAcessoResponse(locatarioIds);
  }

  @Transactional
  public UsuarioLocatarioAcessoResponse setByUsuario(Long usuarioLocalId, List<Long> locatarioIds) {
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

  private Usuario getUsuarioDoTenantAtual(Long usuarioLocalId) {
    if (isLocatarioMaster() || isUsuarioMaster()) {
      return usuarioRepository.findById(usuarioLocalId)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return usuarioRepository.findByIdAndTenantId(usuarioLocalId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
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
}
