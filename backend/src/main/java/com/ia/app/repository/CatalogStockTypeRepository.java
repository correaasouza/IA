package com.ia.app.repository;

import com.ia.app.domain.CatalogStockType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogStockTypeRepository extends JpaRepository<CatalogStockType, Long> {

  List<CatalogStockType> findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId);

  List<CatalogStockType> findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdOrderByOrdemAscNomeAsc(
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId);

  Optional<CatalogStockType> findByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndCodigoAndActiveTrue(
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId,
    String codigo);

  Optional<CatalogStockType> findByIdAndTenantId(Long id, Long tenantId);

  Optional<CatalogStockType> findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrue(
    Long id,
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId);

  Optional<CatalogStockType> findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
    Long id,
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId);

  List<CatalogStockType> findAllByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);

  long countByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrue(
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId);

  @Query("""
    select coalesce(max(s.ordem), 0)
    from CatalogStockType s
    where s.tenantId = :tenantId
      and s.catalogConfigurationId = :catalogConfigurationId
      and s.agrupadorEmpresaId = :agrupadorEmpresaId
    """)
  Integer maxOrdemByScope(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId);
}
