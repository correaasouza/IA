package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogGroup;
import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBook;
import com.ia.app.domain.SalePrice;
import com.ia.app.dto.SalePriceApplyByGroupRequest;
import com.ia.app.dto.SalePriceApplyByGroupResponse;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogGroupRepository;
import com.ia.app.repository.CatalogItemPriceRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.SalePriceRepository;
import com.ia.app.tenant.TenantContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({
  AuditingConfig.class,
  PriceChangeLogService.class,
  SalePriceService.class
})
class SalePriceApplyByGroupServiceTest {

  @Autowired
  private SalePriceService service;

  @Autowired
  private PriceBookRepository priceBookRepository;

  @Autowired
  private CatalogConfigurationRepository catalogConfigurationRepository;

  @Autowired
  private CatalogGroupRepository catalogGroupRepository;

  @Autowired
  private CatalogProductRepository productRepository;

  @Autowired
  private CatalogItemPriceRepository catalogItemPriceRepository;

  @Autowired
  private SalePriceRepository salePriceRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldApplyPercentageByGroupIncludingChildrenAndSkipExistingWhenConfigured() {
    TenantContext.setTenantId(901L);

    PriceBook book = createBook(901L);
    CatalogConfiguration config = createCatalogConfiguration(901L, CatalogConfigurationType.PRODUCTS);
    CatalogGroup root = createGroup(901L, config.getId(), null, "Informatica");
    CatalogGroup child = createGroup(901L, config.getId(), root.getId(), "Monitores");

    CatalogProduct rootItem = createProduct(901L, config.getId(), root.getId(), 1001L, "Notebook");
    CatalogProduct childItem = createProduct(901L, config.getId(), child.getId(), 1002L, "Monitor");

    createSaleBase(901L, CatalogConfigurationType.PRODUCTS, rootItem.getId(), "100.000000");
    createSaleBase(901L, CatalogConfigurationType.PRODUCTS, childItem.getId(), "50.000000");

    SalePrice existing = new SalePrice();
    existing.setTenantId(901L);
    existing.setPriceBookId(book.getId());
    existing.setCatalogType(CatalogConfigurationType.PRODUCTS);
    existing.setCatalogItemId(rootItem.getId());
    existing.setPriceFinal(new BigDecimal("999.000000"));
    salePriceRepository.save(existing);

    SalePriceApplyByGroupResponse response = service.applyByGroup(new SalePriceApplyByGroupRequest(
      book.getId(),
      null,
      CatalogConfigurationType.PRODUCTS,
      root.getId(),
      null,
      null,
      PriceAdjustmentKind.PERCENT,
      new BigDecimal("10"),
      true,
      false));

    assertThat(response.totalItemsInScope()).isEqualTo(2);
    assertThat(response.processedItems()).isEqualTo(1);
    assertThat(response.createdItems()).isEqualTo(1);
    assertThat(response.updatedItems()).isEqualTo(0);
    assertThat(response.skippedExisting()).isEqualTo(1);
    assertThat(response.skippedWithoutBasePrice()).isEqualTo(0);

    SalePrice rootPrice = service.findExact(
        901L,
        book.getId(),
        null,
        CatalogConfigurationType.PRODUCTS,
        rootItem.getId(),
        null)
      .orElseThrow();
    assertThat(rootPrice.getPriceFinal()).isEqualByComparingTo("999.000000");

    SalePrice childPrice = service.findExact(
        901L,
        book.getId(),
        null,
        CatalogConfigurationType.PRODUCTS,
        childItem.getId(),
        null)
      .orElseThrow();
    assertThat(childPrice.getPriceFinal()).isEqualByComparingTo("55.000000");
  }

  @Test
  void shouldApplyFixedValueByGroup() {
    TenantContext.setTenantId(902L);

    PriceBook book = createBook(902L);
    CatalogConfiguration config = createCatalogConfiguration(902L, CatalogConfigurationType.PRODUCTS);
    CatalogGroup root = createGroup(902L, config.getId(), null, "Bebidas");
    CatalogProduct item = createProduct(902L, config.getId(), root.getId(), 3001L, "Suco");
    CatalogProduct itemWithoutBase = createProduct(902L, config.getId(), root.getId(), 3002L, "Agua");
    createSaleBase(902L, CatalogConfigurationType.PRODUCTS, item.getId(), "80.000000");

    SalePriceApplyByGroupResponse response = service.applyByGroup(new SalePriceApplyByGroupRequest(
      book.getId(),
      null,
      CatalogConfigurationType.PRODUCTS,
      root.getId(),
      null,
      null,
      PriceAdjustmentKind.FIXED,
      new BigDecimal("12"),
      false,
      true));

    assertThat(response.totalItemsInScope()).isEqualTo(2);
    assertThat(response.processedItems()).isEqualTo(2);
    assertThat(response.createdItems()).isEqualTo(2);
    assertThat(response.updatedItems()).isEqualTo(0);
    assertThat(response.skippedWithoutBasePrice()).isEqualTo(0);
    assertThat(response.skippedExisting()).isEqualTo(0);

    SalePrice saved = service.findExact(
        902L,
        book.getId(),
        null,
        CatalogConfigurationType.PRODUCTS,
        item.getId(),
        null)
      .orElseThrow();
    assertThat(saved.getPriceFinal()).isEqualByComparingTo("92.000000");

    SalePrice savedWithoutBase = service.findExact(
        902L,
        book.getId(),
        null,
        CatalogConfigurationType.PRODUCTS,
        itemWithoutBase.getId(),
        null)
      .orElseThrow();
    assertThat(savedWithoutBase.getPriceFinal()).isEqualByComparingTo("12.000000");
  }

