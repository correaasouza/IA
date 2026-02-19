package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowTransition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

  List<WorkflowTransition> findAllByDefinitionIdOrderByPriorityAscIdAsc(Long definitionId);

  Optional<WorkflowTransition> findByDefinitionIdAndTransitionKey(Long definitionId, String transitionKey);

  List<WorkflowTransition> findAllByDefinitionIdAndFromStateIdAndEnabledTrueOrderByPriorityAscIdAsc(Long definitionId, Long fromStateId);

  void deleteAllByDefinitionId(Long definitionId);
}
