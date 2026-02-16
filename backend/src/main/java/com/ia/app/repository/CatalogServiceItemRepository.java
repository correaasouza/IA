package com.ia.app.repository;

import com.ia.app.domain.CatalogServiceItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogServiceItemRepository extends JpaRepository<CatalogServiceItem, Long> {

  interface CatalogGroupCountRow {
    Long getCatalogGroupId();
    Long getTotal();
  }

  Optional<CatalogServiceItem> findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
    Long id, Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId);

  boolean existsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdInAndAtivoTrue(
    Long tenantId, Long catalogConfigurationId, Collection<Long> catalogGroupIds);

  @Query("""
    select cs.catalogGroupId as catalogGroupId, count(cs.id) as total
    from CatalogServiceItem cs
    where cs.tenantId = :tenantId
      and cs.catalogConfigurationId = :catalogConfigurationId
      and cs.agrupadorEmpresaId = :agrupadorEmpresaId
      and cs.ativo = true
      and cs.catalogGroupId is not null
    group by cs.catalogGroupId
    """)
  List<CatalogGroupCountRow> countAtivosByGrupo(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId);

  @Query(
    value = """
      SELECT cs.*
      FROM catalog_service_item cs
      WHERE cs.tenant_id = :tenantId
        AND cs.catalog_configuration_id = :catalogConfigurationId
        AND cs.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (:codigo IS NULL AND :text IS NULL)
          OR (:codigo IS NOT NULL AND cs.codigo = :codigo)
          OR (
            :text IS NOT NULL
            AND (
              lower(cs.nome) LIKE lower(concat('%', :text, '%'))
              OR lower(coalesce(cs.descricao, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
        )
        AND (:catalogGroupId IS NULL OR cs.catalog_group_id = :catalogGroupId)
        AND (:ativo IS NULL OR cs.ativo = :ativo)
      ORDER BY cs.codigo ASC
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM catalog_service_item cs
      WHERE cs.tenant_id = :tenantId
        AND cs.catalog_configuration_id = :catalogConfigurationId
        AND cs.agrupador_empresa_id = :agrupadorEmpresaId
        AND (
          (:codigo IS NULL AND :text IS NULL)
          OR (:codigo IS NOT NULL AND cs.codigo = :codigo)
          OR (
            :text IS NOT NULL
            AND (
              lower(cs.nome) LIKE lower(concat('%', :text, '%'))
              OR lower(coalesce(cs.descricao, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
        )
        AND (:catalogGroupId IS NULL OR cs.catalog_group_id = :catalogGroupId)
        AND (:ativo IS NULL OR cs.ativo = :ativo)
      """,
    nativeQuery = true)
  Page<CatalogServiceItem> search(
    @Param("tenantId") Long tenantId,
    @Param("catalogConfigurationId") Long catalogConfigurationId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("codigo") Long codigo,
    @Param("text") String text,
    @Param("catalogGroupId") Long catalogGroupId,
    @Param("ativo") Boolean ativo,
    Pageable pageable);
}
