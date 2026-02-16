package com.ia.app.repository;

import com.ia.app.domain.CatalogStockAdjustment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogStockAdjustmentRepository extends JpaRepository<CatalogStockAdjustment, Long> {

  List<CatalogStockAdjustment> findAllByTenantIdAndCatalogConfigurationIdOrderByOrdemAscNomeAsc(
    Long tenantId,
    Long catalogConfigurationId);

  Optional<CatalogStockAdjustment> findByIdAndTenantIdAndCatalogConfigurationId(
    Long id,
    Long tenantId,
    Long catalogConfigurationId);

  @Query("""
    select coalesce(max(a.ordem), 0)
    from CatalogStockAdjustment a
    where a.tenantId = :tenantId
      and a.catalogConfigurationId = :catalogConfigurationId
    """)
  Integer maxOrdemByScope(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId);

  @Query(
    value = """
      select coalesce(max(cast(a.codigo as bigint)), 0)
      from catalog_stock_adjustment a
      where a.tenant_id = :tenantId
        and a.codigo ~ '^[0-9]+$'
      """,
    nativeQuery = true)
  Long maxNumericCodigoByTenant(@Param("tenantId") Long tenantId);
}
