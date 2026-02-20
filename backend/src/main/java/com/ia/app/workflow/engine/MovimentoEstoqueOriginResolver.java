package com.ia.app.workflow.engine;

import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.workflow.domain.WorkflowDefinitionContext;
import com.ia.app.workflow.domain.WorkflowDefinitionContextType;
import com.ia.app.workflow.domain.WorkflowOrigin;
import org.springframework.stereotype.Component;

@Component
public class MovimentoEstoqueOriginResolver implements WorkflowOriginResolver {

  private final MovimentoEstoqueRepository movimentoEstoqueRepository;

  public MovimentoEstoqueOriginResolver(MovimentoEstoqueRepository movimentoEstoqueRepository) {
    this.movimentoEstoqueRepository = movimentoEstoqueRepository;
  }

  @Override
  public WorkflowOrigin supports() {
    return WorkflowOrigin.MOVIMENTO_ESTOQUE;
  }

  @Override
  public boolean exists(Long tenantId, Long entityId) {
    return movimentoEstoqueRepository.findByIdAndTenantId(entityId, tenantId).isPresent();
  }

  @Override
  public WorkflowDefinitionContext resolveDefinitionContext(Long tenantId, Long entityId) {
    MovimentoEstoque entity = movimentoEstoqueRepository.findByIdAndTenantId(entityId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    return WorkflowDefinitionContext.of(WorkflowDefinitionContextType.MOVIMENTO_CONFIG, entity.getMovimentoConfigId());
  }

  @Override
  public void syncStatus(Long tenantId, Long entityId, String stateKey) {
    MovimentoEstoque entity = movimentoEstoqueRepository.findByIdAndTenantId(entityId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("workflow_entity_not_found"));
    entity.setStatus(stateKey);
    movimentoEstoqueRepository.save(entity);
  }
}
