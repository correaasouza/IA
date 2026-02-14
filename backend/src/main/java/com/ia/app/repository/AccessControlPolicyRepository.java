package com.ia.app.repository;

import com.ia.app.domain.AccessControlPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessControlPolicyRepository extends JpaRepository<AccessControlPolicy, Long> {
  List<AccessControlPolicy> findAllByTenantIdOrderByControlKeyAsc(Long tenantId);
  Optional<AccessControlPolicy> findByTenantIdAndControlKey(Long tenantId, String controlKey);
  void deleteByTenantIdAndControlKey(Long tenantId, String controlKey);
}

