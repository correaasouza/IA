package com.ia.app.security;

import com.ia.app.domain.Empresa;
import com.ia.app.domain.Usuario;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.UsuarioEmpresaAcessoRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.ia.app.service.PermissaoUsuarioService;
import com.ia.app.tenant.TenantContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("authorizationService")
public class AuthorizationService {

  public static final Long MASTER_TENANT_ID = 1L;
  public static final String MASTER_USERNAME = "master";

  private final UsuarioRepository usuarioRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;
  private final UsuarioEmpresaAcessoRepository usuarioEmpresaAcessoRepository;
  private final EmpresaRepository empresaRepository;
  private final PapelRepository papelRepository;
  private final PermissaoUsuarioService permissaoUsuarioService;

  public AuthorizationService(
      UsuarioRepository usuarioRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository,
      UsuarioEmpresaAcessoRepository usuarioEmpresaAcessoRepository,
      EmpresaRepository empresaRepository,
      PapelRepository papelRepository,
      PermissaoUsuarioService permissaoUsuarioService) {
    this.usuarioRepository = usuarioRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
    this.usuarioEmpresaAcessoRepository = usuarioEmpresaAcessoRepository;
    this.empresaRepository = empresaRepository;
    this.papelRepository = papelRepository;
    this.permissaoUsuarioService = permissaoUsuarioService;
  }

  public boolean isCurrentGlobalMaster() {
    return isGlobalMaster(SecurityContextHolder.getContext().getAuthentication(), TenantContext.getTenantId());
  }

  public boolean isGlobalMaster(Authentication authentication, Long activeTenantId) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    // Global master is identified by canonical username "master".
    // Keep role check as a secondary compatibility path.
    return isMasterUsername(authentication) || hasRole(authentication, "ROLE_MASTER");
  }

  public boolean isTenantMaster(String userId, Long tenantId) {
    return hasTenantRole(userId, tenantId, "MASTER");
  }

  public boolean isTenantAdmin(String userId, Long tenantId) {
    return hasTenantRole(userId, tenantId, "ADMIN");
  }

  public boolean canAccessTenant(String userId, Long tenantId) {
    if (tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isGlobalMaster(authentication, tenantId)) {
      return true;
    }
    return usuarioRepository.findByKeycloakIdAndTenantId(userId, tenantId).isPresent()
      || usuarioLocatarioAcessoRepository.existsByUsuarioIdAndLocatarioId(userId, tenantId);
  }

  public boolean canManageTenants(String userId, Long tenantId) {
    return isCurrentGlobalMaster();
  }

  public boolean canAssignUserToTenant(String userId, Long targetTenantId) {
    return canManageTenants(userId, TenantContext.getTenantId()) && targetTenantId != null;
  }

  public boolean canManageUsersInCurrentTenant() {
    Long tenantId = TenantContext.getTenantId();
    String userId = currentUserId();
    if (tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    if (isCurrentGlobalMaster()) {
      return true;
    }
    if (isTenantAdmin(userId, tenantId) || isTenantMaster(userId, tenantId)) {
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return canAccessTenant(userId, tenantId)
      && (hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_MASTER"));
  }

  public boolean canGrantCompanyAccess(String userId, Long tenantId) {
    if (tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    if (isCurrentGlobalMaster()) {
      return true;
    }
    if (isTenantAdmin(userId, tenantId) || isTenantMaster(userId, tenantId)) {
      return true;
    }
    String currentUserId = currentUserId();
    if (currentUserId == null || !currentUserId.equals(userId)) {
      return false;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return canAccessTenant(userId, tenantId)
      && (hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_MASTER"));
  }

  public Set<Long> getAccessibleCompanies(String userId, Long tenantId) {
    if (tenantId == null || userId == null || userId.isBlank()) {
      return Set.of();
    }
    if (isCurrentGlobalMaster() || isTenantAdmin(userId, tenantId) || isTenantMaster(userId, tenantId)) {
      return empresaRepository.findAllByTenantIdOrderByRazaoSocialAsc(tenantId).stream()
        .map(Empresa::getId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
    String currentUserId = currentUserId();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (currentUserId != null
        && currentUserId.equals(userId)
        && canAccessTenant(userId, tenantId)
        && (hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_MASTER"))) {
      return empresaRepository.findAllByTenantIdOrderByRazaoSocialAsc(tenantId).stream()
        .map(Empresa::getId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
    return new LinkedHashSet<>(usuarioEmpresaAcessoRepository.findEmpresaIdsByTenantIdAndUsuarioId(tenantId, userId));
  }

  public boolean canSetDefaultCompany(String userId, Long tenantId, Long companyId) {
    if (companyId == null || tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    if (!empresaRepository.existsByIdAndTenantId(companyId, tenantId)) {
      return false;
    }
    return getAccessibleCompanies(userId, tenantId).contains(companyId);
  }

  public void assertMasterInvariant(Usuario targetUser, List<Long> papelIds) {
    if (targetUser == null) {
      return;
    }
    if (!MASTER_USERNAME.equalsIgnoreCase(targetUser.getUsername())) {
      return;
    }
    if (!MASTER_TENANT_ID.equals(targetUser.getTenantId())) {
      return;
    }
    if (papelIds == null || papelIds.isEmpty()) {
      throw new IllegalArgumentException("usuario_master_role_cannot_be_removed");
    }
    boolean hasMaster = papelIds.stream()
      .map(id -> id == null ? null : findRoleNameById(id))
      .filter(java.util.Objects::nonNull)
      .anyMatch(nome -> "MASTER".equalsIgnoreCase(nome));
    if (!hasMaster) {
      throw new IllegalArgumentException("usuario_master_role_cannot_be_removed");
    }
  }

  public String currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getSubject();
    }
    return auth.getName();
  }

  public Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  public void assertTenantAccess(String userId, Long tenantId) {
    if (!canAccessTenant(userId, tenantId)) {
      throw new IllegalStateException("tenant_forbidden");
    }
  }

  public void assertCanGrantCompanyAccess(String userId, Long tenantId) {
    if (!canGrantCompanyAccess(userId, tenantId)) {
      throw new IllegalStateException("role_required_admin_or_master");
    }
  }

  public void assertCanManageTenants(String userId, Long tenantId) {
    if (!canManageTenants(userId, tenantId)) {
      throw new IllegalStateException("role_required_user_master");
    }
  }

  private boolean hasTenantRole(String userId, Long tenantId, String role) {
    if (tenantId == null || userId == null || userId.isBlank()) {
      return false;
    }
    return permissaoUsuarioService.papeis(tenantId, userId).stream()
      .anyMatch(existing -> role.equalsIgnoreCase(existing));
  }

  private String findRoleNameById(Long roleId) {
    return papelRepository.findById(roleId).map(p -> p.getNome()).orElse(null);
  }

  private boolean hasRole(Authentication authentication, String role) {
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (role.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  private boolean isMasterUsername(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
      return preferredUsername != null && preferredUsername.equalsIgnoreCase(MASTER_USERNAME);
    }
    String name = authentication.getName();
    return name != null && name.equalsIgnoreCase(MASTER_USERNAME);
  }
}
