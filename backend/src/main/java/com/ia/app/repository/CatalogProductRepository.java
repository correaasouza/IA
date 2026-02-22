package com.ia.app.repository;

import com.ia.app.domain.CatalogProduct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, Long> {

  interface CatalogGroupCountRow {
    Long getCatalogGroupId();
    Long getTotal();
  }

  Optional<CatalogProduct> findByIdAndTenantId(Long id, Long tenantId);

  Optional<CatalogProduct> findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
    Long id, Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId);

  Optional<CatalogProduct> findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
    Long id, Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId);

  boolean existsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdInAndAtivoTrue(
    Long tenantId, Long catalogConfigurationId, Collection<Long> catalogGroupIds);

  @Query("""
    select cp.catalogGroupId as catalogGroupId, count(cp.id) as total
    from CatalogProduct cp
    where cp.tenantId = :tenantId
      and cp.catalogConfigurationId = :catalogConfigurationId
      and cp.agrupadorEmpresaId = :agrupadorEmpresaId
      and cp.ativo = true
      and cp.catalogGroupId is not null
    group by cp.catalogGroupId
    """)
  List<CatalogGroupCountRow> countAtivosByGrupo(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId);

  @Query(
    value = """
      SELECT cp.*
      FROM catalog_product cp
      LEFT JOIN catalog_group cg
        ON cg.id = cp.catalog_group_id
       AND cg.tenant_id = cp.tenant_id
       AND cg.catalog_configuration_id = cp.catalog_configuration_id
      WHERE cp.tenant_id = :tenantId
        AND cp.catalog_configuration_id = :catalogConfigurationId
        AND cp.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (cast(:codigo as bigint) IS NULL AND cast(:text as varchar) IS NULL)
          OR (cast(:codigo as bigint) IS NOT NULL AND cp.codigo = cast(:codigo as bigint))
          OR (
            cast(:text as varchar) IS NOT NULL
            AND (
              lower(cp.nome) LIKE lower(concat('%', cast(:text as varchar), '%'))
              OR lower(coalesce(cp.descricao, '')) LIKE lower(concat('%', cast(:text as varchar), '%'))
            )
          )
        )
        AND (
          cast(:catalogGroupId as bigint) IS NULL
          OR cp.catalog_group_id = cast(:catalogGroupId as bigint)
          OR (cast(:groupPathPrefix as varchar) IS NOT NULL AND cg.path LIKE cast(:groupPathPrefix as varchar))
        )
        AND (cast(:ativo as boolean) IS NULL OR cp.ativo = cast(:ativo as boolean))
      ORDER BY cp.codigo ASC
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM catalog_product cp
      LEFT JOIN catalog_group cg
        ON cg.id = cp.catalog_group_id
       AND cg.tenant_id = cp.tenant_id
       AND cg.catalog_configuration_id = cp.catalog_configuration_id
      WHERE cp.tenant_id = :tenantId
        AND cp.catalog_configuration_id = :catalogConfigurationId
        AND cp.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (cast(:codigo as bigint) IS NULL AND cast(:text as varchar) IS NULL)
          OR (cast(:codigo as bigint) IS NOT NULL AND cp.codigo = cast(:codigo as bigint))
          OR (
            cast(:text as varchar) IS NOT NULL
            AND (
              lower(cp.nome) LIKE lower(concat('%', cast(:text as varchar), '%'))
              OR lower(coalesce(cp.descricao, '')) LIKE lower(concat('%', cast(:text as varchar), '%'))
            )
          )
        )
        AND (
          cast(:catalogGroupId as bigint) IS NULL
          OR cp.catalog_group_id = cast(:catalogGroupId as bigint)
          OR (cast(:groupPathPrefix as varchar) IS NOT NULL AND cg.path LIKE cast(:groupPathPrefix as varchar))
        )
        AND (cast(:ativo as boolean) IS NULL OR cp.ativo = cast(:ativo as boolean))
      """,
    nativeQuery = true)
  Page<CatalogProduct> search(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("codigo") Long codigo,
    @Param("text") String text,
    @Param("catalogGroupId") Long catalogGroupId,
    @Param("groupPathPrefix") String groupPathPrefix,
    @Param("ativo") Boolean ativo,
    Pageable pageable);

  @Modifying
  @Query("""
    update CatalogProduct item
    set item.hasStockMovements = true
    where item.tenantId = :tenantId
      and item.id = :itemId
      and item.hasStockMovements = false
    """)
  int markHasStockMovements(
    @Param("tenantId") Long tenantId,
    @Param("itemId") Long itemId);

  @Query("""
    select cp.id
    from CatalogProduct cp
    where cp.tenantId = :tenantId
      and cp.catalogConfigurationId = :catalogConfigurationId
      and cp.catalogGroupId in :catalogGroupIds
      and cp.ativo = true
    order by cp.id
    """)
  List<Long> findAtivoIdsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdIn(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("catalogGroupIds") Collection<Long> catalogGroupIds);
}
