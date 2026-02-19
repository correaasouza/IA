package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

  List<WorkflowState> findAllByDefinitionIdOrderByIdAsc(Long definitionId);

  Optional<WorkflowState> findByDefinitionIdAndStateKey(Long definitionId, String stateKey);

  Optional<WorkflowState> findFirstByDefinitionIdAndInitialTrueOrderByIdAsc(Long definitionId);

  Optional<WorkflowState> findFirstByDefinitionIdAndNameIgnoreCaseOrderByIdAsc(Long definitionId, String name);

  void deleteAllByDefinitionId(Long definitionId);
}
