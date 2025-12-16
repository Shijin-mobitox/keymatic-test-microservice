package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.AuditLogResponse;
import com.kymatic.tenantservice.persistence.entity.TenantAuditLogEntity;
import com.kymatic.tenantservice.persistence.repository.TenantAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

	private final TenantAuditLogRepository auditLogRepository;

	public AuditLogService(TenantAuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void logAction(UUID tenantId, String action, UUID performedBy, Object details) {
		TenantAuditLogEntity entity = new TenantAuditLogEntity();
		entity.setTenantId(tenantId);
		entity.setAction(action);
		entity.setPerformedBy(performedBy);
		// Convert details to JsonNode if needed
		if (details != null) {
			// This would need Jackson ObjectMapper to convert to JsonNode
			// For now, we'll leave it as null and handle it in the controller
		}
		auditLogRepository.save(entity);
	}

	public List<AuditLogResponse> getAuditLogsByTenant(UUID tenantId) {
		return auditLogRepository.findByTenantId(tenantId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public Page<AuditLogResponse> getAuditLogsByTenant(UUID tenantId, Pageable pageable) {
		return auditLogRepository.findByTenantId(tenantId, pageable)
			.map(this::toResponse);
	}

	public List<AuditLogResponse> getAuditLogsByAction(UUID tenantId, String action) {
		return auditLogRepository.findByTenantIdAndAction(tenantId, action).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public AuditLogResponse getAuditLog(Long logId) {
		TenantAuditLogEntity entity = auditLogRepository.findById(logId)
			.orElseThrow(() -> new IllegalArgumentException("Audit log not found: " + logId));
		return toResponse(entity);
	}

	private AuditLogResponse toResponse(TenantAuditLogEntity entity) {
		return new AuditLogResponse(
			entity.getLogId(),
			entity.getTenantId(),
			entity.getAction(),
			entity.getPerformedBy(),
			entity.getDetails(),
			entity.getCreatedAt()
		);
	}
}

