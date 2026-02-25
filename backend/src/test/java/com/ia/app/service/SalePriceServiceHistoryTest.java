package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.PriceBook;
import com.ia.app.domain.PriceChangeAction;
import com.ia.app.domain.PriceChangeOriginType;
import com.ia.app.domain.PriceChangeSourceType;
import com.ia.app.dto.SalePriceBulkItemRequest;
import com.ia.app.dto.SalePriceBulkUpsertRequest;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.PriceChangeLogRepository;
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
  SalePriceService.class
})
class SalePriceServiceHistoryTest {

  @Autowired
  private SalePriceService service;

  @Autowired
  private PriceBookRepository priceBookRepository;

  @Autowired
  private CatalogProductRepository productRepository;

  @Autowired
  private PriceChangeLogRepository logRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldCreateUpdateAndDeleteWithHistoryLog() {
    TenantContext.setTenantId(701L);

    PriceBook book = createBook(701L);
    CatalogProduct product = createProduct(701L, 9981L);

    service.bulkUpsert(new SalePriceBulkUpsertRequest(
      book.getId(),
      null,
      List.of(new SalePriceBulkItemRequest(
        CatalogConfigurationType.PRODUCTS,
        product.getId(),
        null,
        new BigDecimal("10.000000")))));

    service.bulkUpsert(new SalePriceBulkUpsertRequest(
      book.getId(),
      null,
      List.of(new SalePriceBulkItemRequest(
        CatalogConfigurationType.PRODUCTS,
        product.getId(),
        null,
        new BigDecimal("12.500000")))));

    service.bulkUpsert(new SalePriceBulkUpsertRequest(
      book.getId(),
      null,
      List.of(new SalePriceBulkItemRequest(
        CatalogConfigurationType.PRODUCTS,
        product.getId(),
        null,
        null))));

    var logs = logRepository.findAllByTenantIdAndCatalogTypeAndCatalogItemIdOrderByChangedAtDesc(
      701L,
      CatalogConfigurationType.PRODUCTS,
      product.getId());

    assertThat(logs).hasSize(3);
    assertThat(logs).extracting(item -> item.getAction())
      .containsExactly(PriceChangeAction.DELETE, PriceChangeAction.UPDATE, PriceChangeAction.CREATE);
    assertThat(logs).extracting(item -> item.getSourceType())
      .containsOnly(PriceChangeSourceType.SALE_PRICE);
    assertThat(logs).extracting(item -> item.getOriginType())
      .containsOnly(PriceChangeOriginType.ALTERACAO_TABELA_PRECO);
    assertThat(logs).extracting(item -> item.getPriceBookName())
      .containsOnly("Padrao");
    assertThat(logs.get(2).getNewPriceFinal()).isEqualByComparingTo("10.000000");
    assertThat(logs.get(1).getOldPriceFinal()).isEqualByComparingTo("10.000000");
    assertThat(logs.get(1).getNewPriceFinal()).isEqualByComparingTo("12.500000");
    assertThat(logs.get(0).getOldPriceFinal()).isEqualByComparingTo("12.500000");
    assertThat(logs.get(0).getNewPriceFinal()).isNull();
  }

  @Test
  void shouldNotCreateHistoryWhenPriceDidNotChange() {
    TenantContext.setTenantId(702L);

    PriceBook book = createBook(702L);
    CatalogProduct product = createProduct(702L, 9982L);

    service.bulkUpsert(new SalePriceBulkUpsertRequest(
      book.getId(),
      null,
      List.of(new SalePriceBulkItemRequest(
        CatalogConfigurationType.PRODUCTS,
        product.getId(),
        null,
        new BigDecimal("15.000000")))));

    service.bulkUpsert(new SalePriceBulkUpsertRequest(
      book.getId(),
      null,
      List.of(new SalePriceBulkItemRequest(
        CatalogConfigurationType.PRODUCTS,
        product.getId(),
        null,
        new BigDecimal("15.000000")))));

    var logs = logRepository.findAllByTenantIdAndCatalogTypeAndCatalogItemIdOrderByChangedAtDesc(
      702L,
      CatalogConfigurationType.PRODUCTS,
      product.getId());

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo(PriceChangeAction.CREATE);
  }

  private PriceBook createBook(Long tenantId) {
    PriceBook row = new PriceBook();
    row.setTenantId(tenantId);
    row.setName("Padrao");
    row.setDefaultBook(true);
    row.setActive(true);
    return priceBookRepository.save(row);
  }

  private CatalogProduct createProduct(Long tenantId, Long codigo) {
    CatalogProduct item = new CatalogProduct();
    item.setTenantId(tenantId);
    item.setCatalogConfigurationId(100L);
    item.setAgrupadorEmpresaId(200L);
    item.setCodigo(codigo);
    item.setNome("Produto " + codigo);
    item.setAtivo(true);
    return productRepository.save(item);
  }
}
