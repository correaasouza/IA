package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.CatalogConfigurationByGroup;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogPriceRuleByGroup;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBaseMode;
import com.ia.app.domain.PriceUiLockMode;
import com.ia.app.dto.CatalogItemPriceInput;
import com.ia.app.dto.CatalogItemPriceResponse;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.repository.CatalogItemPriceRepository;
import com.ia.app.repository.CatalogPriceRuleByGroupRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
  AuditingConfig.class,
  CatalogPriceRuleService.class,
  CatalogItemPriceService.class,
  CatalogConfigurationService.class,
  CatalogConfigurationGroupSyncService.class,
  CatalogStockTypeSyncService.class,
  AuditService.class
})
class CatalogItemPriceServiceTest {

  @Autowired
  private CatalogItemPriceService itemPriceService;

  @Autowired
  private CatalogPriceRuleByGroupRepository ruleRepository;

  @Autowired
  private CatalogConfigurationByGroupRepository byGroupRepository;

  @Autowired
  private CatalogItemPriceRepository itemPriceRepository;

  @Test
  void shouldSyncAdjustmentWhenModeThreeUsesLastEditedPrice() {
    Long tenantId = 601L;
    Long byGroupId = createByGroup(tenantId, 1001L, 501L);
    seedRulesForSync(tenantId, byGroupId);

    List<CatalogItemPriceResponse> prices = itemPriceService.upsertForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      90001L,
      byGroupId,
      List.of(
        new CatalogItemPriceInput(CatalogPriceType.PURCHASE, new BigDecimal("100.000000"), null, null, null),
        new CatalogItemPriceInput(
          CatalogPriceType.COST,
          new BigDecimal("120.000000"),
          PriceAdjustmentKind.FIXED,
          null,
          com.ia.app.domain.CatalogPriceEditedField.PRICE)
      ));

    CatalogItemPriceResponse cost = find(prices, CatalogPriceType.COST);
    CatalogItemPriceResponse average = find(prices, CatalogPriceType.AVERAGE_COST);
    CatalogItemPriceResponse sale = find(prices, CatalogPriceType.SALE_BASE);

