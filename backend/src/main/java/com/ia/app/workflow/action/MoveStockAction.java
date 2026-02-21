package com.ia.app.workflow.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockAdjustment;
import com.ia.app.domain.CatalogStockAdjustmentType;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.CatalogStockAdjustmentRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.service.CatalogMovementEngine;
import com.ia.app.service.CatalogUnitLockService;
import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowExecutionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionResult;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MoveStockAction implements WorkflowAction {

  private final MovimentoEstoqueItemRepository itemRepository;
  private final MovimentoEstoqueRepository movimentoRepository;
  private final CatalogStockAdjustmentRepository stockAdjustmentRepository;
  private final CatalogMovementEngine catalogMovementEngine;
  private final CatalogUnitLockService catalogUnitLockService;
  private final ObjectMapper objectMapper;

  public MoveStockAction(
      MovimentoEstoqueItemRepository itemRepository,
      MovimentoEstoqueRepository movimentoRepository,
      CatalogStockAdjustmentRepository stockAdjustmentRepository,
      CatalogMovementEngine catalogMovementEngine,
      CatalogUnitLockService catalogUnitLockService,
      ObjectMapper objectMapper) {
    this.itemRepository = itemRepository;
    this.movimentoRepository = movimentoRepository;
    this.stockAdjustmentRepository = stockAdjustmentRepository;
    this.catalogMovementEngine = catalogMovementEngine;
    this.catalogUnitLockService = catalogUnitLockService;
    this.objectMapper = objectMapper;
  }

  @Override
  public WorkflowActionType supports() {
    return WorkflowActionType.MOVE_STOCK;
  }

  @Override
  public WorkflowActionResult execute(WorkflowActionContext context, WorkflowActionConfigRequest config) {
    if (context.instance().getOrigin() != WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE) {
      throw new IllegalArgumentException("workflow_action_move_stock_origin_invalid");
    }
    Long tenantId = context.tenantId();
    Long itemId = context.instance().getEntityId();
    MovimentoEstoqueItem item = itemRepository.findWithLockByIdAndTenantId(itemId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_item_not_found"));
    MovimentoEstoque movimento = movimentoRepository.findByIdAndTenantId(item.getMovimentoEstoqueId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_not_found"));

    if (movimento.getStockAdjustmentId() == null || movimento.getStockAdjustmentId() <= 0) {
      throw new IllegalArgumentException("movimento_estoque_stock_adjustment_required");
    }
    CatalogStockAdjustment adjustment = stockAdjustmentRepository
      .findByIdAndTenantIdAndActiveTrue(movimento.getStockAdjustmentId(), tenantId)
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_stock_adjustment_invalid"));

    String idempotencyKey = buildIdempotencyKey(item, context);
    if (item.isEstoqueMovimentado() && idempotencyKey.equals(item.getEstoqueMovimentacaoChave())) {
      Map<String, Object> reusedResult = new LinkedHashMap<>();
      reusedResult.put("catalogMovementId", item.getEstoqueMovimentacaoId());
      reusedResult.put("reused", true);
      String reusedJson = writeJson(reusedResult);
      return new WorkflowActionResult(
        WorkflowActionType.MOVE_STOCK.name(),
        WorkflowExecutionStatus.SUCCESS,
        idempotencyKey,
        reusedJson,
        null);
    }

    CatalogStockAdjustmentType adjustmentType = CatalogStockAdjustmentType.from(adjustment.getTipo());
    BigDecimal quantidade = normalize(item.getQuantidadeConvertidaBase() == null ? item.getQuantidade() : item.getQuantidadeConvertidaBase());
    BigDecimal valorTotal = normalize(item.getValorTotal());

    List<CatalogMovementEngine.Impact> impacts = new ArrayList<>();
    if (adjustmentType == CatalogStockAdjustmentType.ENTRADA) {
      addScopeImpacts(
        impacts,
        adjustment.getEstoqueDestinoAgrupadorId(),
        adjustment.getEstoqueDestinoTipoId(),
        adjustment.getEstoqueDestinoFilialId(),
        quantidade,
        valorTotal);
    } else if (adjustmentType == CatalogStockAdjustmentType.SAIDA) {
      addScopeImpacts(
        impacts,
        adjustment.getEstoqueOrigemAgrupadorId(),
        adjustment.getEstoqueOrigemTipoId(),
        adjustment.getEstoqueOrigemFilialId(),
        quantidade.negate(),
        valorTotal.negate());
    } else {
      addScopeImpacts(
        impacts,
        adjustment.getEstoqueOrigemAgrupadorId(),
        adjustment.getEstoqueOrigemTipoId(),
        adjustment.getEstoqueOrigemFilialId(),
        quantidade.negate(),
        valorTotal.negate());
      addScopeImpacts(
        impacts,
        adjustment.getEstoqueDestinoAgrupadorId(),
        adjustment.getEstoqueDestinoTipoId(),
        adjustment.getEstoqueDestinoFilialId(),
        quantidade,
        valorTotal);
    }

    Long headerAgrupadorId = adjustment.getEstoqueDestinoAgrupadorId() != null
      ? adjustment.getEstoqueDestinoAgrupadorId()
      : adjustment.getEstoqueOrigemAgrupadorId();
    if (headerAgrupadorId == null || headerAgrupadorId <= 0) {
      throw new IllegalArgumentException("catalog_stock_adjustment_agrupador_not_found");
    }

    CatalogMovementEngine.Command command = new CatalogMovementEngine.Command(
      tenantId,
      item.getCatalogType(),
      item.getCatalogItemId(),
      adjustment.getCatalogConfigurationId(),
      headerAgrupadorId,
      CatalogMovementOriginType.WORKFLOW_ACTION,
      "MOVIMENTO_ESTOQUE:" + movimento.getId(),
      "ITEM:" + item.getId(),
      context.transitionNotes(),
      idempotencyKey,
      Instant.now(),
      item.getTenantUnitId(),
      item.getUnidadeBaseCatalogoTenantUnitId(),
      normalize(item.getQuantidade()),
      normalize(item.getQuantidadeConvertidaBase() == null ? item.getQuantidade() : item.getQuantidadeConvertidaBase()),
      item.getFatorAplicado(),
      item.getFatorFonte(),
      impacts);

    CatalogMovementEngine.Result result = catalogMovementEngine.apply(command);

    item.setEstoqueMovimentado(true);
    item.setEstoqueMovimentacaoChave(idempotencyKey);
    item.setEstoqueMovimentacaoId(result.movementId());
    item.setEstoqueMovimentadoEm(Instant.now());
    item.setEstoqueMovimentadoPor(context.username());
    itemRepository.save(item);
    catalogUnitLockService.markHasStockMovements(item.getTenantId(), item.getCatalogType(), item.getCatalogItemId());

    Map<String, Object> resultData = new LinkedHashMap<>();
    resultData.put("catalogMovementId", result.movementId());
    resultData.put("reused", result.reused());
    resultData.put("adjustmentType", adjustmentType.name());
    String resultJson = writeJson(resultData);
    return new WorkflowActionResult(
      WorkflowActionType.MOVE_STOCK.name(),
      WorkflowExecutionStatus.SUCCESS,
      idempotencyKey,
      resultJson,
      null);
  }

  private void addScopeImpacts(
      List<CatalogMovementEngine.Impact> impacts,
      Long agrupadorId,
      Long estoqueTipoId,
      Long filialId,
      BigDecimal quantidadeDelta,
      BigDecimal precoDelta) {
    if (agrupadorId == null || estoqueTipoId == null || filialId == null) {
      throw new IllegalArgumentException("catalog_stock_adjustment_scope_invalid");
    }
    impacts.add(new CatalogMovementEngine.Impact(
      agrupadorId,
      CatalogMovementMetricType.QUANTIDADE,
      estoqueTipoId,
      filialId,
      normalize(quantidadeDelta)));
    impacts.add(new CatalogMovementEngine.Impact(
      agrupadorId,
      CatalogMovementMetricType.PRECO,
      estoqueTipoId,
      filialId,
      normalize(precoDelta)));
  }

  private String buildIdempotencyKey(MovimentoEstoqueItem item, WorkflowActionContext context) {
    return "WF:ITEM:"
      + item.getId()
      + ":TRANS:"
      + context.transition().getTransitionKey()
      + ":DEF:"
      + context.instance().getDefinitionVersion();
  }

  private BigDecimal normalize(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(6, java.math.RoundingMode.HALF_UP);
    }
    return value.setScale(6, java.math.RoundingMode.HALF_UP);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return null;
    }
  }
}
