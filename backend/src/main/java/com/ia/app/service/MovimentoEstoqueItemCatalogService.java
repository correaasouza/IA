package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.CatalogServiceItem;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.MovimentoItemCatalogOptionResponse;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.tenant.EmpresaContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    BigDecimal quantidade,
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

  public MovimentoEstoqueItemCatalogService(
      MovimentoConfigService movimentoConfigService,
      MovimentoConfigItemTipoService movimentoConfigItemTipoService,
      CatalogItemContextService catalogItemContextService,
      CatalogProductRepository catalogProductRepository,
      CatalogServiceItemRepository catalogServiceItemRepository) {
    this.movimentoConfigService = movimentoConfigService;
    this.movimentoConfigItemTipoService = movimentoConfigItemTipoService;
    this.catalogItemContextService = catalogItemContextService;
    this.catalogProductRepository = catalogProductRepository;
    this.catalogServiceItemRepository = catalogServiceItemRepository;
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

    BigDecimal quantidade = normalizeNonNegative(request.quantidade(), "movimento_estoque_item_quantidade_invalid");
    BigDecimal valorUnitarioInformado = request.valorUnitario() == null ? BigDecimal.ZERO : request.valorUnitario();
    BigDecimal valorUnitario = normalizeNonNegative(valorUnitarioInformado, "movimento_estoque_item_valor_unitario_invalid");
    boolean cobrar = vinculo.cobrar();
    if (!cobrar) {
      valorUnitario = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
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
      quantidade,
      valorUnitario,
      valorTotal,
      cobrar,
      ordem,
      observacao);
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
    return new Snapshot(item.getCodigo(), item.getNome());
  }

  private Snapshot resolveServiceSnapshot(CatalogItemContextService.CatalogItemScope scope, Long catalogItemId) {
    CatalogServiceItem item = catalogServiceItemRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndAtivoTrue(
        catalogItemId,
        scope.tenantId(),
        scope.catalogConfigurationId(),
        scope.agrupadorId())
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_catalogo_invalido"));
    return new Snapshot(item.getCodigo(), item.getNome());
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

  private record Snapshot(Long codigo, String nome) {}
}