    assertThat(cost.priceFinal()).isEqualByComparingTo("120.000000");
    assertThat(cost.adjustmentValue()).isEqualByComparingTo("20.000000");
    assertThat(average.priceFinal()).isEqualByComparingTo("120.000000");
    assertThat(sale.priceFinal()).isEqualByComparingTo("120.000000");
  }

  @Test
  void shouldRecalculateModeFourOnReadWhenStoredPriceIsStale() {
    Long tenantId = 602L;
    Long byGroupId = createByGroup(tenantId, 1002L, 502L);
    seedRulesForModeFour(tenantId, byGroupId);

    itemPriceService.upsertForItem(
      tenantId,
      CatalogConfigurationType.SERVICES,
      70001L,
      byGroupId,
      List.of(new CatalogItemPriceInput(CatalogPriceType.PURCHASE, new BigDecimal("100.000000"), null, null, null)));

    var costRow = itemPriceRepository
      .findByTenantIdAndCatalogTypeAndCatalogItemIdAndPriceType(tenantId, CatalogConfigurationType.SERVICES, 70001L, CatalogPriceType.COST)
      .orElseThrow();
    costRow.setPriceFinal(new BigDecimal("999.000000"));
    itemPriceRepository.save(costRow);

    List<CatalogItemPriceResponse> refreshed = itemPriceService.getOrCreateForItem(
      tenantId,
      CatalogConfigurationType.SERVICES,
      70001L,
      byGroupId,
      true);

    CatalogItemPriceResponse cost = find(refreshed, CatalogPriceType.COST);
    CatalogItemPriceResponse average = find(refreshed, CatalogPriceType.AVERAGE_COST);

    assertThat(cost.priceFinal()).isEqualByComparingTo("105.000000");
    assertThat(average.priceFinal()).isEqualByComparingTo("105.000000");
  }

  @Test
  void shouldCalculateSaleBaseFromCostWithPercentMarginInModeFour() {
    Long tenantId = 604L;
    Long byGroupId = createByGroup(tenantId, 1004L, 504L);
    seedRulesForModeFour(tenantId, byGroupId);

    CatalogPriceRuleByGroup saleRule = ruleRepository
      .findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroupId)
      .stream()
      .filter(row -> row.getPriceType() == CatalogPriceType.SALE_BASE)
      .findFirst()
      .orElseThrow();
    CatalogPriceRuleByGroup costRule = ruleRepository
      .findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroupId)
      .stream()
      .filter(row -> row.getPriceType() == CatalogPriceType.COST)
      .findFirst()
      .orElseThrow();
    costRule.setUiLockMode(PriceUiLockMode.II);
    ruleRepository.save(costRule);
    saleRule.setBasePriceType(CatalogPriceType.COST);
    saleRule.setAdjustmentKindDefault(PriceAdjustmentKind.PERCENT);
    saleRule.setAdjustmentDefault(new BigDecimal("20.000000"));
    ruleRepository.save(saleRule);

    List<CatalogItemPriceResponse> prices = itemPriceService.upsertForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      81001L,
      byGroupId,
      List.of(
        new CatalogItemPriceInput(CatalogPriceType.PURCHASE, new BigDecimal("100.000000"), null, null, null),
        new CatalogItemPriceInput(CatalogPriceType.COST, new BigDecimal("150.000000"), null, null, null)
      ));

    CatalogItemPriceResponse sale = find(prices, CatalogPriceType.SALE_BASE);
    assertThat(sale.priceFinal()).isEqualByComparingTo("180.000000");
    assertThat(sale.adjustmentKind()).isEqualTo(PriceAdjustmentKind.PERCENT);
    assertThat(sale.adjustmentValue()).isEqualByComparingTo("20.000000");
  }

  @Test
  void shouldPreviewDependentPricesWithoutPersistingItemRows() {
    Long tenantId = 605L;
    Long byGroupId = createByGroup(tenantId, 1005L, 505L);
    seedRulesForModeFour(tenantId, byGroupId);

    CatalogPriceRuleByGroup saleRule = ruleRepository
      .findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroupId)
      .stream()
      .filter(row -> row.getPriceType() == CatalogPriceType.SALE_BASE)
      .findFirst()
      .orElseThrow();
    CatalogPriceRuleByGroup costRule = ruleRepository
      .findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroupId)
      .stream()
      .filter(row -> row.getPriceType() == CatalogPriceType.COST)
      .findFirst()
      .orElseThrow();
    costRule.setUiLockMode(PriceUiLockMode.II);
    ruleRepository.save(costRule);
    saleRule.setBasePriceType(CatalogPriceType.COST);
    saleRule.setAdjustmentKindDefault(PriceAdjustmentKind.PERCENT);
    saleRule.setAdjustmentDefault(new BigDecimal("20.000000"));
    ruleRepository.save(saleRule);

    itemPriceService.upsertForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      82001L,
      byGroupId,
      List.of(
        new CatalogItemPriceInput(CatalogPriceType.PURCHASE, new BigDecimal("100.000000"), null, null, null),
        new CatalogItemPriceInput(CatalogPriceType.COST, new BigDecimal("150.000000"), null, null, null)
      ));

    List<CatalogItemPriceResponse> preview = itemPriceService.previewForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      82001L,
      byGroupId,
      List.of(new CatalogItemPriceInput(CatalogPriceType.COST, new BigDecimal("200.000000"), null, null, null)));

    CatalogItemPriceResponse previewSale = find(preview, CatalogPriceType.SALE_BASE);
    assertThat(previewSale.priceFinal()).isEqualByComparingTo("240.000000");

    List<CatalogItemPriceResponse> stored = itemPriceService.getOrCreateForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      82001L,
      byGroupId,
      false);
    CatalogItemPriceResponse storedCost = find(stored, CatalogPriceType.COST);
    CatalogItemPriceResponse storedSale = find(stored, CatalogPriceType.SALE_BASE);

    assertThat(storedCost.priceFinal()).isEqualByComparingTo("150.000000");
    assertThat(storedSale.priceFinal()).isEqualByComparingTo("180.000000");
  }

  @Test
  @Transactional
  void shouldApplyUpdatedRuleDefaultOnModeFourWhenRuleChangesAfterItemWasCreated() {
    Long tenantId = 603L;
    Long byGroupId = createByGroup(tenantId, 1003L, 503L);
    seedRulesForModeFour(tenantId, byGroupId);

    itemPriceService.upsertForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      80001L,
      byGroupId,
      List.of(new CatalogItemPriceInput(CatalogPriceType.PURCHASE, new BigDecimal("100.000000"), null, null, null)));

    CatalogPriceRuleByGroup saleRule = ruleRepository
      .findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroupId)
      .stream()
      .filter(row -> row.getPriceType() == CatalogPriceType.SALE_BASE)
      .findFirst()
      .orElseThrow();
    saleRule.setAdjustmentKindDefault(PriceAdjustmentKind.PERCENT);
    saleRule.setAdjustmentDefault(new BigDecimal("20.000000"));
    ruleRepository.save(saleRule);

    var saleRow = itemPriceRepository
      .findByTenantIdAndCatalogTypeAndCatalogItemIdAndPriceType(tenantId, CatalogConfigurationType.PRODUCTS, 80001L, CatalogPriceType.SALE_BASE)
      .orElseThrow();
    saleRow.setAdjustmentKind(PriceAdjustmentKind.FIXED);
    saleRow.setAdjustmentValue(BigDecimal.ZERO);
    saleRow.setPriceFinal(new BigDecimal("105.000000"));
    itemPriceRepository.save(saleRow);

    List<CatalogItemPriceResponse> refreshed = itemPriceService.getOrCreateForItem(
      tenantId,
      CatalogConfigurationType.PRODUCTS,
      80001L,
      byGroupId,
      true);

    CatalogItemPriceResponse sale = find(refreshed, CatalogPriceType.SALE_BASE);
    assertThat(sale.priceFinal()).isEqualByComparingTo("126.000000");
    assertThat(sale.adjustmentKind()).isEqualTo(PriceAdjustmentKind.PERCENT);
    assertThat(sale.adjustmentValue()).isEqualByComparingTo("20.000000");
  }

  private Long createByGroup(Long tenantId, Long configId, Long agrupadorId) {
    CatalogConfigurationByGroup row = new CatalogConfigurationByGroup();
    row.setTenantId(tenantId);
    row.setCatalogConfigurationId(configId);
    row.setAgrupadorId(agrupadorId);
    row.setActive(true);
    return byGroupRepository.save(row).getId();
  }

  private void seedRulesForSync(Long tenantId, Long byGroupId) {
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.PURCHASE, PriceBaseMode.NONE, null, PriceUiLockMode.II, BigDecimal.ZERO));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.COST, PriceBaseMode.BASE_PRICE, CatalogPriceType.PURCHASE, PriceUiLockMode.III, BigDecimal.ZERO));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.AVERAGE_COST, PriceBaseMode.BASE_PRICE, CatalogPriceType.COST, PriceUiLockMode.IV, BigDecimal.ZERO));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.SALE_BASE, PriceBaseMode.BASE_PRICE, CatalogPriceType.AVERAGE_COST, PriceUiLockMode.IV, BigDecimal.ZERO));
  }

  private void seedRulesForModeFour(Long tenantId, Long byGroupId) {
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.PURCHASE, PriceBaseMode.NONE, null, PriceUiLockMode.II, BigDecimal.ZERO));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.COST, PriceBaseMode.BASE_PRICE, CatalogPriceType.PURCHASE, PriceUiLockMode.IV, new BigDecimal("5.000000")));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.AVERAGE_COST, PriceBaseMode.BASE_PRICE, CatalogPriceType.COST, PriceUiLockMode.IV, BigDecimal.ZERO));
    ruleRepository.save(rule(tenantId, byGroupId, CatalogPriceType.SALE_BASE, PriceBaseMode.BASE_PRICE, CatalogPriceType.AVERAGE_COST, PriceUiLockMode.IV, BigDecimal.ZERO));
  }

  private CatalogPriceRuleByGroup rule(
      Long tenantId,
      Long byGroupId,
      CatalogPriceType type,
      PriceBaseMode baseMode,
      CatalogPriceType baseType,
      PriceUiLockMode lockMode,
      BigDecimal adjustmentDefault) {
    CatalogPriceRuleByGroup row = new CatalogPriceRuleByGroup();
    row.setTenantId(tenantId);
    row.setCatalogConfigurationByGroupId(byGroupId);
    row.setPriceType(type);
    row.setBaseMode(baseMode);
    row.setBasePriceType(baseType);
    row.setAdjustmentKindDefault(PriceAdjustmentKind.FIXED);
    row.setAdjustmentDefault(adjustmentDefault);
    row.setUiLockMode(lockMode);
    row.setActive(true);
    return row;
  }

  private CatalogItemPriceResponse find(List<CatalogItemPriceResponse> rows, CatalogPriceType type) {
    return rows.stream().filter(row -> row.priceType() == type).findFirst().orElseThrow();
  }
}
