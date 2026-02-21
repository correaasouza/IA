package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CatalogUnitLockServiceTest {

  @Mock
  private CatalogProductRepository productRepository;

  @Mock
  private CatalogServiceItemRepository serviceItemRepository;

  @Mock
  private CatalogMovementRepository catalogMovementRepository;

  @Mock
  private MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;

  @InjectMocks
  private CatalogUnitLockService service;

  @Test
  void shouldAllowUnitChangesBeforeFirstStockMovement() {
    UUID currentUnit = UUID.randomUUID();
    CatalogProduct item = baseItem(currentUnit);

    when(catalogMovementRepository.existsByTenantIdAndCatalogTypeAndCatalogoId(120L, CatalogConfigurationType.PRODUCTS, 55L))
      .thenReturn(false);
    when(movimentoEstoqueItemRepository.existsByTenantIdAndCatalogTypeAndCatalogItemIdAndEstoqueMovimentadoTrue(
      120L,
      CatalogConfigurationType.PRODUCTS,
      55L))
      .thenReturn(false);

    assertThatCode(() -> service.enforceUpdateRules(
      CatalogConfigurationType.PRODUCTS,
      item,
      UUID.randomUUID(),
      UUID.randomUUID(),
      new BigDecimal("2.500000000000")))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldBlockUnitChangesAfterFirstStockMovement() {
    UUID currentUnit = UUID.randomUUID();
    CatalogProduct item = baseItem(currentUnit);

    when(catalogMovementRepository.existsByTenantIdAndCatalogTypeAndCatalogoId(120L, CatalogConfigurationType.PRODUCTS, 55L))
      .thenReturn(true);

    assertThatThrownBy(() -> service.enforceUpdateRules(
      CatalogConfigurationType.PRODUCTS,
      item,
      UUID.randomUUID(),
      null,
      null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_item_unit_locked_by_stock_movements");
  }

  private CatalogProduct baseItem(UUID tenantUnitId) {
    CatalogProduct item = new CatalogProduct();
    ReflectionTestUtils.setField(item, "id", 55L);
    item.setTenantId(120L);
    item.setCatalogConfigurationId(10L);
    item.setAgrupadorEmpresaId(20L);
    item.setCodigo(55L);
    item.setNome("Produto teste");
    item.setTenantUnitId(tenantUnitId);
    item.setHasStockMovements(false);
    return item;
  }
}
