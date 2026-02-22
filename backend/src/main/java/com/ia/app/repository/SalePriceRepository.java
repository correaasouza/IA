package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.SalePrice;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalePriceRepository extends JpaRepository<SalePrice, Long> {

  interface SalePriceGridRowProjection {
    Long getId();
    Long getPriceBookId();
    Long getVariantId();
    String getCatalogType();
    Long getCatalogItemId();
    UUID getTenantUnitId();
    BigDecimal getPriceFinal();
    String getCatalogItemName();
    String getCatalogGroupName();
    BigDecimal getCatalogBasePrice();
  }

  Optional<SalePrice> findByIdAndTenantId(Long id, Long tenantId);

  @Query(
    value = """
      SELECT
        sp.id AS id,
        CAST(:priceBookId AS bigint) AS priceBookId,
        CAST(:variantId AS bigint) AS variantId,
        base.catalog_type AS catalogType,
        base.catalog_item_id AS catalogItemId,
        sp.tenant_unit_id AS tenantUnitId,
        sp.price_final AS priceFinal,
        base.catalog_item_name AS catalogItemName,
        base.catalog_group_name AS catalogGroupName,
        base.catalog_base_price AS catalogBasePrice
      FROM (
        SELECT
          'PRODUCTS' AS catalog_type,
          cp.id AS catalog_item_id,
          cp.nome AS catalog_item_name,
          cpg.nome AS catalog_group_name,
          cip.price_final AS catalog_base_price
        FROM catalog_product cp
        LEFT JOIN catalog_group cpg
          ON cpg.id = cp.catalog_group_id
         AND cpg.tenant_id = cp.tenant_id
        LEFT JOIN catalog_group cpg_root
          ON cpg_root.id = :catalogGroupId
         AND cpg_root.tenant_id = cp.tenant_id
        LEFT JOIN catalog_item_price cip
          ON cip.tenant_id = cp.tenant_id
         AND cip.catalog_type = 'PRODUCTS'
         AND cip.catalog_item_id = cp.id
         AND cip.price_type = 'SALE_BASE'
        WHERE cp.tenant_id = :tenantId
          AND cp.ativo = true
          AND (:catalogType IS NULL OR :catalogType = 'PRODUCTS')
          AND (
            (:catalogItemId IS NULL AND :text IS NULL)
            OR (:catalogItemId IS NOT NULL AND cp.id = :catalogItemId)
            OR (
              :text IS NOT NULL
              AND lower(COALESCE(cp.nome, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
          AND (
            :catalogGroupId IS NULL
            OR cp.catalog_group_id = :catalogGroupId
            OR (
              :includeGroupChildren = true
              AND cpg.path LIKE concat(cpg_root.path, '/%')
            )
          )
        UNION ALL
        SELECT
          'SERVICES' AS catalog_type,
          cs.id AS catalog_item_id,
          cs.nome AS catalog_item_name,
          csg.nome AS catalog_group_name,
          cis.price_final AS catalog_base_price
        FROM catalog_service_item cs
        LEFT JOIN catalog_group csg
          ON csg.id = cs.catalog_group_id
         AND csg.tenant_id = cs.tenant_id
        LEFT JOIN catalog_group csg_root
          ON csg_root.id = :catalogGroupId
         AND csg_root.tenant_id = cs.tenant_id
        LEFT JOIN catalog_item_price cis
          ON cis.tenant_id = cs.tenant_id
         AND cis.catalog_type = 'SERVICES'
         AND cis.catalog_item_id = cs.id
         AND cis.price_type = 'SALE_BASE'
        WHERE cs.tenant_id = :tenantId
          AND cs.ativo = true
          AND (:catalogType IS NULL OR :catalogType = 'SERVICES')
          AND (
            (:catalogItemId IS NULL AND :text IS NULL)
            OR (:catalogItemId IS NOT NULL AND cs.id = :catalogItemId)
            OR (
              :text IS NOT NULL
              AND lower(COALESCE(cs.nome, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
          AND (
            :catalogGroupId IS NULL
            OR cs.catalog_group_id = :catalogGroupId
            OR (
              :includeGroupChildren = true
              AND csg.path LIKE concat(csg_root.path, '/%')
            )
          )
      ) base
      LEFT JOIN sale_price sp
        ON sp.tenant_id = :tenantId
       AND sp.price_book_id = :priceBookId
       AND (
         (:variantId IS NULL AND sp.variant_id IS NULL)
         OR sp.variant_id = :variantId
       )
       AND sp.catalog_type = base.catalog_type
       AND sp.catalog_item_id = base.catalog_item_id
       AND sp.tenant_unit_id IS NULL
      ORDER BY base.catalog_type, base.catalog_item_name, base.catalog_item_id
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM (
        SELECT cp.id
        FROM catalog_product cp
        LEFT JOIN catalog_group cpg
          ON cpg.id = cp.catalog_group_id
         AND cpg.tenant_id = cp.tenant_id
        LEFT JOIN catalog_group cpg_root
          ON cpg_root.id = :catalogGroupId
         AND cpg_root.tenant_id = cp.tenant_id
        WHERE cp.tenant_id = :tenantId
          AND cp.ativo = true
          AND (:catalogType IS NULL OR :catalogType = 'PRODUCTS')
          AND (
            (:catalogItemId IS NULL AND :text IS NULL)
            OR (:catalogItemId IS NOT NULL AND cp.id = :catalogItemId)
            OR (
              :text IS NOT NULL
              AND lower(COALESCE(cp.nome, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
          AND (
            :catalogGroupId IS NULL
            OR cp.catalog_group_id = :catalogGroupId
            OR (
              :includeGroupChildren = true
              AND cpg.path LIKE concat(cpg_root.path, '/%')
            )
          )
        UNION ALL
        SELECT cs.id
        FROM catalog_service_item cs
        LEFT JOIN catalog_group csg
          ON csg.id = cs.catalog_group_id
         AND csg.tenant_id = cs.tenant_id
        LEFT JOIN catalog_group csg_root
          ON csg_root.id = :catalogGroupId
         AND csg_root.tenant_id = cs.tenant_id
        WHERE cs.tenant_id = :tenantId
          AND cs.ativo = true
          AND (:catalogType IS NULL OR :catalogType = 'SERVICES')
          AND (
            (:catalogItemId IS NULL AND :text IS NULL)
            OR (:catalogItemId IS NOT NULL AND cs.id = :catalogItemId)
            OR (
              :text IS NOT NULL
              AND lower(COALESCE(cs.nome, '')) LIKE lower(concat('%', :text, '%'))
            )
          )
          AND (
            :catalogGroupId IS NULL
            OR cs.catalog_group_id = :catalogGroupId
            OR (
              :includeGroupChildren = true
              AND csg.path LIKE concat(csg_root.path, '/%')
            )
          )
      ) base
      """,
    nativeQuery = true)
  Page<SalePriceGridRowProjection> searchGrid(
    @Param("tenantId") Long tenantId,
    @Param("priceBookId") Long priceBookId,
    @Param("variantId") Long variantId,
    @Param("catalogType") String catalogType,
    @Param("text") String text,
    @Param("catalogItemId") Long catalogItemId,
    @Param("catalogGroupId") Long catalogGroupId,
    @Param("includeGroupChildren") Boolean includeGroupChildren,
    Pageable pageable);

  Optional<SalePrice> findByTenantIdAndPriceBookIdAndVariantIdAndCatalogTypeAndCatalogItemIdAndTenantUnitId(
    Long tenantId,
    Long priceBookId,
    Long variantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId,
    UUID tenantUnitId);

  Optional<SalePrice> findByTenantIdAndPriceBookIdAndVariantIdAndCatalogTypeAndCatalogItemIdAndTenantUnitIdIsNull(
    Long tenantId,
    Long priceBookId,
    Long variantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId);

  Optional<SalePrice> findByTenantIdAndPriceBookIdAndVariantIdIsNullAndCatalogTypeAndCatalogItemIdAndTenantUnitId(
    Long tenantId,
    Long priceBookId,
    CatalogConfigurationType catalogType,
    Long catalogItemId,
    UUID tenantUnitId);

  Optional<SalePrice> findByTenantIdAndPriceBookIdAndVariantIdIsNullAndCatalogTypeAndCatalogItemIdAndTenantUnitIdIsNull(
    Long tenantId,
    Long priceBookId,
    CatalogConfigurationType catalogType,
    Long catalogItemId);

  void deleteByTenantIdAndPriceBookIdAndVariantIdAndCatalogTypeAndCatalogItemIdAndTenantUnitId(
    Long tenantId,
    Long priceBookId,
    Long variantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId,
    UUID tenantUnitId);

  void deleteByTenantIdAndPriceBookIdAndVariantIdIsNullAndCatalogTypeAndCatalogItemIdAndTenantUnitIdIsNull(
    Long tenantId,
    Long priceBookId,
    CatalogConfigurationType catalogType,
    Long catalogItemId);
}
