package com.ia.app.service;

import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.dto.MovementItemAddRequest;
import com.ia.app.dto.MovementItemsBatchAddRequest;
import com.ia.app.dto.MovementItemsBatchAddResponse;
import com.ia.app.dto.MovimentoEstoqueItemRequest;
import com.ia.app.dto.MovimentoEstoqueItemResponse;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoItemBatchService {

  private final MovimentoEstoqueRepository movimentoEstoqueRepository;
  private final MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;
  private final MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService;
  private final MovimentoItemTipoService movimentoItemTipoService;

  public MovimentoItemBatchService(
      MovimentoEstoqueRepository movimentoEstoqueRepository,
      MovimentoEstoqueItemRepository movimentoEstoqueItemRepository,
      MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService,
      MovimentoItemTipoService movimentoItemTipoService) {
    this.movimentoEstoqueRepository = movimentoEstoqueRepository;
    this.movimentoEstoqueItemRepository = movimentoEstoqueItemRepository;
    this.movimentoEstoqueItemCatalogService = movimentoEstoqueItemCatalogService;
    this.movimentoItemTipoService = movimentoItemTipoService;
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

    int nextOrder = movimentoEstoqueItemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(tenantId, movimento.getId())
      .size();

    List<MovimentoEstoqueItem> entities = new ArrayList<>(requestItems.size());
    for (MovementItemAddRequest item : requestItems) {
      MovimentoEstoqueItemCatalogService.ResolvedMovimentoItem resolved = resolveItem(movimento, item, nextOrder);
      MovimentoEstoqueItem entity = new MovimentoEstoqueItem();
      entity.setTenantId(tenantId);
      entity.setMovimentoEstoqueId(movimento.getId());
      entity.setMovimentoItemTipoId(resolved.movimentoItemTipoId());
      entity.setCatalogType(resolved.catalogType());
      entity.setCatalogItemId(resolved.catalogItemId());
      entity.setCatalogCodigoSnapshot(resolved.catalogCodigoSnapshot());
      entity.setCatalogNomeSnapshot(resolved.catalogNomeSnapshot());
      entity.setQuantidade(resolved.quantidade());
      entity.setValorUnitario(resolved.valorUnitario());
      entity.setValorTotal(resolved.valorTotal());
      entity.setCobrar(resolved.cobrar());
      entity.setOrdem(resolved.ordem());
      entity.setObservacao(resolved.observacao());
      entities.add(entity);
      nextOrder += 1;
    }

    List<MovimentoEstoqueItem> saved = movimentoEstoqueItemRepository.saveAll(entities);

    List<MovimentoEstoqueItem> allItems = movimentoEstoqueItemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(tenantId, movimento.getId());
    BigDecimal totalCobrado = allItems.stream()
      .map(MovimentoEstoqueItem::getValorTotal)
      .filter(Objects::nonNull)
      .reduce(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), BigDecimal::add);

    Map<Long, String> itemTypeNameById = loadItemTypeNames(saved);
    List<MovimentoEstoqueItemResponse> addedResponses = saved.stream()
      .map(item -> new MovimentoEstoqueItemResponse(
        item.getId(),
        item.getMovimentoItemTipoId(),
        itemTypeNameById.getOrDefault(item.getMovimentoItemTipoId(), "-"),
        item.getCatalogType(),
        item.getCatalogItemId(),
        item.getCatalogCodigoSnapshot(),
        item.getCatalogNomeSnapshot(),
        item.getQuantidade(),
        item.getValorUnitario(),
        item.getValorTotal(),
        item.isCobrar(),
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
