package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogStockBalance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogStockBalanceRepository extends JpaRepository<CatalogStockBalance, Long> {

  interface StockTypeConsolidatedRow {
    Long getEstoqueTipoId();

    java.math.BigDecimal getQuantidadeTotal();

    java.math.BigDecimal getPrecoTotal();
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<CatalogStockBalance> findWithLockByTenantIdAndCatalogTypeAndCatalogoIdAndAgrupadorEmpresaIdAndEstoqueTipoIdAndFilialId(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogoId,
    Long agrupadorEmpresaId,
    Long estoqueTipoId,
    Long filialId);

  List<CatalogStockBalance> findAllByTenantIdAndCatalogTypeAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndFilialId(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId,
    Long filialId);

  @Query("""
    select b
    from CatalogStockBalance b
    where b.tenantId = :tenantId
      and b.catalogType = :catalogType
      and b.catalogoId = :catalogoId
      and b.agrupadorEmpresaId = :agrupadorEmpresaId
      and (:estoqueTipoId is null or b.estoqueTipoId = :estoqueTipoId)
      and (:filialId is null or b.filialId = :filialId)
    order by b.estoqueTipoId asc, b.filialId asc
    """)
  List<CatalogStockBalance> listByFilters(
    @Param("tenantId") Long tenantId,
    @Param("catalogType") CatalogConfigurationType catalogType,
    @Param("catalogoId") Long catalogoId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("estoqueTipoId") Long estoqueTipoId,
    @Param("filialId") Long filialId);

  @Query("""
    select
      b.estoqueTipoId as estoqueTipoId,
      sum(b.quantidadeAtual) as quantidadeTotal,
      sum(b.precoAtual) as precoTotal
    from CatalogStockBalance b
    where b.tenantId = :tenantId
      and b.catalogType = :catalogType
      and b.catalogoId = :catalogoId
      and b.agrupadorEmpresaId = :agrupadorEmpresaId
      and (:estoqueTipoId is null or b.estoqueTipoId = :estoqueTipoId)
    group by b.estoqueTipoId
    """)
  List<StockTypeConsolidatedRow> consolidatedByStockType(
    @Param("tenantId") Long tenantId,
    @Param("catalogType") CatalogConfigurationType catalogType,
    @Param("catalogoId") Long catalogoId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("estoqueTipoId") Long estoqueTipoId);
}
