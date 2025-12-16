package com.kymatic.tenantservice.persistence.repository;

import com.kymatic.tenantservice.persistence.entity.TenantMigrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantMigrationRepository extends JpaRepository<TenantMigrationEntity, Long> {
	List<TenantMigrationEntity> findByTenantIdOrderByAppliedAtDesc(UUID tenantId);

	boolean existsByTenantIdAndVersion(UUID tenantId, String version);
}


