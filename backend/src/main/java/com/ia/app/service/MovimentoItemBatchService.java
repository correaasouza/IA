package com.ia.app.service;

import com.ia.app.domain.CatalogMovement;
import com.ia.app.domain.CatalogMovementLine;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.dto.MovementItemAddRequest;
import com.ia.app.dto.MovementItemsBatchAddRequest;
import com.ia.app.dto.MovementItemsBatchAddResponse;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.MovimentoEstoqueItemResponse;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoItemBatchService {

  private final MovimentoEstoqueRepository movimentoEstoqueRepository;
  private final MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;
  private final MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService;
  private final MovimentoItemTipoService movimentoItemTipoService;
  private final CatalogMovementRepository catalogMovementRepository;
  private final CatalogMovementLineRepository catalogMovementLineRepository;
  private final CatalogMovementEngine catalogMovementEngine;
  private final MovimentoEstoqueLockService lockService;
  private final TenantUnitRepository tenantUnitRepository;
  private final AuditService auditService;

  public MovimentoItemBatchService(
      MovimentoEstoqueRepository movimentoEstoqueRepository,
      MovimentoEstoqueItemRepository movimentoEstoqueItemRepository,
      MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService,
      MovimentoItemTipoService movimentoItemTipoService,
      CatalogMovementRepository catalogMovementRepository,
      CatalogMovementLineRepository catalogMovementLineRepository,
      CatalogMovementEngine catalogMovementEngine,
      MovimentoEstoqueLockService lockService,
      TenantUnitRepository tenantUnitRepository,
      AuditService auditService) {
    this.movimentoEstoqueRepository = movimentoEstoqueRepository;
    this.movimentoEstoqueItemRepository = movimentoEstoqueItemRepository;
    this.movimentoEstoqueItemCatalogService = movimentoEstoqueItemCatalogService;
    this.movimentoItemTipoService = movimentoItemTipoService;
    this.catalogMovementRepository = catalogMovementRepository;
    this.catalogMovementLineRepository = catalogMovementLineRepository;
    this.catalogMovementEngine = catalogMovementEngine;
    this.lockService = lockService;
    this.tenantUnitRepository = tenantUnitRepository;
    this.auditService = auditService;
  }

  @Transactional
  public MovementItemsBatchAddResponse addItems(Long movementId, MovementItemsBatchAddRequest request) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    Long normalizedMovementId = requirePositive(movementId, "movimento_estoque_id_invalid");
    List<MovementItemAddRequest> requestItems = request == null ? List.of() : request.items();
    if (requestItems == null || requestItems.isEmpty()) {
      throw new IllegalArgumentException("movimento_estoque_items_required");
    }

    MovimentoEstoque movimento = movimentoEstoqueRepository.findWithLockByIdAndTenantId(normalizedMovementId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_not_found"));
    if (!Objects.equals(movimento.getEmpresaId(), empresaId)) {
      throw new EntityNotFoundException("movimento_estoque_not_found");
    }
    lockService.assertMovimentoAbertoParaItens(tenantId, movimento);

    int nextOrder = movimentoEstoqueItemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(tenantId, movimento.getId())
      .size();
    long nextCodigo = resolveNextCodigo(tenantId, movimento.getId());

    List<MovimentoEstoqueItem> entities = new ArrayList<>(requestItems.size());
    for (MovementItemAddRequest item : requestItems) {
      MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolved = resolveItem(movimento, item, nextOrder);
      MovimentoEstoqueItem entity = new MovimentoEstoqueItem();
      entity.setTenantId(tenantId);
      entity.setCodigo(nextCodigo);
      entity.setMovimentoEstoqueId(movimento.getId());
      entity.setMovimentoItemTipoId(resolved.movimentoItemTipoId());
      entity.setCatalogType(resolved.catalogType());
      entity.setCatalogItemId(resolved.catalogItemId());
      entity.setCatalogCodigoSnapshot(resolved.catalogCodigoSnapshot());
      entity.setCatalogNomeSnapshot(resolved.catalogNomeSnapshot());
      entity.setTenantUnitId(resolved.tenantUnitId());
      entity.setUnidadeBaseCatalogoTenantUnitId(resolved.unidadeBaseCatalogoTenantUnitId());
      entity.setQuantidade(resolved.quantidade());
      entity.setQuantidadeConvertidaBase(resolved.quantidadeConvertidaBase());
      entity.setFatorAplicado(resolved.fatorAplicado());
      entity.setFatorFonte(resolved.fatorFonte());
      entity.setUnitPriceApplied(resolved.unitPriceApplied());
      entity.setPriceBookIdSnapshot(resolved.priceBookIdSnapshot());
      entity.setVariantIdSnapshot(resolved.variantIdSnapshot());
      entity.setSalePriceSourceSnapshot(resolved.salePriceSourceSnapshot());
      entity.setSalePriceIdSnapshot(resolved.salePriceIdSnapshot());
      entity.setValorUnitario(resolved.valorUnitario());
      entity.setValorTotal(resolved.valorTotal());
      entity.setCobrar(resolved.cobrar());
      entity.setOrdem(resolved.ordem());
      entity.setObservacao(resolved.observacao());
      entities.add(entity);
      nextOrder += 1;
      nextCodigo += 1;
    }

    List<MovimentoEstoqueItem> saved = movimentoEstoqueItemRepository.saveAll(entities);

    List<MovimentoEstoqueItem> allItems = movimentoEstoqueItemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(tenantId, movimento.getId());
    BigDecimal totalCobrado = allItems.stream()
      .map(MovimentoEstoqueItem::getValorTotal)
      .filter(Objects::nonNull)
      .reduce(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), BigDecimal::add);

    Map<Long, String> itemTypeNameById = loadItemTypeNames(saved);
    Map<UUID, String> unitSiglaById = loadUnitSiglas(tenantId, saved);
    List<MovimentoEstoqueItemResponse> addedResponses = saved.stream()
      .map(item -> new MovimentoEstoqueItemResponse(
        item.getId(),
        item.getCodigo(),
        item.getMovimentoItemTipoId(),
        itemTypeNameById.getOrDefault(item.getMovimentoItemTipoId(), "-"),
        item.getCatalogType(),
        item.getCatalogItemId(),
        item.getCatalogCodigoSnapshot(),
        item.getCatalogNomeSnapshot(),
        item.getTenantUnitId(),
        unitSiglaById.get(item.getTenantUnitId()),
        item.getUnidadeBaseCatalogoTenantUnitId(),
        unitSiglaById.get(item.getUnidadeBaseCatalogoTenantUnitId()),
        item.getQuantidade(),
        item.getQuantidadeConvertidaBase(),
        item.getFatorAplicado(),
        item.getFatorFonte(),
        item.getUnitPriceApplied(),
        item.getPriceBookIdSnapshot(),
        item.getVariantIdSnapshot(),
        item.getSalePriceSourceSnapshot(),
        item.getSalePriceIdSnapshot(),
        item.getValorUnitario(),
        item.getValorTotal(),
        item.isCobrar(),
        item.isEstoqueMovimentado(),
        item.getEstoqueMovimentacaoId(),
        false,
        item.getStatus(),
        item.getOrdem(),
        item.getObservacao()))
      .toList();

    return new MovementItemsBatchAddResponse(
      movimento.getId(),
      addedResponses.size(),
      addedResponses,
      allItems.size(),
      totalCobrado);
  }

  @Transactional
  public void undoStockMovement(Long movementId, Long itemId) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    Long normalizedMovementId = requirePositive(movementId, "movimento_estoque_id_invalid");
    Long normalizedItemId = requirePositive(itemId, "movimento_estoque_item_id_invalid");

    MovimentoEstoque movimento = movimentoEstoqueRepository.findWithLockByIdAndTenantId(normalizedMovementId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_not_found"));
    if (!Objects.equals(movimento.getEmpresaId(), empresaId)) {
      throw new EntityNotFoundException("movimento_estoque_not_found");
    }

    MovimentoEstoqueItem item = movimentoEstoqueItemRepository.findWithLockByIdAndTenantId(normalizedItemId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_item_not_found"));
    if (!Objects.equals(item.getMovimentoEstoqueId(), movimento.getId())) {
      throw new EntityNotFoundException("movimento_estoque_item_not_found");
    }
    if (!item.isEstoqueMovimentado() || item.getEstoqueMovimentacaoId() == null || item.getEstoqueMovimentacaoId() <= 0) {
      throw new IllegalArgumentException("movimento_estoque_item_stock_movement_not_found");
    }

    Long originalMovementId = item.getEstoqueMovimentacaoId();
    CatalogMovement originalMovement = catalogMovementRepository.findByIdAndTenantId(originalMovementId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_stock_movement_not_found"));
    List<CatalogMovementLine> originalLines = catalogMovementLineRepository
      .findAllByTenantIdAndMovementIdOrderByIdAsc(tenantId, originalMovementId);
    if (originalLines.isEmpty()) {
      throw new IllegalArgumentException("movimento_estoque_item_stock_movement_not_found");
    }

    List<CatalogMovementEngine.Impact> reverseImpacts = originalLines.stream()
      .map(line -> new CatalogMovementEngine.Impact(
        line.getAgrupadorEmpresaId(),
        line.getMetricType(),
        line.getEstoqueTipoId(),
        line.getFilialId(),
        normalize(line.getDelta()).negate()))
      .filter(impact -> impact.delta().compareTo(BigDecimal.ZERO) != 0)
      .toList();
    if (reverseImpacts.isEmpty()) {
      throw new IllegalArgumentException("movimento_estoque_item_stock_movement_not_found");
    }

    CatalogMovementEngine.Result reverseResult = catalogMovementEngine.apply(new CatalogMovementEngine.Command(
      tenantId,
      originalMovement.getCatalogType(),
      originalMovement.getCatalogoId(),
      originalMovement.getCatalogConfigurationId(),
      originalMovement.getAgrupadorEmpresaId(),
      CatalogMovementOriginType.SYSTEM,
      "UNDO_MOVIMENTO_ESTOQUE:" + movimento.getId(),
      "ITEM:" + item.getId() + ":UNDO_OF:" + originalMovementId,
      "Desfazer movimentacao de estoque do item " + (item.getCodigo() == null ? "-" : item.getCodigo()),
      buildUndoIdempotencyKey(item.getId(), originalMovementId),
      Instant.now(),
      originalMovement.getTenantUnitId(),
      originalMovement.getUnidadeBaseCatalogoTenantUnitId(),
      normalizeAbs(originalMovement.getQuantidadeInformada()),
      normalizeAbs(originalMovement.getQuantidadeConvertidaBase()),
      originalMovement.getFatorAplicado(),
      originalMovement.getFatorFonte(),
      reverseImpacts));

    item.setEstoqueMovimentado(false);
    item.setEstoqueMovimentacaoId(null);
    item.setEstoqueMovimentacaoChave(null);
    item.setEstoqueMovimentadoEm(null);
    item.setEstoqueMovimentadoPor(null);
    movimentoEstoqueItemRepository.save(item);

    auditService.log(
      tenantId,
      "MOVIMENTO_ESTOQUE_ITEM_DESFEITO",
      "movimento_estoque_item",
      String.valueOf(item.getId()),
      "movimentoId=" + movimento.getId()
        + ";itemCodigo=" + item.getCodigo()
        + ";catalogMovementOriginalId=" + originalMovementId
        + ";catalogMovementReversalId=" + reverseResult.movementId()
        + ";reused=" + reverseResult.reused());
  }

  private MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolveItem(
      MovimentoEstoque movimento,
      MovementItemAddRequest item,
      int fallbackOrder) {
    if (item == null) {
      throw new IllegalArgumentException("movimento_estoque_item_required");
    }
    MovimentoEstoqueItemRequest request = new MovimentoEstoqueItemRequest(
      item.movementItemTypeId(),
      item.catalogItemId(),
      item.tenantUnitId(),
      item.priceBookId(),
      item.variantId(),
      item.quantidade(),
      item.valorUnitario(),
      fallbackOrder,
      item.observacao());
    return movimentoEstoqueItemCatalogService.resolveItem(movimento.getMovimentoConfigId(), request, fallbackOrder);
  }

  private Map<Long, String> loadItemTypeNames(List<MovimentoEstoqueItem> items) {
    Set<Long> itemTypeIds = new LinkedHashSet<>();
    for (MovimentoEstoqueItem item : items) {
      if (item != null && item.getMovimentoItemTipoId() != null && item.getMovimentoItemTipoId() > 0) {
        itemTypeIds.add(item.getMovimentoItemTipoId());
      }
    }

    Map<Long, String> names = new HashMap<>();
    for (Long itemTypeId : itemTypeIds) {
      try {
        names.put(itemTypeId, movimentoItemTipoService.requireById(itemTypeId).getNome());
      } catch (Exception ex) {
        names.put(itemTypeId, "-");
      }
    }
    return names;
  }

  private long resolveNextCodigo(Long tenantId, Long movimentoId) {
    return movimentoEstoqueItemRepository
      .findTopByTenantIdAndMovimentoEstoqueIdOrderByCodigoDescIdDesc(tenantId, movimentoId)
      .map(item -> {
        Long codigo = item.getCodigo();
        if (codigo == null || codigo < 1) {
          return 1L;
        }
        return codigo + 1L;
      })
      .orElse(1L);
  }

  private String buildUndoIdempotencyKey(Long itemId, Long originalMovementId) {
    return "UNDO:ITEM:" + itemId + ":MOV:" + originalMovementId;
  }

  private BigDecimal normalize(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }
    return value.setScale(6, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizeAbs(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.abs().setScale(6, RoundingMode.HALF_UP);
  }

  private Map<UUID, String> loadUnitSiglas(Long tenantId, List<MovimentoEstoqueItem> items) {
    Set<UUID> unitIds = new LinkedHashSet<>();
    for (MovimentoEstoqueItem item : items) {
      if (item == null) {
        continue;
      }
      if (item.getTenantUnitId() != null) {
        unitIds.add(item.getTenantUnitId());
      }
      if (item.getUnidadeBaseCatalogoTenantUnitId() != null) {
        unitIds.add(item.getUnidadeBaseCatalogoTenantUnitId());
      }
    }
    if (unitIds.isEmpty()) {
      return java.util.Collections.emptyMap();
    }
    Map<UUID, String> map = new HashMap<>();
    tenantUnitRepository.findAllByTenantIdAndIdIn(tenantId, unitIds)
      .forEach(unit -> map.put(unit.getId(), unit.getSigla()));
    return map;
  }

  private Long requirePositive(Long value, String errorCode) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Long requireEmpresaContext() {
    Long empresaId = EmpresaContext.getEmpresaId();
    if (empresaId == null || empresaId <= 0) {
      throw new IllegalArgumentException("movimento_empresa_context_required");
    }
    return empresaId;
  }
}
