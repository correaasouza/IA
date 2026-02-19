package com.ia.app.workflow.action;

import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionResult;

public interface WorkflowAction {

  WorkflowActionType supports();

  WorkflowActionResult execute(WorkflowActionContext context, WorkflowActionConfigRequest config);
}
