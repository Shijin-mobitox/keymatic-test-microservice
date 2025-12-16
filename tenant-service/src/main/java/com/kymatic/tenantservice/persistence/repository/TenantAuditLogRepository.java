package com.kymatic.tenantservice.persistence.repository;

import com.kymatic.tenantservice.persistence.entity.TenantAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLogEntity, Long> {
	List<TenantAuditLogEntity> findByTenantId(UUID tenantId);
	Page<TenantAuditLogEntity> findByTenantId(UUID tenantId, Pageable pageable);
	List<TenantAuditLogEntity> findByTenantIdAndAction(UUID tenantId, String action);
}

