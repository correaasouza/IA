package com.ia.app.repository;

import com.ia.app.domain.CatalogMovementLine;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogMovementLineRepository extends JpaRepository<CatalogMovementLine, Long> {

  List<CatalogMovementLine> findAllByTenantIdAndMovementIdInOrderByMovementIdAscIdAsc(
    Long tenantId,
    Collection<Long> movementIds);

  List<CatalogMovementLine> findAllByTenantIdAndMovementIdOrderByIdAsc(Long tenantId, Long movementId);
}
