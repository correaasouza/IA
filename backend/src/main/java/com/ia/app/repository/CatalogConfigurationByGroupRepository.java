package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationByGroup;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogConfigurationByGroupRepository extends JpaRepository<CatalogConfigurationByGroup, Long> {
  List<CatalogConfigurationByGroup> findAllByTenantIdAndCatalogConfigurationIdAndActiveTrue(
    Long tenantId, Long catalogConfigurationId);

  Optional<CatalogConfigurationByGroup> findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
    Long tenantId, Long catalogConfigurationId, Long agrupadorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CatalogConfigurationByGroup> findWithLockByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
    Long tenantId, Long catalogConfigurationId, Long agrupadorId);
}
