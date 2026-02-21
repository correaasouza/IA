package com.ia.app.service;

import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.repository.WorkflowInstanceRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MovimentoEstoqueLockService {

  private final WorkflowInstanceRepository workflowInstanceRepository;

  public MovimentoEstoqueLockService(WorkflowInstanceRepository workflowInstanceRepository) {
    this.workflowInstanceRepository = workflowInstanceRepository;
  }

  public void assertMovimentoAbertoParaItens(Long tenantId, MovimentoEstoque movimento) {
    if (movimento == null || movimento.getId() == null) {
      return;
    }
    if (isMovimentoFinalizado(tenantId, movimento.getId(), movimento.getStatus())) {
      throw new IllegalArgumentException("movimento_estoque_finalizado");
    }
  }

  public void assertMovimentoPermiteAlterarItens(
      Long tenantId,
      MovimentoEstoque movimento,
      List<MovimentoEstoqueItem> itensAtuais) {
    assertMovimentoAbertoParaItens(tenantId, movimento);
    Map<Long, Boolean> itemFinalizadoById = mapItensFinalizados(
      tenantId,
      itensAtuais == null ? List.of() : itensAtuais.stream()
        .map(MovimentoEstoqueItem::getId)
        .filter(id -> id != null && id > 0)
        .toList());
    for (MovimentoEstoqueItem item : itensAtuais == null ? List.<MovimentoEstoqueItem>of() : itensAtuais) {
      if (item == null) {
        continue;
      }
      if (item.isEstoqueMovimentado()) {
        throw new IllegalArgumentException("movimento_estoque_item_locked_by_stock_movement");
      }
      if (isItemFinalizado(item, itemFinalizadoById)) {
        throw new IllegalArgumentException("movimento_estoque_item_finalizado");
      }
    }
  }

  public boolean isMovimentoFinalizado(Long tenantId, Long movimentoId, String status) {
    if (movimentoId != null && movimentoId > 0) {
      boolean terminalState = workflowInstanceRepository
        .findByTenantIdAndOriginAndEntityId(tenantId, WorkflowOrigin.MOVIMENTO_ESTOQUE, movimentoId)
        .map(instance -> instance.getCurrentState() != null && instance.getCurrentState().isTerminal())
        .orElse(false);
      if (terminalState) {
        return true;
      }
    }
    return statusLooksFinalized(status);
  }

  public Map<Long, Boolean> mapItensFinalizados(Long tenantId, Collection<Long> itemIds) {
    Map<Long, Boolean> map = new HashMap<>();
    List<Long> ids = (itemIds == null ? List.<Long>of() : itemIds.stream()
      .filter(id -> id != null && id > 0)
      .distinct()
      .toList());
    if (ids.isEmpty()) {
      return map;
    }
    workflowInstanceRepository.findAllByTenantIdAndOriginAndEntityIdIn(
      tenantId,
      WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
      ids).forEach(instance -> {
        if (instance == null || instance.getEntityId() == null) {
          return;
        }
        boolean terminal = instance.getCurrentState() != null && instance.getCurrentState().isTerminal();
        map.put(instance.getEntityId(), terminal);
      });
    return map;
  }

  public boolean isItemFinalizado(MovimentoEstoqueItem item, Map<Long, Boolean> itemFinalizadoById) {
    if (item == null) {
      return false;
    }
    Long itemId = item.getId();
    if (itemId != null && itemId > 0 && Boolean.TRUE.equals(itemFinalizadoById.get(itemId))) {
      return true;
    }
    return statusLooksFinalized(item.getStatus());
  }

  private boolean statusLooksFinalized(String status) {
    if (status == null || status.isBlank()) {
      return false;
    }
    String normalized = status.trim().toUpperCase(Locale.ROOT);
    return normalized.equals("FINAL")
      || normalized.equals("FINALIZADO")
      || normalized.equals("FINALIZADA")
      || normalized.equals("CONCLUIDO")
      || normalized.equals("CONCLUIDA")
      || normalized.equals("ENCERRADO")
      || normalized.equals("ENCERRADA")
      || normalized.equals("DONE")
      || normalized.equals("COMPLETED")
      || normalized.equals("CLOSED")
      || normalized.equals("FINALIZED");
  }
}
