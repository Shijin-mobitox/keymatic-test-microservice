package com.kymatic.tenantservice.controller.tenant;

import com.kymatic.tenantservice.dto.tenant.TaskRequest;
import com.kymatic.tenantservice.dto.tenant.TaskResponse;
import com.kymatic.tenantservice.service.tenant.TenantTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Task Management", description = "Manage tenant-specific tasks")
public class TaskController {

	private final TenantTaskService taskService;

	public TaskController(TenantTaskService taskService) {
		this.taskService = taskService;
	}

	@Operation(summary = "Create a new task")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
		TaskResponse response = taskService.createTask(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get all tasks")
	@GetMapping
	public ResponseEntity<List<TaskResponse>> getAllTasks() {
		List<TaskResponse> tasks = taskService.getAllTasks();
		return ResponseEntity.ok(tasks);
	}

	@Operation(summary = "Get task by ID")
	@GetMapping("/{taskId}")
	public ResponseEntity<TaskResponse> getTask(
		@Parameter(description = "Task ID") @PathVariable UUID taskId
	) {
		TaskResponse response = taskService.getTask(taskId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get tasks by project")
	@GetMapping("/project/{projectId}")
	public ResponseEntity<List<TaskResponse>> getTasksByProject(
		@Parameter(description = "Project ID") @PathVariable UUID projectId
	) {
		List<TaskResponse> tasks = taskService.getTasksByProject(projectId);
		return ResponseEntity.ok(tasks);
	}

	@Operation(summary = "Get tasks by assignee")
	@GetMapping("/assignee/{assignedTo}")
	public ResponseEntity<List<TaskResponse>> getTasksByAssignee(
		@Parameter(description = "Assignee ID") @PathVariable UUID assignedTo
	) {
		List<TaskResponse> tasks = taskService.getTasksByAssignee(assignedTo);
		return ResponseEntity.ok(tasks);
	}

	@Operation(summary = "Update task")
	@PutMapping("/{taskId}")
	public ResponseEntity<TaskResponse> updateTask(
		@Parameter(description = "Task ID") @PathVariable UUID taskId,
		@Valid @RequestBody TaskRequest request
	) {
		TaskResponse response = taskService.updateTask(taskId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete task")
	@DeleteMapping("/{taskId}")
	@ApiResponse(responseCode = "204", description = "Task deleted")
	public ResponseEntity<Void> deleteTask(
		@Parameter(description = "Task ID") @PathVariable UUID taskId
	) {
		taskService.deleteTask(taskId);
		return ResponseEntity.noContent().build();
	}
}

