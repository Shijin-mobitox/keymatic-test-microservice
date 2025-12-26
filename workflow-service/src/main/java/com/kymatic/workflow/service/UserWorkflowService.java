package com.kymatic.workflow.service;

import com.kymatic.workflow.dto.UserCreationRequest;
import com.kymatic.workflow.dto.WorkflowProcessResponse;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(UserWorkflowService.class);

    private final RuntimeService runtimeService;

    public UserWorkflowService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public WorkflowProcessResponse startUserCreation(UserCreationRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tenantId", request.tenantId());
        variables.put("email", request.email());
        variables.put("password", request.password());
        variables.put("firstName", request.firstName());
        variables.put("lastName", request.lastName());
        variables.put("emailVerified", request.emailVerified() != null ? request.emailVerified() : false);
        variables.put("role", request.role() != null ? request.role() : "user");

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("user-creation", variables);
        logger.info("Started user creation workflow instance {} for email: {}", 
            instance.getId(), request.email());
        return WorkflowProcessResponse.started(instance.getId());
    }

    public WorkflowProcessResponse startRoleAssignment(UUID userId, Map<String, Object> request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId.toString());
        variables.putAll(request);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("role-assignment", variables);
        logger.info("Started role assignment workflow instance {} for user: {}", 
            instance.getId(), userId);
        return WorkflowProcessResponse.started(instance.getId());
    }

    public WorkflowProcessResponse startOrganizationAssignment(UUID userId, Map<String, Object> request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId.toString());
        variables.putAll(request);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("organization-assignment", variables);
        logger.info("Started organization assignment workflow instance {} for user: {}", 
            instance.getId(), userId);
        return WorkflowProcessResponse.started(instance.getId());
    }
}
