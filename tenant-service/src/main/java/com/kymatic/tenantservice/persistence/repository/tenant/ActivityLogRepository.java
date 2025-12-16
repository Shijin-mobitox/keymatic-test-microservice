package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.ActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {
	List<ActivityLogEntity> findByUserId(UUID userId);
	Page<ActivityLogEntity> findByUserId(UUID userId, Pageable pageable);
	List<ActivityLogEntity> findByAction(String action);
	List<ActivityLogEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}

