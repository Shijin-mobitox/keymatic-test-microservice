package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
	List<TaskEntity> findByProjectId(UUID projectId);
	List<TaskEntity> findByAssignedTo(UUID assignedTo);
	List<TaskEntity> findByStatus(String status);
}

