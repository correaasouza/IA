package com.ia.app.service;

import com.ia.app.domain.Empresa;
import com.ia.app.domain.Usuario;
import com.ia.app.domain.UsuarioEmpresaAcesso;
import com.ia.app.dto.EmpresaResponse;
import com.ia.app.dto.UserCompanyAccessSummaryResponse;
import com.ia.app.dto.UsuarioEmpresaAcessoResponse;
import com.ia.app.mapper.EmpresaMapper;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.UsuarioEmpresaAcessoRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.security.AuthorizationService;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioEmpresaAcessoService {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioEmpresaAcessoRepository acessoRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;
  private final EmpresaRepository empresaRepository;
  private final UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService;
  private final AuthorizationService authorizationService;

  public UsuarioEmpresaAcessoService(
      UsuarioRepository usuarioRepository,
      UsuarioEmpresaAcessoRepository acessoRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository,
      EmpresaRepository empresaRepository,
      UsuarioEmpresaPreferenciaService usuarioEmpresaPreferenciaService,
      AuthorizationService authorizationService) {
    this.usuarioRepository = usuarioRepository;
    this.acessoRepository = acessoRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
    this.empresaRepository = empresaRepository;
    this.usuarioEmpresaPreferenciaService = usuarioEmpresaPreferenciaService;
    this.authorizationService = authorizationService;
  }

  public UsuarioEmpresaAcessoResponse listByUsuario(Long usuarioLocalId) {
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Set<Long> allowedTenantIds = resolveAllowedTenantIdsForTarget(usuario);
    LinkedHashSet<Long> empresaIds = new LinkedHashSet<>();
    for (Long tenantId : allowedTenantIds) {
      empresaIds.addAll(authorizationService.getAccessibleCompanies(usuario.getKeycloakId(), tenantId));
    }
    return new UsuarioEmpresaAcessoResponse(List.copyOf(empresaIds));
  }

  public UserCompanyAccessSummaryResponse summaryByUsuario(Long usuarioLocalId) {
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Long tenantId = requireUserTenantLink(usuario);
    List<Long> accessible = List.copyOf(authorizationService.getAccessibleCompanies(usuario.getKeycloakId(), tenantId));
    Long defaultCompanyId = usuarioEmpresaPreferenciaService.getEmpresaPadraoId(usuario.getKeycloakId());
    boolean canGrant = authorizationService.canGrantCompanyAccess(authorizationService.currentUserId(), tenantId);
    return new UserCompanyAccessSummaryResponse(accessible, defaultCompanyId, canGrant);
  }

  @Transactional
  public UsuarioEmpresaAcessoResponse setByUsuario(Long usuarioLocalId, List<Long> empresaIds) {
    String actorId = authorizationService.currentUserId();
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Set<Long> allowedTenantIds = resolveAllowedTenantIdsForTarget(usuario);
    for (Long tenantId : allowedTenantIds) {
      authorizationService.assertCanGrantCompanyAccess(actorId, tenantId);
    }
    LinkedHashSet<Long> cleaned = new LinkedHashSet<>(empresaIds == null ? List.of() : empresaIds);

    Map<Long, Empresa> empresasById = new HashMap<>();
    if (!cleaned.isEmpty()) {
      List<Empresa> empresas = empresaRepository.findAllById(cleaned);
      for (Empresa empresa : empresas) {
        empresasById.put(empresa.getId(), empresa);
      }
    }

    for (Long empresaId : cleaned) {
      Empresa empresa = empresasById.get(empresaId);
      if (empresa == null) {
        throw new EntityNotFoundException("empresa_not_found");
      }
      if (!allowedTenantIds.contains(empresa.getTenantId())) {
        throw new IllegalArgumentException("empresa_not_allowed_for_user");
      }
    }

    for (Long tenantId : allowedTenantIds) {
      acessoRepository.deleteAllByTenantIdAndUsuarioId(tenantId, usuario.getKeycloakId());
    }
    for (Long empresaId : cleaned) {
      Empresa empresa = empresasById.get(empresaId);
      UsuarioEmpresaAcesso acesso = new UsuarioEmpresaAcesso();
      acesso.setTenantId(empresa.getTenantId());
      acesso.setUsuarioId(usuario.getKeycloakId());
      acesso.setEmpresaId(empresaId);
      acessoRepository.save(acesso);
    }

    Long empresaPadrao = usuarioEmpresaPreferenciaService.getEmpresaPadraoId(usuario.getKeycloakId());
    if (empresaPadrao != null && !cleaned.contains(empresaPadrao)) {
      usuarioEmpresaPreferenciaService.clearEmpresaPadraoId(usuario.getKeycloakId());
    }

    return new UsuarioEmpresaAcessoResponse(List.copyOf(cleaned));
  }

  public List<EmpresaResponse> listDisponiveis(Long usuarioLocalId) {
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Set<Long> allowedTenantIds = resolveAllowedTenantIdsForTarget(usuario);
    List<Empresa> empresas = new ArrayList<>();
    for (Long tenantId : allowedTenantIds) {
      empresas.addAll(empresaRepository.findAllByTenantIdAndAtivoTrueOrderByRazaoSocialAsc(tenantId));
    }
    return empresas.stream().map(EmpresaMapper::toResponse).toList();
  }

  public Long getEmpresaPadraoByUsuario(Long usuarioLocalId) {
    Usuario usuario = resolveUsuario(usuarioLocalId);
    requireUserTenantLink(usuario);
    return usuarioEmpresaPreferenciaService.getEmpresaPadraoId(usuario.getKeycloakId());
  }

  public Long setEmpresaPadraoByUsuario(Long usuarioLocalId, Long empresaId) {
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Long tenantId = requireUserTenantLink(usuario);
    if (!isAuthenticatedUser(usuario.getKeycloakId())
        && !authorizationService.canGrantCompanyAccess(authorizationService.currentUserId(), tenantId)) {
      throw new IllegalStateException("forbidden");
    }
    return usuarioEmpresaPreferenciaService.setEmpresaPadraoId(usuario.getKeycloakId(), empresaId);
  }

  @Transactional
  public UserCompanyAccessSummaryResponse grantCompanyAccess(Long usuarioLocalId, Long companyId) {
    String actorId = authorizationService.currentUserId();
    Usuario usuario = resolveUsuario(usuarioLocalId);
    Long tenantId = requireUserTenantLink(usuario);
    authorizationService.assertCanGrantCompanyAccess(actorId, tenantId);
    if (!empresaRepository.existsByIdAndTenantId(companyId, tenantId)) {
      throw new EntityNotFoundException("empresa_not_found");
    }
    List<Long> currentIds = acessoRepository.findEmpresaIdsByTenantIdAndUsuarioId(tenantId, usuario.getKeycloakId());
    LinkedHashSet<Long> nextIds = new LinkedHashSet<>(currentIds);
    nextIds.add(companyId);
    setByUsuario(usuarioLocalId, List.copyOf(nextIds));
    return summaryByUsuario(usuarioLocalId);
  }

  private Usuario resolveUsuario(Long usuarioLocalId) {
    if (authorizationService.isCurrentGlobalMaster()) {
      return usuarioRepository.findById(usuarioLocalId)
        .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    }
    Long tenantId = requireTenant();
    Usuario usuario = usuarioRepository.findById(usuarioLocalId)
      .orElseThrow(() -> new EntityNotFoundException("usuario_not_found"));
    if (!authorizationService.canAccessTenant(usuario.getKeycloakId(), tenantId)) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    return usuario;
  }

  private boolean isAuthenticatedUser(String keycloakId) {
    String usuarioId = authorizationService.currentUserId();
    return usuarioId != null && usuarioId.equals(keycloakId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Long requireUserTenantLink(Usuario usuario) {
    Long tenantId = requireTenant();
    if (usuario == null) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    if (!authorizationService.canAccessTenant(usuario.getKeycloakId(), tenantId)) {
      throw new IllegalStateException("tenant_forbidden");
    }
    return tenantId;
  }

  private Set<Long> resolveAllowedTenantIdsForTarget(Usuario usuario) {
    if (usuario == null) {
      throw new EntityNotFoundException("usuario_not_found");
    }
    if (authorizationService.isCurrentGlobalMaster()) {
      LinkedHashSet<Long> tenantIds = new LinkedHashSet<>();
      if (usuario.getTenantId() != null) {
        tenantIds.add(usuario.getTenantId());
      }
      tenantIds.addAll(usuarioLocatarioAcessoRepository.findLocatarioIdsByUsuarioId(usuario.getKeycloakId()));
      return tenantIds;
    }
    Long tenantId = requireUserTenantLink(usuario);
    return Set.of(tenantId);
  }

}
