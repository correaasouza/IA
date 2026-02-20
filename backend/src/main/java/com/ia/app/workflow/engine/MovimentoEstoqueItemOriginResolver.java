package com.ia.app.workflow.engine;

import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.workflow.domain.WorkflowDefinitionContext;
import com.ia.app.workflow.domain.WorkflowDefinitionContextType;
import com.ia.app.workflow.domain.WorkflowOrigin;
import org.springframework.stereotype.Component;

@Component
public class MovimentoEstoqueItemOriginResolver implements WorkflowOriginResolver {

  private final MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;
  private final MovimentoEstoqueRepository movimentoEstoqueRepository;

  public MovimentoEstoqueItemOriginResolver(
      MovimentoEstoqueItemRepository movimentoEstoqueItemRepository,
      MovimentoEstoqueRepository movimentoEstoqueRepository) {
    this.movimentoEstoqueItemRepository = movimentoEstoqueItemRepository;
    this.movimentoEstoqueRepository = movimentoEstoqueRepository;
  }

  @Override
  public WorkflowOrigin supports() {
    return WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE;
  }

  @Override
  public boolean exists(Long tenantId, Long entityId) {
    return movimentoEstoqueItemRepository.findByIdAndTenantId(entityId, tenantId).isPresent();
  }

  @Override
  public WorkflowDefinitionContext resolveDefinitionContext(Long tenantId, Long entityId) {
    MovimentoEstoqueItem item = movimentoEstoqueItemRepository.findByIdAndTenantId(entityId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    Long movimentoEstoqueId = item.getMovimentoEstoqueId();
    if (movimentoEstoqueId == null || movimentoEstoqueId <= 0) {
      return WorkflowDefinitionContext.none();
    }
    Long movimentoConfigId = movimentoEstoqueRepository.findByIdAndTenantId(movimentoEstoqueId, tenantId)
      .map(movimento -> movimento.getMovimentoConfigId())
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    return WorkflowDefinitionContext.of(WorkflowDefinitionContextType.MOVIMENTO_CONFIG, movimentoConfigId);
  }

  @Override
  public void syncStatus(Long tenantId, Long entityId, String stateKey) {
    MovimentoEstoqueItem entity = movimentoEstoqueItemRepository.findByIdAndTenantId(entityId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    entity.setStatus(stateKey);
    movimentoEstoqueItemRepository.save(entity);
  }
}
