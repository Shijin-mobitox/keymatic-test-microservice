package com.kymatic.tenantservice.service.tenant;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.tenantservice.dto.tenant.TaskRequest;
import com.kymatic.tenantservice.dto.tenant.TaskResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.TaskEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("tenantTaskService")
public class TenantTaskService {

	private final TaskRepository taskRepository;

	public TenantTaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Transactional
	public TaskResponse createTask(TaskRequest request) {
		String tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			throw new IllegalStateException("Tenant ID is required");
		}

		TaskEntity entity = new TaskEntity();
		entity.setTenantId(tenantId);
		entity.setProjectId(request.projectId());
		entity.setTitle(request.title());
		entity.setDescription(request.description());
		entity.setAssignedTo(request.assignedTo());
		entity.setStatus(request.status());
		entity.setDueDate(request.dueDate());

		TaskEntity saved = taskRepository.save(entity);
		return toResponse(saved);
	}

	@org.springframework.cache.annotation.Cacheable(value = "tasks", key = "'all-' + T(com.kymatic.shared.multitenancy.TenantContext).getTenantId()")
	public List<TaskResponse> getAllTasks() {
		return taskRepository.findAll().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public TaskResponse getTask(UUID taskId) {
		TaskEntity entity = taskRepository.findById(taskId)
			.orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
		return toResponse(entity);
	}

	public List<TaskResponse> getTasksByProject(UUID projectId) {
		return taskRepository.findByProjectId(projectId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public List<TaskResponse> getTasksByAssignee(UUID assignedTo) {
		return taskRepository.findByAssignedTo(assignedTo).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	@Transactional
	public TaskResponse updateTask(UUID taskId, TaskRequest request) {
		TaskEntity entity = taskRepository.findById(taskId)
			.orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

		if (request.projectId() != null) {
			entity.setProjectId(request.projectId());
		}
		if (request.title() != null) {
			entity.setTitle(request.title());
		}
		if (request.description() != null) {
			entity.setDescription(request.description());
		}
		if (request.assignedTo() != null) {
			entity.setAssignedTo(request.assignedTo());
		}
		if (request.status() != null) {
			entity.setStatus(request.status());
		}
		if (request.dueDate() != null) {
			entity.setDueDate(request.dueDate());
		}

		TaskEntity saved = taskRepository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void deleteTask(UUID taskId) {
		if (!taskRepository.existsById(taskId)) {
			throw new IllegalArgumentException("Task not found: " + taskId);
		}
		taskRepository.deleteById(taskId);
	}

	private TaskResponse toResponse(TaskEntity entity) {
		return new TaskResponse(
			entity.getTaskId(),
			entity.getProjectId(),
			entity.getTitle(),
			entity.getDescription(),
			entity.getAssignedTo(),
			entity.getStatus(),
			entity.getDueDate(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}
}

