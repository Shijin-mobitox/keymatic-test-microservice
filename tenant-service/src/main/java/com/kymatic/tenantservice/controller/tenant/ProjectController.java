package com.kymatic.tenantservice.controller.tenant;

import com.kymatic.tenantservice.dto.tenant.ProjectRequest;
import com.kymatic.tenantservice.dto.tenant.ProjectResponse;
import com.kymatic.tenantservice.service.tenant.ProjectService;
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
@RequestMapping(path = "/api/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Project Management", description = "Manage tenant-specific projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@Operation(summary = "Create a new project")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
		ProjectResponse response = projectService.createProject(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get all projects")
	@GetMapping
	public ResponseEntity<List<ProjectResponse>> getAllProjects() {
		List<ProjectResponse> projects = projectService.getAllProjects();
		return ResponseEntity.ok(projects);
	}

	@Operation(summary = "Get project by ID")
	@GetMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> getProject(
		@Parameter(description = "Project ID") @PathVariable UUID projectId
	) {
		ProjectResponse response = projectService.getProject(projectId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get projects by owner")
	@GetMapping("/owner/{ownerId}")
	public ResponseEntity<List<ProjectResponse>> getProjectsByOwner(
		@Parameter(description = "Owner ID") @PathVariable UUID ownerId
	) {
		List<ProjectResponse> projects = projectService.getProjectsByOwner(ownerId);
		return ResponseEntity.ok(projects);
	}

	@Operation(summary = "Update project")
	@PutMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> updateProject(
		@Parameter(description = "Project ID") @PathVariable UUID projectId,
		@Valid @RequestBody ProjectRequest request
	) {
		ProjectResponse response = projectService.updateProject(projectId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete project")
	@DeleteMapping("/{projectId}")
	@ApiResponse(responseCode = "204", description = "Project deleted")
	public ResponseEntity<Void> deleteProject(
		@Parameter(description = "Project ID") @PathVariable UUID projectId
	) {
		projectService.deleteProject(projectId);
		return ResponseEntity.noContent().build();
	}
}

