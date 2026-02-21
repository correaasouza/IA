package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ia.app.domain.ConversionFactorSource;
import com.ia.app.domain.TenantUnitConversion;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.TenantUnitConversionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitConversionServiceTest {

  @Mock
  private TenantUnitConversionRepository conversionRepository;

  @Mock
  private CatalogProductRepository productRepository;

  @Mock
  private CatalogServiceItemRepository serviceItemRepository;

  @InjectMocks
  private UnitConversionService service;

  @Test
  void shouldResolveConversionUsingTenantConversionRule() {
    Long tenantId = 901L;
    UUID origem = UUID.randomUUID();
    UUID destino = UUID.randomUUID();

    TenantUnitConversion conversion = new TenantUnitConversion();
    conversion.setTenantId(tenantId);
    conversion.setUnidadeOrigemId(origem);
    conversion.setUnidadeDestinoId(destino);
    conversion.setFator(new BigDecimal("1000"));

    when(conversionRepository.findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, origem, destino))
      .thenReturn(Optional.of(conversion));

    UnitConversionService.ConversionResolution resolution = service.resolveConversao(
      tenantId,
      origem,
      destino,
      null);

    assertThat(resolution.source()).isEqualTo(ConversionFactorSource.TENANT_CONVERSION);
    assertThat(resolution.fator()).isEqualByComparingTo("1000.000000000000");
  }

  @Test
  void shouldResolveConversionUsingCatalogItemSpecificRule() {
    Long tenantId = 902L;
    UUID base = UUID.randomUUID();
    UUID alternativa = UUID.randomUUID();

    UnitConversionService.CatalogUnitRule rule = new UnitConversionService.CatalogUnitRule(
      base,
      alternativa,
      new BigDecimal("800"));

    UnitConversionService.ConversionResolution resolution = service.resolveConversao(
      tenantId,
      base,
      alternativa,
      rule);

    assertThat(resolution.source()).isEqualTo(ConversionFactorSource.ITEM_SPECIFIC);
    assertThat(resolution.fator()).isEqualByComparingTo("800.000000000000");
  }

  @Test
  void shouldRejectConversionWhenNoRuleSupportsRequestedUnits() {
    Long tenantId = 903L;
    UUID origem = UUID.randomUUID();
    UUID destino = UUID.randomUUID();

    when(conversionRepository.findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, origem, destino))
      .thenReturn(Optional.empty());
    when(conversionRepository.findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, destino, origem))
      .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolveConversao(tenantId, origem, destino, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("tenant_unit_conversion_not_supported");
  }
}

