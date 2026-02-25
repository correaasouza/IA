package com.ia.app.service;

import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.PriceBook;
import com.ia.app.domain.PriceChangeAction;
import com.ia.app.domain.PriceChangeLog;
import com.ia.app.domain.PriceChangeOriginType;
import com.ia.app.domain.PriceChangeSourceType;
import com.ia.app.domain.SalePrice;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.PriceChangeLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PriceChangeLogService {

  private final PriceChangeLogRepository repository;
  private final PriceBookRepository priceBookRepository;

  public PriceChangeLogService(
      PriceChangeLogRepository repository,
      PriceBookRepository priceBookRepository) {
    this.repository = repository;
    this.priceBookRepository = priceBookRepository;
  }

  public void logSalePriceChange(
      Long tenantId,
      SalePrice source,
      PriceChangeAction action,
      BigDecimal oldValue,
      BigDecimal newValue) {
    PriceChangeLog log = new PriceChangeLog();
    log.setTenantId(tenantId);
    log.setSalePriceId(source.getId());
    log.setAction(action);
    log.setSourceType(PriceChangeSourceType.SALE_PRICE);
    log.setOriginType(PriceChangeOriginType.ALTERACAO_TABELA_PRECO);
    log.setOriginId(source.getCatalogItemId());
    log.setOldPriceFinal(normalizeNullable(oldValue));
    log.setNewPriceFinal(normalizeNullable(newValue));
    log.setPriceBookId(source.getPriceBookId());
    log.setPriceBookName(resolvePriceBookName(tenantId, source.getPriceBookId()));
    log.setVariantId(source.getVariantId());
    log.setCatalogType(source.getCatalogType());
    log.setCatalogItemId(source.getCatalogItemId());
    log.setTenantUnitId(source.getTenantUnitId());
    log.setPriceType(null);
    log.setChangedBy(resolveUserId());
    log.setChangedAt(Instant.now());
    repository.save(log);
  }

  public void logCatalogItemPriceChange(
      Long tenantId,
      CatalogItemPrice source,
      PriceChangeAction action,
      BigDecimal oldValue,
      BigDecimal newValue) {
    PriceChangeLog log = new PriceChangeLog();
    log.setTenantId(tenantId);
    log.setSalePriceId(null);
    log.setAction(action);
    log.setSourceType(PriceChangeSourceType.CATALOG_ITEM_PRICE);
    log.setOriginType(PriceChangeOriginType.ALTERACAO_PRECO_BASE);
    log.setOriginId(source.getCatalogItemId());
    log.setOldPriceFinal(normalizeNullable(oldValue));
    log.setNewPriceFinal(normalizeNullable(newValue));
    log.setPriceBookId(null);
    log.setPriceBookName(null);
    log.setVariantId(null);
    log.setCatalogType(source.getCatalogType());
    log.setCatalogItemId(source.getCatalogItemId());
    log.setTenantUnitId(null);
    log.setPriceType(source.getPriceType());
    log.setChangedBy(resolveUserId());
    log.setChangedAt(Instant.now());
    repository.save(log);
  }

  private String resolvePriceBookName(Long tenantId, Long priceBookId) {
    if (tenantId == null || priceBookId == null || priceBookId <= 0) {
      return null;
    }
    Optional<PriceBook> book = priceBookRepository.findByIdAndTenantId(priceBookId, tenantId);
    return book.map(PriceBook::getName).orElse(null);
  }

  private String resolveUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return "system";
    }
    String name = auth.getName();
    return name == null || name.isBlank() ? "system" : name;
  }

  private BigDecimal normalizeNullable(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }
}
