package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceChangeLog;
import com.ia.app.domain.PriceChangeSourceType;
import com.ia.app.dto.CatalogPriceHistoryResponse;
import com.ia.app.repository.PriceChangeLogRepository;
import com.ia.app.tenant.TenantContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogPriceHistoryService {

  private final PriceChangeLogRepository repository;

  public CatalogPriceHistoryService(PriceChangeLogRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Page<CatalogPriceHistoryResponse> listByItem(
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      PriceChangeSourceType sourceType,
      CatalogPriceType priceType,
      Long priceBookId,
      String text,
      Instant fromDate,
      Instant toDate,
      Pageable pageable) {
    if (catalogType == null) {
      throw new IllegalArgumentException("sale_price_catalog_type_required");
    }
    if (catalogItemId == null || catalogItemId <= 0) {
      throw new IllegalArgumentException("sale_price_catalog_item_required");
    }
    Long tenantId = requireTenant();
    String normalizedText = normalizeOptionalText(text);
    Pageable effectivePageable = normalizePageable(pageable);

    Specification<PriceChangeLog> specification = (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("tenantId"), tenantId));
      predicates.add(cb.equal(root.get("catalogType"), catalogType));
      predicates.add(cb.equal(root.get("catalogItemId"), catalogItemId));
      if (sourceType != null) {
        predicates.add(cb.equal(root.get("sourceType"), sourceType));
      }
      if (priceType != null) {
        predicates.add(cb.equal(root.get("priceType"), priceType));
      }
      if (priceBookId != null && priceBookId > 0) {
        predicates.add(cb.equal(root.get("priceBookId"), priceBookId));
      }
      if (fromDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("changedAt"), fromDate));
      }
      if (toDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("changedAt"), toDate));
      }
      if (normalizedText != null) {
        String like = "%" + normalizedText.toLowerCase() + "%";
        predicates.add(cb.or(
          cb.like(cb.lower(cb.coalesce(root.get("changedBy"), "")), like),
          cb.like(cb.lower(cb.coalesce(root.get("priceBookName"), "")), like)));
      }
      return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };

    return repository.findAll(specification, effectivePageable)
      .map(item -> new CatalogPriceHistoryResponse(
        item.getId(),
        item.getAction(),
        item.getSourceType(),
        item.getOriginType(),
        item.getOriginId(),
        item.getPriceType(),
        item.getOldPriceFinal(),
        item.getNewPriceFinal(),
        item.getPriceBookId(),
        item.getPriceBookName(),
        item.getVariantId(),
        item.getChangedBy(),
        item.getChangedAt()));
  }

  private Pageable normalizePageable(Pageable pageable) {
    int pageNumber = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
    int pageSize = pageable == null ? 20 : Math.min(Math.max(pageable.getPageSize(), 1), 200);
    Sort defaultSort = Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id"));
    if (pageable == null || pageable.getSort().isUnsorted()) {
      return PageRequest.of(pageNumber, pageSize, defaultSort);
    }
    return PageRequest.of(pageNumber, pageSize, pageable.getSort());
  }

  private String normalizeOptionalText(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