  @Test
  void shouldListCatalogItemsIncludingRowsWithoutSalePrice() {
    TenantContext.setTenantId(903L);

    PriceBook book = createBook(903L);
    CatalogConfiguration config = createCatalogConfiguration(903L, CatalogConfigurationType.PRODUCTS);
    CatalogGroup root = createGroup(903L, config.getId(), null, "Linha");
    CatalogProduct withPrice = createProduct(903L, config.getId(), root.getId(), 4001L, "Item com preco");
    CatalogProduct withoutPrice = createProduct(903L, config.getId(), root.getId(), 4002L, "Item sem preco");
    createSaleBase(903L, CatalogConfigurationType.PRODUCTS, withPrice.getId(), "50.000000");
    createSaleBase(903L, CatalogConfigurationType.PRODUCTS, withoutPrice.getId(), "40.000000");

    SalePrice existing = new SalePrice();
    existing.setTenantId(903L);
    existing.setPriceBookId(book.getId());
    existing.setCatalogType(CatalogConfigurationType.PRODUCTS);
    existing.setCatalogItemId(withPrice.getId());
    existing.setPriceFinal(new BigDecimal("55.000000"));
    salePriceRepository.save(existing);

    var page = service.grid(
      book.getId(),
      null,
      CatalogConfigurationType.PRODUCTS,
      null,
      null,
      null,
      true,
      PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(2);
    var rows = page.getContent();

    var rowWithPrice = rows.stream()
      .filter(row -> row.catalogItemId().equals(withPrice.getId()))
      .findFirst()
      .orElseThrow();
    assertThat(rowWithPrice.priceFinal()).isEqualByComparingTo("55.000000");
    assertThat(rowWithPrice.id()).isNotNull();

    var rowWithoutPrice = rows.stream()
      .filter(row -> row.catalogItemId().equals(withoutPrice.getId()))
      .findFirst()
      .orElseThrow();
    assertThat(rowWithoutPrice.priceFinal()).isNull();
    assertThat(rowWithoutPrice.id()).isNull();
  }

  private PriceBook createBook(Long tenantId) {
    PriceBook row = new PriceBook();
    row.setTenantId(tenantId);
    row.setName("Padrao");
    row.setDefaultBook(true);
    row.setActive(true);
    return priceBookRepository.save(row);
  }

  private CatalogConfiguration createCatalogConfiguration(Long tenantId, CatalogConfigurationType type) {
    CatalogConfiguration row = new CatalogConfiguration();
    row.setTenantId(tenantId);
    row.setType(type);
    row.setActive(true);
    return catalogConfigurationRepository.save(row);
  }

  private CatalogGroup createGroup(Long tenantId, Long catalogConfigurationId, Long parentId, String nome) {
    CatalogGroup group = new CatalogGroup();
    group.setTenantId(tenantId);
    group.setCatalogConfigurationId(catalogConfigurationId);
    group.setParentId(parentId);
    group.setNome(nome);
    group.setNomeNormalizado(nome.toLowerCase());
    group.setNivel(parentId == null ? 0 : 1);
    group.setOrdem(1);
    group.setPath("TMP");
    group.setAtivo(true);
    CatalogGroup saved = catalogGroupRepository.save(group);
    String path = parentId == null
      ? String.format("%08d", saved.getId())
      : catalogGroupRepository.findById(parentId).orElseThrow().getPath() + "/" + String.format("%08d", saved.getId());
    saved.setPath(path);
    return catalogGroupRepository.save(saved);
  }

  private CatalogProduct createProduct(
      Long tenantId,
      Long catalogConfigurationId,
      Long catalogGroupId,
      Long codigo,
      String nome) {
    CatalogProduct item = new CatalogProduct();
    item.setTenantId(tenantId);
    item.setCatalogConfigurationId(catalogConfigurationId);
    item.setAgrupadorEmpresaId(200L);
    item.setCatalogGroupId(catalogGroupId);
    item.setCodigo(codigo);
    item.setNome(nome);
    item.setAtivo(true);
    return productRepository.save(item);
  }

  private void createSaleBase(Long tenantId, CatalogConfigurationType type, Long itemId, String price) {
    CatalogItemPrice row = new CatalogItemPrice();
    row.setTenantId(tenantId);
    row.setCatalogType(type);
    row.setCatalogItemId(itemId);
    row.setPriceType(CatalogPriceType.SALE_BASE);
    row.setPriceFinal(new BigDecimal(price));
    row.setAdjustmentKind(PriceAdjustmentKind.FIXED);
    row.setAdjustmentValue(BigDecimal.ZERO);
    catalogItemPriceRepository.save(row);
  }
}
