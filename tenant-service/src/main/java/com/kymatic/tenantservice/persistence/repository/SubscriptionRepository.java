package com.kymatic.tenantservice.persistence.repository;

import com.kymatic.tenantservice.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
	List<SubscriptionEntity> findByTenantId(UUID tenantId);
	List<SubscriptionEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}

