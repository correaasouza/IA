package com.ia.app.service;

import com.ia.app.domain.AccessControlPolicy;
import com.ia.app.dto.AccessControlPolicyResponse;
import com.ia.app.repository.AccessControlPolicyRepository;
import com.ia.app.tenant.TenantContext;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessControlPolicyService {

  private final AccessControlPolicyRepository repository;

  public AccessControlPolicyService(AccessControlPolicyRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public List<AccessControlPolicyResponse> list() {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdOrderByControlKeyAsc(tenantId).stream()
      .map(this::sanitizeAndSaveIfNeeded)
      .map(this::toResponse)
      .toList();
  }

  @Transactional
  public AccessControlPolicyResponse upsert(String controlKey, List<String> roles) {
    Long tenantId = requireTenant();
    String key = normalizeKey(controlKey);
    if (key.isEmpty()) throw new IllegalArgumentException("access_control_key_required");
    String csv = normalizeRoles(roles).stream().collect(Collectors.joining(","));
    AccessControlPolicy entity = repository.findByTenantIdAndControlKey(tenantId, key)
      .orElseGet(() -> {
        AccessControlPolicy p = new AccessControlPolicy();
        p.setTenantId(tenantId);
        p.setControlKey(key);
        return p;
      });
    entity.setRolesCsv(csv);
    return toResponse(repository.save(entity));
  }

  @Transactional
  public void delete(String controlKey) {
    Long tenantId = requireTenant();
    String key = normalizeKey(controlKey);
    if (key.isEmpty()) return;
    repository.deleteByTenantIdAndControlKey(tenantId, key);
  }

  private AccessControlPolicyResponse toResponse(AccessControlPolicy entity) {
    return new AccessControlPolicyResponse(
      entity.getControlKey(),
      parseCsv(entity.getRolesCsv())
    );
  }

  private List<String> normalizeRoles(List<String> roles) {
    return (roles == null ? List.<String>of() : roles).stream()
      .map(this::normalizeRole)
      .filter(r -> !r.isBlank())
      .distinct()
      .toList();
  }

  private List<String> parseCsv(String csv) {
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(","))
      .map(this::normalizeRole)
      .filter(s -> !s.isBlank())
      .distinct()
      .toList();
  }

  private String normalizeRole(String role) {
    String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      return "";
    }
    int sepIdx = normalized.lastIndexOf(':');
    if (sepIdx >= 0 && sepIdx < normalized.length() - 1) {
      normalized = normalized.substring(sepIdx + 1);
    }
    if (normalized.startsWith("ROLE_") && normalized.length() > 5) {
      normalized = normalized.substring(5);
    }
    if (isTechnicalRole(normalized)) {
      return "";
    }
    return normalized;
  }

  private boolean isTechnicalRole(String role) {
    if (role == null || role.isBlank()) {
      return true;
    }
    String normalized = role.trim().toUpperCase(Locale.ROOT);
    if (normalized.startsWith("DEFAULT-ROLES-")) {
      return true;
    }
    return normalized.equals("OFFLINE_ACCESS")
      || normalized.equals("UMA_AUTHORIZATION")
      || normalized.equals("MANAGE-ACCOUNT")
      || normalized.equals("MANAGE-ACCOUNT-LINKS")
      || normalized.equals("VIEW-PROFILE");
  }

  private AccessControlPolicy sanitizeAndSaveIfNeeded(AccessControlPolicy entity) {
    List<String> normalized = parseCsv(entity.getRolesCsv());
    String normalizedCsv = String.join(",", normalized);
    if (!normalizedCsv.equals(entity.getRolesCsv())) {
      entity.setRolesCsv(normalizedCsv);
      return repository.save(entity);
    }
    return entity;
  }

  private String normalizeKey(String key) {
    return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }
}

