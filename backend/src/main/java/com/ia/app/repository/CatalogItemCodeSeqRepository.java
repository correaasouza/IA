package com.ia.app.repository;

import com.ia.app.domain.CatalogItemCodeSeq;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CatalogItemCodeSeqRepository extends JpaRepository<CatalogItemCodeSeq, Long> {
  Optional<CatalogItemCodeSeq> findByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
    Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CatalogItemCodeSeq> findWithLockByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
    Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId);
}
