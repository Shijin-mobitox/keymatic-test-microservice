package com.kymatic.tenantservice.service.tenant;

import com.kymatic.tenantservice.dto.tenant.ActivityLogResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.ActivityLogEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

	private final ActivityLogRepository activityLogRepository;

	public ActivityLogService(ActivityLogRepository activityLogRepository) {
		this.activityLogRepository = activityLogRepository;
	}

	public List<ActivityLogResponse> getAllActivityLogs() {
		return activityLogRepository.findAll().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public Page<ActivityLogResponse> getActivityLogs(Pageable pageable) {
		return activityLogRepository.findAll(pageable)
			.map(this::toResponse);
	}

	public ActivityLogResponse getActivityLog(Long logId) {
		ActivityLogEntity entity = activityLogRepository.findById(logId)
			.orElseThrow(() -> new IllegalArgumentException("Activity log not found: " + logId));
		return toResponse(entity);
	}

	public List<ActivityLogResponse> getActivityLogsByUser(UUID userId) {
		return activityLogRepository.findByUserId(userId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public List<ActivityLogResponse> getActivityLogsByAction(String action) {
		return activityLogRepository.findByAction(action).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public List<ActivityLogResponse> getActivityLogsByEntity(String entityType, UUID entityId) {
		return activityLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	private ActivityLogResponse toResponse(ActivityLogEntity entity) {
		return new ActivityLogResponse(
			entity.getLogId(),
			entity.getUserId(),
			entity.getAction(),
			entity.getEntityType(),
			entity.getEntityId(),
			entity.getChanges(),
			entity.getCreatedAt()
		);
	}
}

