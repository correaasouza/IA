package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.CatalogServiceItem;
import com.ia.app.domain.ConversionFactorSource;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.SalePriceSource;
import com.ia.app.domain.TenantUnit;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.MovimentoItemAllowedUnitResponse;
import com.ia.app.dto.MovimentoItemCatalogOptionResponse;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoItemUnitConversionPreviewRequest;
import com.ia.app.dto.MovimentoItemUnitConversionPreviewResponse;
import com.ia.app.dto.SalePriceResolveRequest;
import com.ia.app.dto.SalePriceResolveResponse;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoEstoqueItemCatalogService {

  public record ResolvedMovimentoItem(
    Long movimentoItemTipoId,
    String movimentoItemTipoNome,
    CatalogConfigurationType catalogType,
    Long catalogItemId,
    Long catalogCodigoSnapshot,
    String catalogNomeSnapshot,
    UUID tenantUnitId,
    String tenantUnitSigla,
    UUID unidadeBaseCatalogoTenantUnitId,
    String unidadeBaseCatalogoSigla,
    BigDecimal quantidade,
    BigDecimal quantidadeConvertidaBase,
    BigDecimal fatorAplicado,
    ConversionFactorSource fatorFonte,
    BigDecimal unitPriceApplied,
    Long priceBookIdSnapshot,
    Long variantIdSnapshot,
    SalePriceSource salePriceSourceSnapshot,
    Long salePriceIdSnapshot,
    BigDecimal valorUnitario,
    BigDecimal valorTotal,
    boolean cobrar,
    Integer ordem,
    String observacao
  ) {}

  private final MovimentoConfigService movimentoConfigService;
  private final MovimentoConfigItemTipoService movimentoConfigItemTipoService;
  private final CatalogItemContextService catalogItemContextService;
  private final CatalogProductRepository catalogProductRepository;
  private final CatalogServiceItemRepository catalogServiceItemRepository;
  private final TenantUnitRepository tenantUnitRepository;
  private final UnitConversionService unitConversionService;
  private final SalePriceResolverService salePriceResolverService;

  public MovimentoEstoqueItemCatalogService(
      MovimentoConfigService movimentoConfigService,
      MovimentoConfigItemTipoService movimentoConfigItemTipoService,
      CatalogItemContextService catalogItemContextService,
      CatalogProductRepository catalogProductRepository,
      CatalogServiceItemRepository catalogServiceItemRepository,
      TenantUnitRepository tenantUnitRepository,
      UnitConversionService unitConversionService,
      SalePriceResolverService salePriceResolverService) {
    this.movimentoConfigService = movimentoConfigService;
    this.movimentoConfigItemTipoService = movimentoConfigItemTipoService;
    this.catalogItemContextService = catalogItemContextService;
    this.catalogProductRepository = catalogProductRepository;
    this.catalogServiceItemRepository = catalogServiceItemRepository;
    this.tenantUnitRepository = tenantUnitRepository;
    this.unitConversionService = unitConversionService;
    this.salePriceResolverService = salePriceResolverService;
  }

  @Transactional(readOnly = true)
  public Page<MovimentoItemCatalogOptionResponse> searchByTipoItem(Long tipoItemId, String text, Pageable pageable) {
    Long empresaId = requireEmpresaContext();
    MovimentoConfigResolverResponse resolver = movimentoConfigService.resolve(MovimentoTipo.MOVIMENTO_ESTOQUE, empresaId, null);
    MovimentoConfigItemTipoResponse vinculo = requireVinculo(resolver.configuracaoId(), tipoItemId);

    String normalizedText = normalizeOptionalText(text);
    CatalogItemContextService.CatalogItemScope scope = catalogItemContextService.resolveObrigatorio(vinculo.catalogType());

    if (vinculo.catalogType() == CatalogConfigurationType.PRODUCTS) {
      return catalogProductRepository
        .search(scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId(), null, normalizedText, null, true, pageable)
        .map(this::toProductOption);
    }
    return catalogServiceItemRepository
      .search(scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId(), null, normalizedText, null, true, pageable)
      .map(this::toServiceOption);
  }

  @Transactional(readOnly = true)
  public ResolvedMovimentoItem resolveItem(Long movimentoConfigId, MovimentoEstoqueItemRequest request, int fallbackOrdem) {
    if (request == null) {
      throw new IllegalArgumentException("movimento_estoque_item_required");
    }
    Long tipoItemId = requirePositive(request.movimentoItemTipoId(), "movimento_estoque_item_tipo_required");
    Long catalogItemId = requirePositive(request.catalogItemId(), "movimento_estoque_item_catalog_item_required");

    MovimentoConfigItemTipoResponse vinculo = requireVinculo(movimentoConfigId, tipoItemId);
    CatalogItemContextService.CatalogItemScope scope = catalogItemContextService.resolveObrigatorio(vinculo.catalogType());

    Snapshot snapshot = switch (vinculo.catalogType()) {
      case PRODUCTS -> resolveProductSnapshot(scope, catalogItemId);
      case SERVICES -> resolveServiceSnapshot(scope, catalogItemId);
    };
    if (snapshot.unidadeBaseId() == null) {
      throw new IllegalArgumentException("catalog_item_unit_required");
    }

    BigDecimal quantidade = normalizeNonNegative(request.quantidade(), "movimento_estoque_item_quantidade_invalid");
    UUID unidadeInformadaId = request.tenantUnitId() == null ? snapshot.unidadeBaseId() : request.tenantUnitId();
    TenantUnit unidadeInformada = tenantUnitRepository.findByIdAndTenantId(unidadeInformadaId, scope.tenantId())
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_unidade_invalid"));
    TenantUnit unidadeBase = tenantUnitRepository.findByIdAndTenantId(snapshot.unidadeBaseId(), scope.tenantId())
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_unidade_invalid"));

    UnitConversionService.CatalogUnitRule conversionRule = new UnitConversionService.CatalogUnitRule(
      snapshot.unidadeBaseId(),
      snapshot.unidadeAlternativaId(),
      snapshot.fatorAlternativo());
    UnitConversionService.ConversionResolution baseToInformed = unitConversionService.resolveConversao(
      scope.tenantId(),
      snapshot.unidadeBaseId(),
      unidadeInformadaId,
      conversionRule);
    BigDecimal fatorInformadaParaBase = BigDecimal.ONE
      .divide(baseToInformed.fator(), UnitConversionService.FACTOR_SCALE, RoundingMode.HALF_UP)
      .setScale(UnitConversionService.FACTOR_SCALE, RoundingMode.HALF_UP);
    BigDecimal quantidadeConvertidaBase = unitConversionService.convert(quantidade, fatorInformadaParaBase);

    PricingSnapshot pricingSnapshot = resolvePricingSnapshot(
      vinculo.cobrar(),
      request,
      vinculo.catalogType(),
      snapshot,
      unidadeInformadaId);
    BigDecimal valorUnitario = pricingSnapshot.unitPriceApplied();
    boolean cobrar = vinculo.cobrar();
    if (!cobrar) {
      valorUnitario = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
      pricingSnapshot = pricingSnapshot.withUnitPriceApplied(valorUnitario);
    }

    BigDecimal valorTotal = cobrar
      ? quantidade.multiply(valorUnitario).setScale(6, RoundingMode.HALF_UP)
      : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    Integer ordem = request.ordem() == null || request.ordem() < 0 ? fallbackOrdem : request.ordem();
    String observacao = normalizeOptional(request.observacao(), 255);

    return new ResolvedMovimentoItem(
      tipoItemId,
      vinculo.nome(),
      vinculo.catalogType(),
      catalogItemId,
      snapshot.codigo(),
      snapshot.nome(),
      unidadeInformadaId,
      unidadeInformada.getSigla(),
      snapshot.unidadeBaseId(),
      unidadeBase.getSigla(),
      quantidade,
      quantidadeConvertidaBase,
      fatorInformadaParaBase,
      baseToInformed.source(),
      pricingSnapshot.unitPriceApplied(),
      pricingSnapshot.priceBookIdSnapshot(),
      pricingSnapshot.variantIdSnapshot(),
      pricingSnapshot.salePriceSourceSnapshot(),
      pricingSnapshot.salePriceIdSnapshot(),
      valorUnitario,
      valorTotal,
      cobrar,
      ordem,
      observacao);
  }

  @Transactional(readOnly = true)
  public List<MovimentoItemAllowedUnitResponse> listAllowedUnits(CatalogConfigurationType catalogType, Long catalogItemId) {
    Long tenantId = requireTenant();
    CatalogItemContextService.CatalogItemScope scope = catalogItemContextService.resolveObrigatorio(catalogType);
    Snapshot snapshot = switch (catalogType) {
      case PRODUCTS -> resolveProductSnapshot(scope, catalogItemId);
      case SERVICES -> resolveServiceSnapshot(scope, catalogItemId);
    };
    if (snapshot.unidadeBaseId() == null) {
      throw new IllegalArgumentException("catalog_item_unit_required");
    }

    UnitConversionService.CatalogUnitRule rule = new UnitConversionService.CatalogUnitRule(
      snapshot.unidadeBaseId(),
      snapshot.unidadeAlternativaId(),
      snapshot.fatorAlternativo());
    List<UnitConversionService.AllowedUnit> allowed = unitConversionService.listAllowedUnitsFromBase(tenantId, rule);

    Set<UUID> unitIds = allowed.stream().map(UnitConversionService.AllowedUnit::unitId).collect(java.util.stream.Collectors.toSet());
    Map<UUID, TenantUnit> unitById = tenantUnitRepository.findAllByTenantIdAndIdIn(tenantId, unitIds).stream()
      .collect(java.util.stream.Collectors.toMap(TenantUnit::getId, unit -> unit, (a, b) -> a));

    return allowed.stream()
      .map(item -> {
        TenantUnit unit = unitById.get(item.unitId());
        if (unit == null) {
          return null;
        }
        return new MovimentoItemAllowedUnitResponse(
          unit.getId(),
          unit.getSigla(),
          unit.getNome(),
          item.fatorBaseParaUnidade(),
          item.source());
      })
      .filter(java.util.Objects::nonNull)
      .sorted(Comparator.comparing(MovimentoItemAllowedUnitResponse::sigla, String.CASE_INSENSITIVE_ORDER))
      .toList();
  }

  @Transactional(readOnly = true)
  public MovimentoItemUnitConversionPreviewResponse previewConversion(MovimentoItemUnitConversionPreviewRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("movimento_estoque_item_required");
    }
    Long tenantId = requireTenant();
    UnitConversionService.PreviewResult preview = unitConversionService.preview(
      tenantId,
      request.catalogType(),
      request.catalogItemId(),
      request.unidadeOrigemId(),
      request.unidadeDestinoId(),
      request.quantidadeOrigem(),
      request.valorUnitarioOrigem());
    return new MovimentoItemUnitConversionPreviewResponse(
      request.unidadeOrigemId(),
      request.unidadeDestinoId(),
      preview.fatorBaseParaOrigem(),
      preview.fatorBaseParaDestino(),
      preview.fatorOrigemParaDestino(),
      preview.quantidadeDestino(),
      preview.valorUnitarioDestino(),
      preview.valorTotal(),
      preview.quantidadeBase(),
      preview.source());
  }

  private MovimentoConfigItemTipoResponse requireVinculo(Long movimentoConfigId, Long tipoItemId) {
    List<MovimentoConfigItemTipoResponse> vinculos = movimentoConfigItemTipoService.listAtivosForConfig(movimentoConfigId);
    return vinculos.stream()
      .filter(item -> item.movimentoItemTipoId().equals(tipoItemId))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_tipo_nao_habilitado"));
  }

  private Snapshot resolveProductSnapshot(CatalogItemContextService.CatalogItemScope scope, Long catalogItemId) {
    CatalogProduct item = catalogProductRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
        catalogItemId,
        scope.tenantId(),
        scope.catalogConfigurationId(),
        scope.agrupadorId())
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_catalogo_invalido"));
    return new Snapshot(
      item.getId(),
      item.getCodigo(),
      item.getNome(),
      item.getTenantUnitId(),
      item.getUnidadeAlternativaTenantUnitId(),
      item.getFatorConversaoAlternativa());
  }

  private Snapshot resolveServiceSnapshot(CatalogItemContextService.CatalogItemScope scope, Long catalogItemId) {
    CatalogServiceItem item = catalogServiceItemRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
        catalogItemId,
        scope.tenantId(),
        scope.catalogConfigurationId(),
        scope.agrupadorId())
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_catalogo_invalido"));
    return new Snapshot(
      item.getId(),
      item.getCodigo(),
      item.getNome(),
      item.getTenantUnitId(),
      item.getUnidadeAlternativaTenantUnitId(),
      item.getFatorConversaoAlternativa());
  }

  private MovimentoItemCatalogOptionResponse toProductOption(CatalogProduct item) {
    return new MovimentoItemCatalogOptionResponse(item.getId(), CatalogConfigurationType.PRODUCTS, item.getCodigo(), item.getNome(), item.getDescricao());
  }

  private MovimentoItemCatalogOptionResponse toServiceOption(CatalogServiceItem item) {
    return new MovimentoItemCatalogOptionResponse(item.getId(), CatalogConfigurationType.SERVICES, item.getCodigo(), item.getNome(), item.getDescricao());
  }

  private Long requireEmpresaContext() {
    Long empresaId = EmpresaContext.getEmpresaId();
    if (empresaId == null || empresaId <= 0) {
      throw new IllegalArgumentException("movimento_empresa_context_required");
    }
    return empresaId;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Long requirePositive(Long value, String errorCode) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private BigDecimal normalizeNonNegative(BigDecimal value, String errorCode) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value.setScale(6, RoundingMode.HALF_UP);
  }

  private String normalizeOptional(String value, int maxLen) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private PricingSnapshot resolvePricingSnapshot(
      boolean cobrar,
      MovimentoEstoqueItemRequest request,
      CatalogConfigurationType catalogType,
      Snapshot snapshot,
      UUID tenantUnitId) {
    Long priceBookId = normalizeOptionalPositive(request.priceBookId(), "movimento_estoque_item_price_book_invalid");
    Long variantId = normalizeOptionalPositive(request.variantId(), "movimento_estoque_item_price_variant_invalid");
    if (variantId != null && priceBookId == null) {
      throw new IllegalArgumentException("movimento_estoque_item_price_book_required_for_variant");
    }

    if (!cobrar) {
      return PricingSnapshot.manual(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), priceBookId, variantId);
    }

    if (request.valorUnitario() != null) {
      BigDecimal informed = normalizeNonNegative(request.valorUnitario(), "movimento_estoque_item_valor_unitario_invalid");
      return PricingSnapshot.manual(informed, priceBookId, variantId);
    }

    if (priceBookId != null) {
      SalePriceResolveResponse resolved = salePriceResolverService.resolve(new SalePriceResolveRequest(
        priceBookId,
        variantId,
        catalogType,
        snapshot.catalogItemId(),
        tenantUnitId));
      BigDecimal resolvedPrice = normalizeNonNegative(resolved.priceFinal(), "movimento_estoque_item_valor_unitario_invalid");
      return new PricingSnapshot(
        resolvedPrice,
        priceBookId,
        variantId,
        resolved.source(),
        resolved.salePriceId());
    }

    return PricingSnapshot.manual(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), null, null);
  }

  private Long normalizeOptionalPositive(Long value, String errorCode) {
    if (value == null) {
      return null;
    }
    if (value <= 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private record Snapshot(
    Long catalogItemId,
    Long codigo,
    String nome,
    UUID unidadeBaseId,
    UUID unidadeAlternativaId,
    BigDecimal fatorAlternativo
  ) {}

  private record PricingSnapshot(
    BigDecimal unitPriceApplied,
    Long priceBookIdSnapshot,
    Long variantIdSnapshot,
    SalePriceSource salePriceSourceSnapshot,
    Long salePriceIdSnapshot
  ) {
    static PricingSnapshot manual(BigDecimal unitPriceApplied, Long priceBookIdSnapshot, Long variantIdSnapshot) {
      return new PricingSnapshot(unitPriceApplied, priceBookIdSnapshot, variantIdSnapshot, SalePriceSource.MANUAL, null);
    }

    PricingSnapshot withUnitPriceApplied(BigDecimal value) {
      return new PricingSnapshot(value, priceBookIdSnapshot, variantIdSnapshot, salePriceSourceSnapshot, salePriceIdSnapshot);
    }
  }
}
