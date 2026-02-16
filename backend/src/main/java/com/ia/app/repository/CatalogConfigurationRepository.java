package com.ia.app.repository;

import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogConfigurationRepository extends JpaRepository<CatalogConfiguration, Long> {
  Optional<CatalogConfiguration> findByTenantIdAndType(Long tenantId, CatalogConfigurationType type);

  boolean existsByIdAndTenantId(Long id, Long tenantId);

  boolean existsByTenantIdAndType(Long tenantId, CatalogConfigurationType type);
}
