package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.SalePrice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalePriceRepository extends JpaRepository<SalePrice, Long> {

  Optional<SalePrice> findByIdAndTenantId(Long id, Long tenantId);

  @Query(
    value = """
      SELECT sp.*
      FROM sale_price sp
      WHERE sp.tenant_id = :tenantId
        AND sp.price_book_id = :priceBookId
        AND (
          (:variantId IS NULL AND sp.variant_id IS NULL)
          OR sp.variant_id = :variantId
        )
        AND (:catalogType IS NULL OR sp.catalog_type = :catalogType)
      ORDER BY sp.catalog_type, sp.catalog_item_id, sp.id
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM sale_price sp
      WHERE sp.tenant_id = :tenantId
        AND sp.price_book_id = :priceBookId
        AND (
          (:variantId IS NULL AND sp.variant_id IS NULL)
          OR sp.variant_id = :variantId
        )
        AND (:catalogType IS NULL OR sp.catalog_type = :catalogType)
      """,
    nativeQuery = true)
  Page<SalePrice> searchGrid(
    @Param("tenantId") Long tenantId,
    @Param("priceBookId") Long priceBookId,
    @Param("variantId") Long variantId,
    @Param("catalogType") String catalogType,
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
