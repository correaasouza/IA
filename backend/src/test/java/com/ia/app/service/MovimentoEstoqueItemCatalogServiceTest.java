package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.ConversionFactorSource;
import com.ia.app.domain.SalePriceSource;
import com.ia.app.domain.TenantUnit;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.SalePriceResolveResponse;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.TenantUnitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimentoEstoqueItemCatalogServiceTest {

  @Mock
  private MovimentoConfigService movimentoConfigService;

  @Mock
  private MovimentoConfigItemTipoService movimentoConfigItemTipoService;

  @Mock
  private CatalogItemContextService catalogItemContextService;

  @Mock
  private CatalogProductRepository catalogProductRepository;

  @Mock
  private CatalogServiceItemRepository catalogServiceItemRepository;

  @Mock
  private TenantUnitRepository tenantUnitRepository;

  @Mock
  private UnitConversionService unitConversionService;

  @Mock
  private SalePriceResolverService salePriceResolverService;

  @InjectMocks
  private MovimentoEstoqueItemCatalogService service;

  @Test
  void shouldResolveSalePriceAndPersistSnapshotMetadata() {
    Long tenantId = 701L;
    Long movimentoConfigId = 99L;
    Long itemId = 123L;
    Long priceBookId = 11L;
    Long variantId = 22L;
    UUID unitId = UUID.randomUUID();

    mockCommonContext(tenantId, movimentoConfigId, itemId, unitId);

    when(salePriceResolverService.resolve(org.mockito.ArgumentMatchers.any()))
      .thenReturn(new SalePriceResolveResponse(
        new BigDecimal("15.500000"),
        777L,
        variantId,
        SalePriceSource.EXACT_VARIANT));

    MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolved = service.resolveItem(
      movimentoConfigId,
      new MovimentoEstoqueItemRequest(
        1L,
        itemId,
        unitId,
        priceBookId,
        variantId,
        new BigDecimal("2.000000"),
        null,
        null,
        null),
      0);

    assertThat(resolved.unitPriceApplied()).isEqualByComparingTo("15.500000");
    assertThat(resolved.priceBookIdSnapshot()).isEqualTo(priceBookId);
    assertThat(resolved.variantIdSnapshot()).isEqualTo(variantId);
    assertThat(resolved.salePriceSourceSnapshot()).isEqualTo(SalePriceSource.EXACT_VARIANT);
    assertThat(resolved.salePriceIdSnapshot()).isEqualTo(777L);
    assertThat(resolved.valorUnitario()).isEqualByComparingTo("15.500000");
    assertThat(resolved.valorTotal()).isEqualByComparingTo("31.000000");
  }

  @Test
  void shouldPreferManualPriceWhenProvided() {
    Long tenantId = 702L;
    Long movimentoConfigId = 100L;
    Long itemId = 124L;
    UUID unitId = UUID.randomUUID();

    mockCommonContext(tenantId, movimentoConfigId, itemId, unitId);

    MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolved = service.resolveItem(
      movimentoConfigId,
      new MovimentoEstoqueItemRequest(
        1L,
        itemId,
        unitId,
        33L,
        44L,
        new BigDecimal("1.000000"),
        new BigDecimal("9.900000"),
        null,
        null),
      0);

    assertThat(resolved.unitPriceApplied()).isEqualByComparingTo("9.900000");
    assertThat(resolved.salePriceSourceSnapshot()).isEqualTo(SalePriceSource.MANUAL);
    assertThat(resolved.salePriceIdSnapshot()).isNull();
    assertThat(resolved.valorUnitario()).isEqualByComparingTo("9.900000");
    assertThat(resolved.valorTotal()).isEqualByComparingTo("9.900000");
  }

  private void mockCommonContext(Long tenantId, Long movimentoConfigId, Long itemId, UUID unitId) {
    when(movimentoConfigItemTipoService.listAtivosForConfig(movimentoConfigId))
      .thenReturn(List.of(new MovimentoConfigItemTipoResponse(1L, "Produto", CatalogConfigurationType.PRODUCTS, true, true)));

    when(catalogItemContextService.resolveObrigatorio(CatalogConfigurationType.PRODUCTS))
      .thenReturn(new CatalogItemContextService.CatalogItemScope(
        tenantId,
        1L,
        CatalogConfigurationType.PRODUCTS,
        200L,
        300L,
        "Grupo",
        CatalogNumberingMode.AUTOMATICA));

    CatalogProduct product = new CatalogProduct();
    product.setCodigo(5001L);
    product.setNome("Produto Teste");
    product.setTenantUnitId(unitId);
    when(catalogProductRepository.findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
      itemId,
      tenantId,
      200L,
      300L)).thenReturn(Optional.of(product));

    TenantUnit unit = new TenantUnit();
    unit.setTenantId(tenantId);
    unit.setSigla("UN");
    unit.setNome("Unidade");
    when(tenantUnitRepository.findByIdAndTenantId(unitId, tenantId))
      .thenReturn(Optional.of(unit));

    when(unitConversionService.resolveConversao(
      tenantId,
      unitId,
      unitId,
      new UnitConversionService.CatalogUnitRule(unitId, null, null)))
      .thenReturn(new UnitConversionService.ConversionResolution(
        BigDecimal.ONE.setScale(UnitConversionService.FACTOR_SCALE),
        ConversionFactorSource.IDENTITY));

    when(unitConversionService.convert(
      org.mockito.ArgumentMatchers.any(BigDecimal.class),
      org.mockito.ArgumentMatchers.any(BigDecimal.class)))
      .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).setScale(6, RoundingMode.HALF_UP));
  }
}
