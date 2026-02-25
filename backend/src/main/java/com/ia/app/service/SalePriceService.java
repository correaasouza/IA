package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogGroup;
import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceBook;
import com.ia.app.domain.PriceChangeAction;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceVariant;
import com.ia.app.domain.SalePrice;
import com.ia.app.dto.SalePriceApplyByGroupRequest;
import com.ia.app.dto.SalePriceApplyByGroupResponse;
import com.ia.app.dto.SalePriceBulkItemRequest;
import com.ia.app.dto.SalePriceBulkUpsertRequest;
import com.ia.app.dto.SalePriceGridRowResponse;
import com.ia.app.dto.SalePriceGroupOptionResponse;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogGroupRepository;
import com.ia.app.repository.CatalogItemPriceRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.PriceVariantRepository;
import com.ia.app.repository.SalePriceRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalePriceService {

  private final SalePriceRepository repository;
  private final PriceBookRepository priceBookRepository;
  private final PriceVariantRepository priceVariantRepository;
  private final CatalogConfigurationRepository catalogConfigurationRepository;
  private final CatalogGroupRepository catalogGroupRepository;
  private final CatalogItemPriceRepository catalogItemPriceRepository;
  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;
  private final PriceChangeLogService priceChangeLogService;

  public SalePriceService(
      SalePriceRepository repository,
      PriceBookRepository priceBookRepository,
      PriceVariantRepository priceVariantRepository,
      CatalogConfigurationRepository catalogConfigurationRepository,
      CatalogGroupRepository catalogGroupRepository,
      CatalogItemPriceRepository catalogItemPriceRepository,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      PriceChangeLogService priceChangeLogService) {
    this.repository = repository;
    this.priceBookRepository = priceBookRepository;
    this.priceVariantRepository = priceVariantRepository;
    this.catalogConfigurationRepository = catalogConfigurationRepository;
    this.catalogGroupRepository = catalogGroupRepository;
    this.catalogItemPriceRepository = catalogItemPriceRepository;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.priceChangeLogService = priceChangeLogService;
  }

  @Transactional(readOnly = true)
  public Page<SalePriceGridRowResponse> grid(
      Long priceBookId,
      Long variantId,
      CatalogConfigurationType catalogType,
      String text,
      Long catalogItemId,
      Long catalogGroupId,
      Boolean includeGroupChildren,
      Pageable pageable) {
    Long tenantId = requireTenant();
    validateBookAndVariant(tenantId, priceBookId, variantId);
    String normalizedText = text == null ? null : text.trim();
    if (normalizedText != null && normalizedText.isBlank()) {
      normalizedText = null;
    }

    Page<SalePriceRepository.SalePriceGridRowProjection> page = repository.searchGrid(
      tenantId,
      priceBookId,
      variantId,
      catalogType == null ? null : catalogType.name(),
      normalizedText,
      catalogItemId,
      catalogGroupId,
      includeGroupChildren != null && includeGroupChildren,
      pageable);

    return page.map(this::toGridRow);
  }

  @Transactional
  public List<SalePriceGridRowResponse> bulkUpsert(SalePriceBulkUpsertRequest request) {
    Long tenantId = requireTenant();
    validateBookAndVariant(tenantId, request.priceBookId(), request.variantId());

    List<SalePriceGridRowResponse> result = new ArrayList<>();
    for (SalePriceBulkItemRequest item : request.items()) {
      validateCatalogItemExists(tenantId, item.catalogType(), item.catalogItemId());
      Optional<SalePrice> saved = applyScopedPriceChange(
        tenantId,
        request.priceBookId(),
        request.variantId(),
        item.catalogType(),
        item.catalogItemId(),
        item.tenantUnitId(),
        item.priceFinal());
      saved.ifPresent(salePrice -> result.add(toGridRow(salePrice)));
    }

    return result;
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    SalePrice entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("sale_price_not_found"));
    applyScopedPriceChange(
      tenantId,
      entity.getPriceBookId(),
      entity.getVariantId(),
      entity.getCatalogType(),
      entity.getCatalogItemId(),
      entity.getTenantUnitId(),
      null);
  }

  @Transactional(readOnly = true)
  public List<SalePriceGroupOptionResponse> listGroupOptions(CatalogConfigurationType catalogType) {
    Long tenantId = requireTenant();
    if (catalogType == null) {
      throw new IllegalArgumentException("sale_price_catalog_type_required");
    }

    Long catalogConfigurationId = catalogConfigurationRepository.findByTenantIdAndType(tenantId, catalogType)
      .map(config -> config.getId())
      .orElse(null);
    if (catalogConfigurationId == null) {
      return List.of();
    }

    return catalogGroupRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAtivoTrueOrderByPathAsc(tenantId, catalogConfigurationId)
      .stream()
      .map(group -> new SalePriceGroupOptionResponse(group.getId(), group.getNome(), group.getNivel()))
      .toList();
  }

  @Transactional
  public SalePriceApplyByGroupResponse applyByGroup(SalePriceApplyByGroupRequest request) {
    Long tenantId = requireTenant();
    validateBookAndVariant(tenantId, request.priceBookId(), request.variantId());
    if (request.catalogType() == null) {
      throw new IllegalArgumentException("sale_price_catalog_type_required");
    }

    PriceAdjustmentKind adjustmentKind = request.adjustmentKind() == null
      ? PriceAdjustmentKind.PERCENT
      : request.adjustmentKind();
    BigDecimal adjustmentValue = normalizeScale(request.adjustmentValue());
    boolean includeChildren = request.includeChildren() == null || request.includeChildren();
    boolean overwriteExisting = request.overwriteExisting() != null && request.overwriteExisting();
    String normalizedText = request.text() == null ? null : request.text().trim();
    if (normalizedText != null && normalizedText.isBlank()) {
      normalizedText = null;
    }
    Long catalogItemId = request.catalogItemId() == null || request.catalogItemId() <= 0
      ? null
      : request.catalogItemId();
    Long catalogGroupId = request.catalogGroupId() == null || request.catalogGroupId() <= 0
      ? null
      : request.catalogGroupId();

    List<Long> itemIds = resolveItemIdsInScope(
      tenantId,
      request.priceBookId(),
      request.variantId(),
      request.catalogType(),
      normalizedText,
      catalogItemId,
      catalogGroupId,
      includeChildren);
    if (itemIds.isEmpty()) {
      return new SalePriceApplyByGroupResponse(
        catalogGroupId,
        0,
        0,
        0,
        0,
        0,
        0);
    }

    Map<Long, BigDecimal> saleBaseByItemId = loadSaleBaseMap(tenantId, request.catalogType(), itemIds);
    int processed = 0;
    int created = 0;
    int updated = 0;
    int skippedWithoutBasePrice = 0;
    int skippedExisting = 0;

    for (Long itemId : itemIds) {
      Optional<SalePrice> existing = findExact(
        tenantId,
        request.priceBookId(),
        request.variantId(),
        request.catalogType(),
        itemId,
        null);
      if (existing.isPresent() && !overwriteExisting) {
        skippedExisting++;
        continue;
      }

      BigDecimal saleBase = saleBaseByItemId.get(itemId);
      if (saleBase == null && adjustmentKind == PriceAdjustmentKind.PERCENT) {
        skippedWithoutBasePrice++;
        continue;
      }
      BigDecimal baseForAdjustment = saleBase == null ? BigDecimal.ZERO : saleBase;

      BigDecimal targetPrice = normalizePrice(applyAdjustment(baseForAdjustment, adjustmentKind, adjustmentValue));
      Optional<SalePrice> saved = applyScopedPriceChange(
        tenantId,
        request.priceBookId(),
        request.variantId(),
        request.catalogType(),
        itemId,
        null,
        targetPrice);
      if (saved.isEmpty()) {
        continue;
      }
      processed++;
      if (existing.isPresent()) {
        updated++;
      } else {
        created++;
      }
    }

    return new SalePriceApplyByGroupResponse(
      catalogGroupId,
      itemIds.size(),
      processed,
      created,
      updated,
      skippedWithoutBasePrice,
      skippedExisting);
  }

  private List<Long> resolveItemIdsInScope(
      Long tenantId,
      Long priceBookId,
      Long variantId,
      CatalogConfigurationType catalogType,
      String text,
      Long catalogItemId,
      Long catalogGroupId,
      boolean includeChildren) {
    List<Long> itemIds = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    int pageIndex = 0;
    final int pageSize = 500;

    while (true) {
      Page<SalePriceRepository.SalePriceGridRowProjection> page = repository.searchGrid(
        tenantId,
        priceBookId,
        variantId,
        catalogType.name(),
        text,
        catalogItemId,
        catalogGroupId,
        includeChildren,
        PageRequest.of(pageIndex, pageSize));

      if (page.isEmpty()) {
        break;
      }

      for (SalePriceRepository.SalePriceGridRowProjection row : page.getContent()) {
        Long itemId = row.getCatalogItemId();
        if (itemId != null && seen.add(itemId)) {
          itemIds.add(itemId);
        }
      }

      if (!page.hasNext()) {
        break;
      }
      pageIndex++;
      if (pageIndex > 5000) {
        break;
      }
    }

    return itemIds;
  }

  private void validateBookAndVariant(Long tenantId, Long priceBookId, Long variantId) {
    if (priceBookId == null || priceBookId <= 0) {
      throw new IllegalArgumentException("sale_price_book_required");
    }

    PriceBook book = priceBookRepository.findByIdAndTenantId(priceBookId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_book_not_found"));
    if (!book.isActive()) {
      throw new IllegalArgumentException("sale_price_book_inactive");
    }

    if (variantId != null) {
      PriceVariant variant = priceVariantRepository.findByIdAndTenantId(variantId, tenantId)
        .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));
      if (!variant.isActive()) {
        throw new IllegalArgumentException("sale_price_variant_inactive");
      }
    }
  }

  private void validateCatalogItemExists(Long tenantId, CatalogConfigurationType catalogType, Long catalogItemId) {
    if (catalogType == null) {
      throw new IllegalArgumentException("sale_price_catalog_type_required");
    }
    if (catalogItemId == null || catalogItemId <= 0) {
      throw new IllegalArgumentException("sale_price_catalog_item_required");
    }

    boolean exists = switch (catalogType) {
      case PRODUCTS -> productRepository.findByIdAndTenantId(catalogItemId, tenantId).isPresent();
      case SERVICES -> serviceItemRepository.findByIdAndTenantId(catalogItemId, tenantId).isPresent();
    };

    if (!exists) {
      throw new EntityNotFoundException("catalog_item_not_found");
    }
  }

  public Optional<SalePrice> findExact(
      Long tenantId,
      Long priceBookId,
      Long variantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      UUID tenantUnitId) {
    if (variantId == null) {
      return tenantUnitId == null
        ? repository.findByTenantIdAndPriceBookIdAndVariantIdIsNullAndCatalogTypeAndCatalogItemIdAndTenantUnitIdIsNull(
          tenantId,
          priceBookId,
          catalogType,
          catalogItemId)
        : repository.findByTenantIdAndPriceBookIdAndVariantIdIsNullAndCatalogTypeAndCatalogItemIdAndTenantUnitId(
          tenantId,
          priceBookId,
          catalogType,
          catalogItemId,
          tenantUnitId);
    }

    return tenantUnitId == null
      ? repository.findByTenantIdAndPriceBookIdAndVariantIdAndCatalogTypeAndCatalogItemIdAndTenantUnitIdIsNull(
        tenantId,
        priceBookId,
        variantId,
        catalogType,
        catalogItemId)
      : repository.findByTenantIdAndPriceBookIdAndVariantIdAndCatalogTypeAndCatalogItemIdAndTenantUnitId(
        tenantId,
        priceBookId,
        variantId,
        catalogType,
        catalogItemId,
        tenantUnitId);
  }

  private SalePrice save(SalePrice entity) {
    try {
      return repository.save(entity);
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
      if (message.contains("ux_sale_price_scope")) {
        throw new IllegalArgumentException("sale_price_scope_duplicated");
      }
      throw ex;
    }
  }

  private Optional<SalePrice> applyScopedPriceChange(
      Long tenantId,
      Long priceBookId,
      Long variantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      UUID tenantUnitId,
      BigDecimal requestedPrice) {
    Optional<SalePrice> existingOpt = findExact(
      tenantId,
      priceBookId,
      variantId,
      catalogType,
      catalogItemId,
      tenantUnitId);
    BigDecimal normalized = requestedPrice == null ? null : normalizePrice(requestedPrice);

    if (normalized == null) {
      existingOpt.ifPresent(existing -> {
        repository.delete(existing);
        priceChangeLogService.logSalePriceChange(
          tenantId,
          existing,
          PriceChangeAction.DELETE,
          existing.getPriceFinal(),
          null);
      });
      return Optional.empty();
    }

    SalePrice entity = existingOpt.orElseGet(() -> {
      SalePrice row = new SalePrice();
      row.setTenantId(tenantId);
      row.setPriceBookId(priceBookId);
      row.setVariantId(variantId);
      row.setCatalogType(catalogType);
      row.setCatalogItemId(catalogItemId);
      row.setTenantUnitId(tenantUnitId);
      return row;
    });

    BigDecimal oldValue = entity.getId() == null ? null : entity.getPriceFinal();
    if (oldValue != null && oldValue.compareTo(normalized) == 0) {
      return existingOpt;
    }

    entity.setPriceFinal(normalized);
    SalePrice saved = save(entity);
    priceChangeLogService.logSalePriceChange(
      tenantId,
      saved,
      oldValue == null ? PriceChangeAction.CREATE : PriceChangeAction.UPDATE,
      oldValue,
      saved.getPriceFinal());
    return Optional.of(saved);
  }

  private SalePriceGridRowResponse toGridRow(SalePrice row) {
    return new SalePriceGridRowResponse(
      row.getId(),
      row.getPriceBookId(),
      row.getVariantId(),
      row.getCatalogType(),
      row.getCatalogItemId(),
      null,
      null,
      null,
      row.getTenantUnitId(),
      normalizeNullablePrice(row.getPriceFinal()));
  }

  private SalePriceGridRowResponse toGridRow(SalePriceRepository.SalePriceGridRowProjection row) {
    return new SalePriceGridRowResponse(
      row.getId(),
      row.getPriceBookId(),
      row.getVariantId(),
      CatalogConfigurationType.from(row.getCatalogType()),
      row.getCatalogItemId(),
      row.getCatalogItemName(),
      row.getCatalogGroupName(),
      normalizeNullablePrice(row.getCatalogBasePrice()),
      row.getTenantUnitId(),
      normalizeNullablePrice(row.getPriceFinal()));
  }

  private BigDecimal applyAdjustment(BigDecimal basePrice, PriceAdjustmentKind adjustmentKind, BigDecimal adjustmentValue) {
    BigDecimal base = basePrice == null ? BigDecimal.ZERO : basePrice;
    BigDecimal value = adjustmentValue == null ? BigDecimal.ZERO : adjustmentValue;
    if (adjustmentKind == PriceAdjustmentKind.PERCENT) {
      BigDecimal factor = BigDecimal.ONE.add(
        value.divide(new BigDecimal("100"), CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP));
      return base.multiply(factor).setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
    }
    return base.add(value).setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizePrice(BigDecimal value) {
    BigDecimal normalized = normalizeScale(value);
    if (normalized.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("sale_price_negative");
    }
    return normalized;
  }

  private BigDecimal normalizeNullablePrice(BigDecimal value) {
    return value == null ? null : normalizePrice(value);
  }

  private BigDecimal normalizeScale(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value)
      .setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private List<Long> resolveActiveItemIds(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogConfigurationId,
      Collection<Long> groupIds) {
    if (groupIds == null || groupIds.isEmpty()) {
      return List.of();
    }
    return switch (catalogType) {
      case PRODUCTS -> productRepository.findAtivoIdsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdIn(
        tenantId, catalogConfigurationId, groupIds);
      case SERVICES -> serviceItemRepository.findAtivoIdsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdIn(
        tenantId, catalogConfigurationId, groupIds);
    };
  }

  private Map<Long, BigDecimal> loadSaleBaseMap(
      Long tenantId,
      CatalogConfigurationType catalogType,
      List<Long> itemIds) {
    Map<Long, BigDecimal> saleBaseByItemId = new java.util.HashMap<>();
    if (itemIds == null || itemIds.isEmpty()) {
      return saleBaseByItemId;
    }
    List<CatalogItemPrice> rows = catalogItemPriceRepository
      .findAllByTenantIdAndCatalogTypeAndPriceTypeAndCatalogItemIdIn(
        tenantId,
        catalogType,
        CatalogPriceType.SALE_BASE,
        itemIds);
    for (CatalogItemPrice row : rows) {
      saleBaseByItemId.put(row.getCatalogItemId(), normalizePrice(row.getPriceFinal()));
    }
    return saleBaseByItemId;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
