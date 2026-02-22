package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceVariant;
import com.ia.app.domain.SalePrice;
import com.ia.app.domain.SalePriceSource;
import com.ia.app.dto.SalePriceResolveRequest;
import com.ia.app.dto.SalePriceResolveResponse;
import com.ia.app.repository.CatalogItemPriceRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.PriceVariantRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalePriceResolverService {

  private final SalePriceService salePriceService;
  private final PriceBookRepository priceBookRepository;
  private final PriceVariantRepository priceVariantRepository;
  private final CatalogItemPriceRepository catalogItemPriceRepository;

  public SalePriceResolverService(
      SalePriceService salePriceService,
      PriceBookRepository priceBookRepository,
      PriceVariantRepository priceVariantRepository,
      CatalogItemPriceRepository catalogItemPriceRepository) {
    this.salePriceService = salePriceService;
    this.priceBookRepository = priceBookRepository;
    this.priceVariantRepository = priceVariantRepository;
    this.catalogItemPriceRepository = catalogItemPriceRepository;
  }

  @Transactional(readOnly = true)
  public SalePriceResolveResponse resolve(SalePriceResolveRequest request) {
    Long tenantId = requireTenant();
    if (request.priceBookId() == null || request.priceBookId() <= 0) {
      throw new IllegalArgumentException("sale_price_book_required");
    }

    priceBookRepository.findByIdAndTenantId(request.priceBookId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_book_not_found"));

    boolean inactiveVariantFallback = false;
    if (request.variantId() != null) {
      PriceVariant variant = priceVariantRepository.findByIdAndTenantId(request.variantId(), tenantId)
        .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));
      inactiveVariantFallback = !variant.isActive();

      if (!inactiveVariantFallback) {
        Optional<SalePrice> exactVariant = resolveSalePrice(
          tenantId,
          request.priceBookId(),
          request.variantId(),
          request.catalogType(),
          request.catalogItemId(),
          request.tenantUnitId());
        if (exactVariant.isPresent()) {
          SalePrice row = exactVariant.get();
          return new SalePriceResolveResponse(
            normalize(row.getPriceFinal()),
            row.getId(),
            row.getVariantId(),
            SalePriceSource.EXACT_VARIANT);
        }
      }
    }

    Optional<SalePrice> bookBase = resolveSalePrice(
      tenantId,
      request.priceBookId(),
      null,
      request.catalogType(),
      request.catalogItemId(),
      request.tenantUnitId());
    if (bookBase.isPresent()) {
      SalePrice row = bookBase.get();
      return new SalePriceResolveResponse(
        normalize(row.getPriceFinal()),
        row.getId(),
        row.getVariantId(),
        inactiveVariantFallback ? SalePriceSource.INACTIVE_VARIANT_FALLBACK : SalePriceSource.BOOK_BASE);
    }

    BigDecimal fallback = catalogItemPriceRepository
      .findByTenantIdAndCatalogTypeAndCatalogItemIdAndPriceType(
        tenantId,
        request.catalogType(),
        request.catalogItemId(),
        CatalogPriceType.SALE_BASE)
      .map(item -> item.getPriceFinal())
      .orElse(BigDecimal.ZERO);

    return new SalePriceResolveResponse(
      normalize(fallback),
      null,
      null,
      SalePriceSource.CATALOG_BASE);
  }

  private Optional<SalePrice> resolveSalePrice(
      Long tenantId,
      Long priceBookId,
      Long variantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      java.util.UUID tenantUnitId) {
    if (tenantUnitId != null) {
      Optional<SalePrice> unitExact = salePriceService.findExact(
        tenantId,
        priceBookId,
        variantId,
        catalogType,
        catalogItemId,
        tenantUnitId);
      if (unitExact.isPresent()) {
        return unitExact;
      }
    }

    return salePriceService.findExact(
      tenantId,
      priceBookId,
      variantId,
      catalogType,
      catalogItemId,
      null);
  }

  private BigDecimal normalize(BigDecimal value) {
    BigDecimal normalized = (value == null ? BigDecimal.ZERO : value)
      .setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
    if (normalized.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("sale_price_negative");
    }
    return normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
