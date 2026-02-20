package com.ia.app.workflow.service;

import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowDefinitionContextType;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowTriggerType;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.dto.WorkflowDefinitionUpsertRequest;
import com.ia.app.workflow.dto.WorkflowStateDefinitionRequest;
import com.ia.app.workflow.dto.WorkflowTransitionDefinitionRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WorkflowValidationService {

  public List<String> validateUpsert(WorkflowDefinitionUpsertRequest request) {
    List<String> errors = new ArrayList<>();
    if (request == null) {
      errors.add("workflow_payload_required");
      return errors;
    }
    WorkflowOrigin origin = null;
    try {
      origin = WorkflowOrigin.from(request.origin());
    } catch (RuntimeException ex) {
      errors.add("workflow_origin_invalid");
    }
    try {
      WorkflowDefinitionContextType.fromNullable(request.contextType());
    } catch (RuntimeException ex) {
      errors.add("workflow_context_invalid");
    }
    if ((request.contextType() == null) != (request.contextId() == null)) {
      errors.add("workflow_context_invalid");
    }
    if (request.contextId() != null && request.contextId() <= 0) {
      errors.add("workflow_context_id_invalid");
    }
    if (request.name() == null || request.name().isBlank()) {
      errors.add("workflow_name_required");
    }

    List<WorkflowStateDefinitionRequest> states = request.states() == null ? List.of() : request.states();
    if (states.isEmpty()) {
      errors.add("workflow_state_required");
      return errors;
    }

    Set<String> stateKeys = new HashSet<>();
    int initialCount = 0;
    for (WorkflowStateDefinitionRequest state : states) {
      if (state == null) {
        errors.add("workflow_state_invalid");
        continue;
      }
      String key = normalizeKey(state.key());
      if (key == null) {
        errors.add("workflow_state_key_required");
      } else if (!stateKeys.add(key)) {
        errors.add("workflow_state_key_duplicated");
      }
      if (Boolean.TRUE.equals(state.isInitial())) {
        initialCount += 1;
      }
    }
    if (initialCount != 1) {
      errors.add("workflow_state_initial_exactly_one");
    }

    Set<String> transitionKeys = new HashSet<>();
    List<WorkflowTransitionDefinitionRequest> transitions = request.transitions() == null ? List.of() : request.transitions();
    for (WorkflowTransitionDefinitionRequest transition : transitions) {
      if (transition == null) {
        errors.add("workflow_transition_invalid");
        continue;
      }
      String transitionKey = normalizeKey(transition.key());
      if (transitionKey == null) {
        errors.add("workflow_transition_key_required");
      } else if (!transitionKeys.add(transitionKey)) {
        errors.add("workflow_transition_key_duplicated");
      }

      String fromStateKey = normalizeKey(transition.fromStateKey());
      String toStateKey = normalizeKey(transition.toStateKey());
      if (fromStateKey == null || toStateKey == null) {
        errors.add("workflow_transition_state_required");
      } else {
        if (fromStateKey.equals(toStateKey)) {
          errors.add("workflow_transition_same_state");
        }
        if (!stateKeys.contains(fromStateKey)) {
          errors.add("workflow_transition_from_state_not_found");
        }
        if (!stateKeys.contains(toStateKey)) {
          errors.add("workflow_transition_to_state_not_found");
        }
      }

      List<WorkflowActionConfigRequest> actions = transition.actions() == null ? List.of() : transition.actions();
      for (WorkflowActionConfigRequest action : actions) {
        if (action == null) {
          errors.add("workflow_action_invalid");
          continue;
        }
        WorkflowActionType actionType;
        try {
          actionType = WorkflowActionType.from(action.type());
        } catch (RuntimeException ex) {
          errors.add("workflow_action_type_invalid");
          continue;
        }
        if (actionType == WorkflowActionType.MOVE_STOCK && origin != WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE) {
          errors.add("workflow_action_move_stock_origin_invalid");
        }
        if (actionType == WorkflowActionType.SET_ITEM_STATUS && origin != WorkflowOrigin.MOVIMENTO_ESTOQUE) {
          errors.add("workflow_action_set_item_status_origin_invalid");
        }
        try {
          WorkflowTriggerType.from(action.trigger() == null ? "ON_TRANSITION" : action.trigger());
        } catch (RuntimeException ex) {
          errors.add("workflow_action_trigger_invalid");
        }
      }
    }
    return errors;
  }

  private String normalizeKey(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }
}
