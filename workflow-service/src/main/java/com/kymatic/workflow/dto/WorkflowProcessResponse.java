package com.kymatic.workflow.dto;

public record WorkflowProcessResponse(
    String processInstanceId,
    String status
) {

    public static WorkflowProcessResponse started(String processInstanceId) {
        return new WorkflowProcessResponse(processInstanceId, "workflow_started");
    }
}

