package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
	List<ProjectEntity> findByOwnerId(UUID ownerId);
	List<ProjectEntity> findByStatus(String status);
}

