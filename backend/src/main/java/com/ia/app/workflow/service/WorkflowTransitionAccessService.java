package com.ia.app.workflow.service;

import com.ia.app.domain.AccessControlPolicy;
import com.ia.app.repository.AccessControlPolicyRepository;
import com.ia.app.workflow.domain.WorkflowDefinition;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class WorkflowTransitionAccessService {

  private final AccessControlPolicyRepository accessControlPolicyRepository;

  public WorkflowTransitionAccessService(AccessControlPolicyRepository accessControlPolicyRepository) {
    this.accessControlPolicyRepository = accessControlPolicyRepository;
  }

  public String buildControlKey(WorkflowDefinition definition, String transitionKey) {
    String origin = normalizeToken(definition == null || definition.getOrigin() == null
      ? "unknown"
      : definition.getOrigin().name());
    String scope = "global";
    if (definition != null
      && definition.getContextType() != null
      && definition.getContextId() != null
      && definition.getContextId() > 0) {
      scope = normalizeToken(definition.getContextType().name()) + "." + definition.getContextId();
    }
    String transition = normalizeToken(transitionKey);
    if (transition.isBlank()) {
      transition = "transition";
    }
    if (transition.length() > 80) {
      transition = transition.substring(0, 80);
    }
    return "workflow.transition." + origin + "." + scope + "." + transition;
  }

  public boolean canExecute(Long tenantId, String controlKey, Authentication authentication) {
    if (tenantId == null || controlKey == null || controlKey.isBlank()) {
      return true;
    }
    if (isMasterTenantContext(authentication, tenantId)) {
      return true;
    }
    List<String> requiredRoles = accessControlPolicyRepository
      .findByTenantIdAndControlKey(tenantId, normalizeKey(controlKey))
      .map(AccessControlPolicy::getRolesCsv)
      .map(this::parseRolesCsv)
      .orElse(List.of());
    if (requiredRoles.isEmpty()) {
      return true;
    }
    Set<String> userRoles = extractUserRoles(authentication);
    if (userRoles.isEmpty()) {
      return false;
    }
    return requiredRoles.stream().anyMatch(userRoles::contains);
  }

  private Set<String> extractUserRoles(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Set.of();
    }
    Set<String> roles = new LinkedHashSet<>();
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (authority == null || authority.getAuthority() == null) {
        continue;
      }
      String normalized = authority.getAuthority().trim().toUpperCase(Locale.ROOT);
      if (normalized.startsWith("ROLE_")) {
        normalized = normalized.substring(5);
      }
      if (!normalized.isBlank()) {
        roles.add(normalized);
      }
    }
    return roles;
  }

  private List<String> parseRolesCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(","))
      .map(value -> value == null ? "" : value.trim().toUpperCase(Locale.ROOT))
      .filter(value -> !value.isBlank())
      .distinct()
      .toList();
  }

  private boolean isMasterTenantContext(Authentication authentication, Long tenantId) {
    if (tenantId == null || tenantId != 1L || authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    String username = authentication.getName();
    if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      Object preferredUsername = jwtAuthenticationToken.getToken().getClaims().get("preferred_username");
      if (preferredUsername != null) {
        username = preferredUsername.toString();
      }
    }
    return username != null && username.trim().equalsIgnoreCase("master");
  }

  private String normalizeToken(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.replaceAll("[^a-z0-9._-]", "_");
  }

  private String normalizeKey(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
