package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
    List<UserRoleEntity> findByTenantIdAndUserIdAndActiveTrue(String tenantId, UUID userId);
    Optional<UserRoleEntity> findByTenantIdAndUserIdAndRoleIdAndSiteId(String tenantId, UUID userId, UUID roleId, UUID siteId);
}

