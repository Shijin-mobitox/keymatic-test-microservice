package com.kymatic.tenantservice.persistence.repository;

import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {
	Optional<TenantEntity> findBySlug(String slug);
}


