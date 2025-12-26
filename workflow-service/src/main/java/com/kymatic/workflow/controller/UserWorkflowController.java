package com.kymatic.workflow.controller;

import com.kymatic.workflow.dto.UserCreationRequest;
import com.kymatic.workflow.dto.WorkflowProcessResponse;
import com.kymatic.workflow.service.UserWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/workflows/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Workflows", description = "User management workflows")
public class UserWorkflowController {

    private final UserWorkflowService userWorkflowService;

    public UserWorkflowController(UserWorkflowService userWorkflowService) {
        this.userWorkflowService = userWorkflowService;
    }

    @Operation(summary = "Start user creation workflow")
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startUserCreation(
        @Valid @RequestBody UserCreationRequest request
    ) {
        WorkflowProcessResponse response = userWorkflowService.startUserCreation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "processInstanceId", response.processInstanceId(),
                "status", response.status()
            ));
    }

    @Operation(summary = "Start user role assignment workflow")
    @PostMapping(value = "/{userId}/roles/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startRoleAssignment(
        @PathVariable UUID userId,
        @RequestBody Map<String, Object> request
    ) {
        WorkflowProcessResponse response = userWorkflowService.startRoleAssignment(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "processInstanceId", response.processInstanceId(),
                "status", response.status()
            ));
    }

    @Operation(summary = "Start organization user assignment workflow")
    @PostMapping(value = "/{userId}/organizations/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startOrganizationAssignment(
        @PathVariable UUID userId,
        @RequestBody Map<String, Object> request
    ) {
        WorkflowProcessResponse response = userWorkflowService.startOrganizationAssignment(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "processInstanceId", response.processInstanceId(),
                "status", response.status()
            ));
    }
}
