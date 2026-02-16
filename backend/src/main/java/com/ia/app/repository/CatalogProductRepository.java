package com.ia.app.repository;

import com.ia.app.domain.CatalogProduct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
      WHERE cp.tenant_id = :tenantId
        AND cp.catalog_configuration_id = :catalogConfigurationId
        AND cp.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (:codigo IS NULL AND :text IS NULL)
          OR (:codigo IS NOT NULL AND cp.codigo = :codigo)
          OR (
            :text IS NOT NULL
            AND (
              lower(cp.nome) LIKE lower(concat('%', :text, '%'))
              OR lower(coalesce(cp.descricao, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
        )
        AND (:catalogGroupId IS NULL OR cp.catalog_group_id = :catalogGroupId)
        AND (:ativo IS NULL OR cp.ativo = :ativo)
      ORDER BY cp.codigo ASC
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM catalog_product cp
      WHERE cp.tenant_id = :tenantId
        AND cp.catalog_configuration_id = :catalogConfigurationId
        AND cp.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (:codigo IS NULL AND :text IS NULL)
          OR (:codigo IS NOT NULL AND cp.codigo = :codigo)
          OR (
            :text IS NOT NULL
            AND (
              lower(cp.nome) LIKE lower(concat('%', :text, '%'))
              OR lower(coalesce(cp.descricao, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
        )
        AND (:catalogGroupId IS NULL OR cp.catalog_group_id = :catalogGroupId)
        AND (:ativo IS NULL OR cp.ativo = :ativo)
      """,
    nativeQuery = true)
  Page<CatalogProduct> search(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("codigo") Long codigo,
    @Param("text") String text,
    @Param("catalogGroupId") Long catalogGroupId,
    @Param("ativo") Boolean ativo,
    Pageable pageable);
}
