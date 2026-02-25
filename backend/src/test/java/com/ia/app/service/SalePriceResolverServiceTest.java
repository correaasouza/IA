package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceBook;
import com.ia.app.domain.PriceVariant;
import com.ia.app.domain.SalePrice;
import com.ia.app.domain.SalePriceSource;
import com.ia.app.dto.SalePriceByItemRowResponse;
import com.ia.app.dto.SalePriceResolveRequest;
import com.ia.app.dto.SalePriceResolveResponse;
import com.ia.app.repository.CatalogItemPriceRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.PriceVariantRepository;
import com.ia.app.repository.SalePriceRepository;
import com.ia.app.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  PriceChangeLogService.class,
  SalePriceService.class,
  SalePriceResolverService.class
})
class SalePriceResolverServiceTest {

  @Autowired
  private SalePriceResolverService resolverService;

  @Autowired
  private PriceBookRepository priceBookRepository;

  @Autowired
  private PriceVariantRepository priceVariantRepository;

  @Autowired
  private SalePriceRepository salePriceRepository;

  @Autowired
  private CatalogItemPriceRepository catalogItemPriceRepository;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void shouldResolveExactVariantBeforeBaseAndCatalogFallback() {
    TenantContext.setTenantId(501L);

    PriceBook book = createBook(501L, "Padrao", true);
    PriceVariant variant = createVariant(501L, "Atacado", true);

    SalePrice base = new SalePrice();
    base.setTenantId(501L);
    base.setPriceBookId(book.getId());
    base.setVariantId(null);
    base.setCatalogType(CatalogConfigurationType.PRODUCTS);
    base.setCatalogItemId(9001L);
    base.setPriceFinal(new BigDecimal("15.000000"));
    salePriceRepository.save(base);

    SalePrice exact = new SalePrice();
    exact.setTenantId(501L);
    exact.setPriceBookId(book.getId());
    exact.setVariantId(variant.getId());
    exact.setCatalogType(CatalogConfigurationType.PRODUCTS);
    exact.setCatalogItemId(9001L);
    exact.setPriceFinal(new BigDecimal("19.000000"));
    salePriceRepository.save(exact);

    CatalogItemPrice fallback = new CatalogItemPrice();
    fallback.setTenantId(501L);
    fallback.setCatalogType(CatalogConfigurationType.PRODUCTS);
    fallback.setCatalogItemId(9001L);
    fallback.setPriceType(CatalogPriceType.SALE_BASE);
    fallback.setPriceFinal(new BigDecimal("12.000000"));
    fallback.setAdjustmentKind(com.ia.app.domain.PriceAdjustmentKind.FIXED);
    fallback.setAdjustmentValue(BigDecimal.ZERO);
    catalogItemPriceRepository.save(fallback);

    SalePriceResolveResponse resolved = resolverService.resolve(new SalePriceResolveRequest(
      book.getId(),
      variant.getId(),
      CatalogConfigurationType.PRODUCTS,
      9001L,
      null));

    assertThat(resolved.priceFinal()).isEqualByComparingTo("19.000000");
    assertThat(resolved.salePriceId()).isEqualTo(exact.getId());
    assertThat(resolved.source()).isEqualTo(SalePriceSource.EXACT_VARIANT);
  }

  @Test
  void shouldFallbackToBookBaseWhenVariantIsInactive() {
    TenantContext.setTenantId(502L);

    PriceBook book = createBook(502L, "Padrao", true);
    PriceVariant inactiveVariant = createVariant(502L, "Promocao", false);

    SalePrice base = new SalePrice();
    base.setTenantId(502L);
    base.setPriceBookId(book.getId());
    base.setCatalogType(CatalogConfigurationType.SERVICES);
    base.setCatalogItemId(7001L);
    base.setPriceFinal(new BigDecimal("42.500000"));
    salePriceRepository.save(base);

    SalePriceResolveResponse resolved = resolverService.resolve(new SalePriceResolveRequest(
      book.getId(),
      inactiveVariant.getId(),
      CatalogConfigurationType.SERVICES,
      7001L,
      null));

    assertThat(resolved.priceFinal()).isEqualByComparingTo("42.500000");
    assertThat(resolved.source()).isEqualTo(SalePriceSource.INACTIVE_VARIANT_FALLBACK);
  }

  @Test
  void shouldFallbackToCatalogSaleBaseWhenNoSalePriceExists() {
    TenantContext.setTenantId(503L);

    PriceBook book = createBook(503L, "Padrao", true);

    CatalogItemPrice fallback = new CatalogItemPrice();
    fallback.setTenantId(503L);
    fallback.setCatalogType(CatalogConfigurationType.PRODUCTS);
    fallback.setCatalogItemId(333L);
    fallback.setPriceType(CatalogPriceType.SALE_BASE);
    fallback.setPriceFinal(new BigDecimal("88.123456"));
    fallback.setAdjustmentKind(com.ia.app.domain.PriceAdjustmentKind.FIXED);
    fallback.setAdjustmentValue(BigDecimal.ZERO);
    catalogItemPriceRepository.save(fallback);

    SalePriceResolveResponse resolved = resolverService.resolve(new SalePriceResolveRequest(
      book.getId(),
      null,
      CatalogConfigurationType.PRODUCTS,
      333L,
      null));

    assertThat(resolved.priceFinal()).isEqualByComparingTo("88.123456");
    assertThat(resolved.salePriceId()).isNull();
    assertThat(resolved.source()).isEqualTo(SalePriceSource.CATALOG_BASE);
  }

