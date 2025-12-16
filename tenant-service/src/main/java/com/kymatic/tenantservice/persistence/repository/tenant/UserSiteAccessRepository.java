package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.UserSiteAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSiteAccessRepository extends JpaRepository<UserSiteAccessEntity, UUID> {
    Optional<UserSiteAccessEntity> findByTenantIdAndUserIdAndSiteId(String tenantId, UUID userId, UUID siteId);
    List<UserSiteAccessEntity> findByTenantIdAndUserIdAndActiveTrue(String tenantId, UUID userId);
}

