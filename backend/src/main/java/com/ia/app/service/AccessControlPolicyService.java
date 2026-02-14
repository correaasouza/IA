package com.ia.app.service;

import com.ia.app.domain.AccessControlPolicy;
import com.ia.app.dto.AccessControlPolicyResponse;
import com.ia.app.repository.AccessControlPolicyRepository;
import com.ia.app.tenant.TenantContext;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessControlPolicyService {

  private final AccessControlPolicyRepository repository;

  public AccessControlPolicyService(AccessControlPolicyRepository repository) {
    this.repository = repository;
  }

  public List<AccessControlPolicyResponse> list() {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdOrderByControlKeyAsc(tenantId).stream()
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
      .map(r -> r == null ? "" : r.trim().toUpperCase(Locale.ROOT))
      .filter(r -> !r.isBlank())
      .distinct()
      .toList();
  }

  private List<String> parseCsv(String csv) {
    if (csv == null || csv.isBlank()) return List.of();
    return Arrays.stream(csv.split(","))
      .map(String::trim)
      .filter(s -> !s.isBlank())
      .distinct()
      .toList();
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

