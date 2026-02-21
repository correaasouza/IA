package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogItemBase;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.CatalogServiceItem;
import com.ia.app.domain.ConversionFactorSource;
import com.ia.app.domain.TenantUnitConversion;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.TenantUnitConversionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitConversionService {

  public static final int FACTOR_SCALE = 12;
  public static final int QUANTITY_SCALE = 6;
  public static final int VALUE_SCALE = 6;

  public record CatalogUnitRule(
    UUID unidadeBaseId,
    UUID unidadeAlternativaId,
    BigDecimal fatorBaseParaAlternativa
  ) {}

  public record ConversionResolution(
    BigDecimal fator,
    ConversionFactorSource source
  ) {}

  public record AllowedUnit(
    UUID unitId,
    BigDecimal fatorBaseParaUnidade,
    ConversionFactorSource source
  ) {}

  public record PreviewResult(
    BigDecimal fatorBaseParaOrigem,
    BigDecimal fatorBaseParaDestino,
    BigDecimal fatorOrigemParaDestino,
    BigDecimal quantidadeDestino,
    BigDecimal valorUnitarioDestino,
    BigDecimal valorTotal,
    BigDecimal quantidadeBase,
    ConversionFactorSource source
  ) {}

  private final TenantUnitConversionRepository conversionRepository;
  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;

  public UnitConversionService(
      TenantUnitConversionRepository conversionRepository,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository) {
    this.conversionRepository = conversionRepository;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
  }

  @Transactional(readOnly = true)
  public ConversionResolution resolveConversao(
      Long tenantId,
      UUID unidadeOrigem,
      UUID unidadeDestino,
      CatalogUnitRule catalogRule) {
    if (unidadeOrigem == null || unidadeDestino == null) {
      throw new IllegalArgumentException("tenant_unit_conversion_not_supported");
    }

    if (unidadeOrigem.equals(unidadeDestino)) {
      return new ConversionResolution(BigDecimal.ONE.setScale(FACTOR_SCALE, RoundingMode.HALF_UP), ConversionFactorSource.IDENTITY);
    }

    ConversionResolution itemSpecific = resolveItemSpecific(unidadeOrigem, unidadeDestino, catalogRule);
    if (itemSpecific != null) {
      return itemSpecific;
    }

    Optional<TenantUnitConversion> direct = conversionRepository
      .findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, unidadeOrigem, unidadeDestino);
    if (direct.isPresent()) {
      return new ConversionResolution(normalizeFactor(direct.get().getFator()), ConversionFactorSource.TENANT_CONVERSION);
    }

    Optional<TenantUnitConversion> reverse = conversionRepository
      .findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, unidadeDestino, unidadeOrigem);
    if (reverse.isPresent()) {
      return new ConversionResolution(inverse(reverse.get().getFator()), ConversionFactorSource.TENANT_CONVERSION);
    }

    throw new IllegalArgumentException("tenant_unit_conversion_not_supported");
  }

  @Transactional(readOnly = true)
  public List<AllowedUnit> listAllowedUnitsFromBase(Long tenantId, CatalogUnitRule catalogRule) {
    if (catalogRule == null || catalogRule.unidadeBaseId() == null) {
      return List.of();
    }

    UUID baseUnit = catalogRule.unidadeBaseId();
    Map<UUID, AllowedUnit> allowed = new LinkedHashMap<>();
    allowed.put(baseUnit, new AllowedUnit(
      baseUnit,
      BigDecimal.ONE.setScale(FACTOR_SCALE, RoundingMode.HALF_UP),
      ConversionFactorSource.IDENTITY));

    for (TenantUnitConversion conversion : conversionRepository.findAllByTenantIdAndUnidadeOrigemId(tenantId, baseUnit)) {
      if (conversion == null || conversion.getUnidadeDestinoId() == null) {
        continue;
      }
      allowed.put(conversion.getUnidadeDestinoId(), new AllowedUnit(
        conversion.getUnidadeDestinoId(),
        normalizeFactor(conversion.getFator()),
        ConversionFactorSource.TENANT_CONVERSION));
    }

    for (TenantUnitConversion conversion : conversionRepository.findAllByTenantIdAndUnidadeDestinoId(tenantId, baseUnit)) {
      if (conversion == null || conversion.getUnidadeOrigemId() == null) {
        continue;
      }
      allowed.putIfAbsent(conversion.getUnidadeOrigemId(), new AllowedUnit(
        conversion.getUnidadeOrigemId(),
        inverse(conversion.getFator()),
        ConversionFactorSource.TENANT_CONVERSION));
    }

    if (catalogRule.unidadeAlternativaId() != null && isPositive(catalogRule.fatorBaseParaAlternativa())) {
      allowed.put(catalogRule.unidadeAlternativaId(), new AllowedUnit(
        catalogRule.unidadeAlternativaId(),
        normalizeFactor(catalogRule.fatorBaseParaAlternativa()),
        ConversionFactorSource.ITEM_SPECIFIC));
    }

    return new ArrayList<>(allowed.values());
  }

  @Transactional(readOnly = true)
  public PreviewResult preview(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      UUID unidadeOrigemId,
      UUID unidadeDestinoId,
      BigDecimal quantidadeOrigem,
      BigDecimal valorUnitarioOrigem) {
    CatalogUnitRule rule = loadCatalogRule(tenantId, catalogType, catalogItemId);

    ConversionResolution baseToOrigem = resolveConversao(tenantId, rule.unidadeBaseId(), unidadeOrigemId, rule);
    ConversionResolution baseToDestino = resolveConversao(tenantId, rule.unidadeBaseId(), unidadeDestinoId, rule);

    BigDecimal fatorOrigemParaDestino = divide(baseToDestino.fator(), baseToOrigem.fator());
    BigDecimal qtdOrigemNorm = normalizeQuantity(requireNonNegative(quantidadeOrigem, "movimento_estoque_item_quantidade_invalid"));
    BigDecimal unitOrigemNorm = normalizeValue(requireNonNegative(valorUnitarioOrigem, "movimento_estoque_item_valor_unitario_invalid"));

    BigDecimal quantidadeDestino = normalizeQuantity(qtdOrigemNorm.multiply(fatorOrigemParaDestino));
    BigDecimal valorTotal = normalizeValue(qtdOrigemNorm.multiply(unitOrigemNorm));
    BigDecimal valorUnitarioDestino = quantidadeDestino.compareTo(BigDecimal.ZERO) == 0
      ? BigDecimal.ZERO.setScale(VALUE_SCALE, RoundingMode.HALF_UP)
      : normalizeValue(valorTotal.divide(quantidadeDestino, VALUE_SCALE, RoundingMode.HALF_UP));

    BigDecimal fatorOrigemParaBase = inverse(baseToOrigem.fator());
    BigDecimal quantidadeBase = normalizeQuantity(qtdOrigemNorm.multiply(fatorOrigemParaBase));

    ConversionFactorSource source = chooseSource(baseToOrigem.source(), baseToDestino.source());
    return new PreviewResult(
      baseToOrigem.fator(),
      baseToDestino.fator(),
      fatorOrigemParaDestino,
      quantidadeDestino,
      valorUnitarioDestino,
      valorTotal,
      quantidadeBase,
      source);
  }

  @Transactional(readOnly = true)
  public CatalogUnitRule loadCatalogRule(Long tenantId, CatalogConfigurationType catalogType, Long catalogItemId) {
    CatalogItemBase item = switch (catalogType) {
      case PRODUCTS -> productRepository.findByIdAndTenantId(catalogItemId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_catalogo_invalido"));
      case SERVICES -> serviceItemRepository.findByIdAndTenantId(catalogItemId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_catalogo_invalido"));
    };

    if (item.getTenantUnitId() == null) {
      throw new IllegalArgumentException("catalog_item_unit_required");
    }

    return new CatalogUnitRule(
      item.getTenantUnitId(),
      item.getUnidadeAlternativaTenantUnitId(),
      item.getFatorConversaoAlternativa());
  }

  public BigDecimal convert(BigDecimal quantidade, BigDecimal fator) {
    return normalizeQuantity(requireNonNegative(quantidade, "movimento_estoque_item_quantidade_invalid")
      .multiply(normalizeFactor(fator)));
  }

  private ConversionResolution resolveItemSpecific(UUID origem, UUID destino, CatalogUnitRule rule) {
    if (rule == null || rule.unidadeBaseId() == null || rule.unidadeAlternativaId() == null || !isPositive(rule.fatorBaseParaAlternativa())) {
      return null;
    }

    UUID base = rule.unidadeBaseId();
    UUID alt = rule.unidadeAlternativaId();
    BigDecimal factor = normalizeFactor(rule.fatorBaseParaAlternativa());

    if (origem.equals(base) && destino.equals(alt)) {
      return new ConversionResolution(factor, ConversionFactorSource.ITEM_SPECIFIC);
    }
    if (origem.equals(alt) && destino.equals(base)) {
      return new ConversionResolution(inverse(factor), ConversionFactorSource.ITEM_SPECIFIC);
    }
    return null;
  }

  private ConversionFactorSource chooseSource(ConversionFactorSource sourceA, ConversionFactorSource sourceB) {
    if (sourceA == ConversionFactorSource.ITEM_SPECIFIC || sourceB == ConversionFactorSource.ITEM_SPECIFIC) {
      return ConversionFactorSource.ITEM_SPECIFIC;
    }
    if (sourceA == ConversionFactorSource.TENANT_CONVERSION || sourceB == ConversionFactorSource.TENANT_CONVERSION) {
      return ConversionFactorSource.TENANT_CONVERSION;
    }
    return ConversionFactorSource.IDENTITY;
  }

  private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
    if (!isPositive(denominator)) {
      throw new IllegalArgumentException("tenant_unit_conversion_not_supported");
    }
    return numerator.divide(denominator, FACTOR_SCALE, RoundingMode.HALF_UP).setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal inverse(BigDecimal value) {
    if (!isPositive(value)) {
      throw new IllegalArgumentException("tenant_unit_conversion_not_supported");
    }
    return BigDecimal.ONE.divide(value, FACTOR_SCALE, RoundingMode.HALF_UP).setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizeFactor(BigDecimal value) {
    if (!isPositive(value)) {
      throw new IllegalArgumentException("tenant_unit_conversion_not_supported");
    }
    return value.setScale(FACTOR_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizeQuantity(BigDecimal value) {
    return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizeValue(BigDecimal value) {
    return value.setScale(VALUE_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal requireNonNegative(BigDecimal value, String errorCode) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private boolean isPositive(BigDecimal value) {
    return value != null && value.compareTo(BigDecimal.ZERO) > 0;
  }
}
