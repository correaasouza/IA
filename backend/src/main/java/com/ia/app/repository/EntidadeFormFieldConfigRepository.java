package com.ia.app.repository;

import com.ia.app.domain.EntidadeFormFieldConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeFormFieldConfigRepository extends JpaRepository<EntidadeFormFieldConfig, Long> {
  List<EntidadeFormFieldConfig> findAllByTenantIdAndGroupConfigIdInOrderByOrdemAscIdAsc(Long tenantId, List<Long> groupConfigIds);

  Optional<EntidadeFormFieldConfig> findByTenantIdAndGroupConfigIdAndFieldKey(Long tenantId, Long groupConfigId, String fieldKey);
}

