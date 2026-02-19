package com.ia.app.workflow.engine;

import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.workflow.domain.WorkflowOrigin;
import org.springframework.stereotype.Component;

@Component
public class MovimentoEstoqueItemOriginResolver implements WorkflowOriginResolver {

  private final MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;

  public MovimentoEstoqueItemOriginResolver(MovimentoEstoqueItemRepository movimentoEstoqueItemRepository) {
    this.movimentoEstoqueItemRepository = movimentoEstoqueItemRepository;
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
  public void syncStatus(Long tenantId, Long entityId, String stateKey) {
    MovimentoEstoqueItem entity = movimentoEstoqueItemRepository.findByIdAndTenantId(entityId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    entity.setStatus(stateKey);
    movimentoEstoqueItemRepository.save(entity);
  }
}
