package com.kymatic.tenantservice.service.tenant;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.tenantservice.dto.tenant.ProjectRequest;
import com.kymatic.tenantservice.dto.tenant.ProjectResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.ProjectEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	@Transactional
	public ProjectResponse createProject(ProjectRequest request) {
		String tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			throw new IllegalStateException("Tenant ID is required");
		}

		ProjectEntity entity = new ProjectEntity();
		entity.setTenantId(tenantId);
		entity.setName(request.name());
		entity.setDescription(request.description());
		entity.setOwnerId(request.ownerId());
		entity.setStatus(request.status());

		ProjectEntity saved = projectRepository.save(entity);
		return toResponse(saved);
	}

	@org.springframework.cache.annotation.Cacheable(value = "projects", key = "'all-' + T(com.kymatic.shared.multitenancy.TenantContext).getTenantId()")
	public List<ProjectResponse> getAllProjects() {
		return projectRepository.findAll().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public ProjectResponse getProject(UUID projectId) {
		ProjectEntity entity = projectRepository.findById(projectId)
			.orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
		return toResponse(entity);
	}

	public List<ProjectResponse> getProjectsByOwner(UUID ownerId) {
		return projectRepository.findByOwnerId(ownerId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	@Transactional
	public ProjectResponse updateProject(UUID projectId, ProjectRequest request) {
		ProjectEntity entity = projectRepository.findById(projectId)
			.orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

		if (request.name() != null) {
			entity.setName(request.name());
		}
		if (request.description() != null) {
			entity.setDescription(request.description());
		}
		if (request.ownerId() != null) {
			entity.setOwnerId(request.ownerId());
		}
		if (request.status() != null) {
			entity.setStatus(request.status());
		}

		ProjectEntity saved = projectRepository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void deleteProject(UUID projectId) {
		if (!projectRepository.existsById(projectId)) {
			throw new IllegalArgumentException("Project not found: " + projectId);
		}
		projectRepository.deleteById(projectId);
	}

	private ProjectResponse toResponse(ProjectEntity entity) {
		return new ProjectResponse(
			entity.getProjectId(),
			entity.getName(),
			entity.getDescription(),
			entity.getOwnerId(),
			entity.getStatus(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}
}