  @Test
  void shouldListAllBookAndVariantSalePricesByItem() {
    TenantContext.setTenantId(504L);

    PriceBook bookA = createBook(504L, "Tabela A", true);
    PriceBook bookB = createBook(504L, "Tabela B", true);
    PriceVariant variantAtacado = createVariant(504L, "Atacado", true);
    PriceVariant variantPromo = createVariant(504L, "Promocao", false);

    SalePrice bookABase = new SalePrice();
    bookABase.setTenantId(504L);
    bookABase.setPriceBookId(bookA.getId());
    bookABase.setVariantId(null);
    bookABase.setCatalogType(CatalogConfigurationType.PRODUCTS);
    bookABase.setCatalogItemId(9901L);
    bookABase.setPriceFinal(new BigDecimal("30.000000"));
    salePriceRepository.save(bookABase);

    SalePrice bookAAtacado = new SalePrice();
    bookAAtacado.setTenantId(504L);
    bookAAtacado.setPriceBookId(bookA.getId());
    bookAAtacado.setVariantId(variantAtacado.getId());
    bookAAtacado.setCatalogType(CatalogConfigurationType.PRODUCTS);
    bookAAtacado.setCatalogItemId(9901L);
    bookAAtacado.setPriceFinal(new BigDecimal("35.000000"));
    salePriceRepository.save(bookAAtacado);

    CatalogItemPrice fallback = new CatalogItemPrice();
    fallback.setTenantId(504L);
    fallback.setCatalogType(CatalogConfigurationType.PRODUCTS);
    fallback.setCatalogItemId(9901L);
    fallback.setPriceType(CatalogPriceType.SALE_BASE);
    fallback.setPriceFinal(new BigDecimal("20.000000"));
    fallback.setAdjustmentKind(com.ia.app.domain.PriceAdjustmentKind.FIXED);
    fallback.setAdjustmentValue(BigDecimal.ZERO);
    catalogItemPriceRepository.save(fallback);

    List<SalePriceByItemRowResponse> rows = resolverService.listByItem(
      CatalogConfigurationType.PRODUCTS,
      9901L,
      null);

    assertThat(rows).hasSize(6);

    SalePriceByItemRowResponse baseA = rows.stream()
      .filter(row -> bookA.getId().equals(row.priceBookId()) && row.variantId() == null)
      .findFirst()
      .orElseThrow();
    assertThat(baseA.priceFinal()).isEqualByComparingTo("30.000000");
    assertThat(baseA.source()).isEqualTo(SalePriceSource.BOOK_BASE);

    SalePriceByItemRowResponse atacadoA = rows.stream()
      .filter(row -> bookA.getId().equals(row.priceBookId()) && variantAtacado.getId().equals(row.variantId()))
      .findFirst()
      .orElseThrow();
    assertThat(atacadoA.priceFinal()).isEqualByComparingTo("35.000000");
    assertThat(atacadoA.source()).isEqualTo(SalePriceSource.EXACT_VARIANT);

    SalePriceByItemRowResponse promoA = rows.stream()
      .filter(row -> bookA.getId().equals(row.priceBookId()) && variantPromo.getId().equals(row.variantId()))
      .findFirst()
      .orElseThrow();
    assertThat(promoA.priceFinal()).isEqualByComparingTo("30.000000");
    assertThat(promoA.source()).isEqualTo(SalePriceSource.INACTIVE_VARIANT_FALLBACK);

    SalePriceByItemRowResponse baseB = rows.stream()
      .filter(row -> bookB.getId().equals(row.priceBookId()) && row.variantId() == null)
      .findFirst()
      .orElseThrow();
    assertThat(baseB.priceFinal()).isEqualByComparingTo("20.000000");
    assertThat(baseB.source()).isEqualTo(SalePriceSource.CATALOG_BASE);
  }

  private PriceBook createBook(Long tenantId, String name, boolean active) {
    PriceBook book = new PriceBook();
    book.setTenantId(tenantId);
    book.setName(name);
    book.setActive(active);
    book.setDefaultBook(true);
    return priceBookRepository.save(book);
  }

  private PriceVariant createVariant(Long tenantId, String name, boolean active) {
    PriceVariant variant = new PriceVariant();
    variant.setTenantId(tenantId);
    variant.setName(name);
    variant.setActive(active);
    return priceVariantRepository.save(variant);
  }
}
