package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.CatalogPriceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemPriceRepository extends JpaRepository<CatalogItemPrice, Long> {

  List<CatalogItemPrice> findAllByTenantIdAndCatalogTypeAndCatalogItemIdOrderByPriceTypeAsc(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId);

  Optional<CatalogItemPrice> findByTenantIdAndCatalogTypeAndCatalogItemIdAndPriceType(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId,
    CatalogPriceType priceType);

  List<CatalogItemPrice> findAllByTenantIdAndCatalogTypeAndPriceTypeAndCatalogItemIdIn(
    Long tenantId,
    CatalogConfigurationType catalogType,
    CatalogPriceType priceType,
    Collection<Long> catalogItemIds);
}
