package com.kymatic.tenantservice.persistence.repository;

import com.kymatic.tenantservice.persistence.entity.TenantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUserEntity, UUID> {
	List<TenantUserEntity> findByTenantId(UUID tenantId);
	Optional<TenantUserEntity> findByTenantIdAndEmail(UUID tenantId, String email);
	boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}

