package com.ia.app.repository;

import com.ia.app.domain.PriceChangeLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceChangeLogRepository extends JpaRepository<PriceChangeLog, Long> {

  List<PriceChangeLog> findAllByTenantIdAndCatalogTypeAndCatalogItemIdOrderByChangedAtDesc(
    Long tenantId,
    com.ia.app.domain.CatalogConfigurationType catalogType,
    Long catalogItemId);
}
