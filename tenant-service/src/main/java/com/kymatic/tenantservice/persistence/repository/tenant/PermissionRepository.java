package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByTenantIdAndPermissionKey(String tenantId, String permissionKey);
    List<PermissionEntity> findByTenantIdAndPermissionKeyIn(String tenantId, List<String> keys);
    List<PermissionEntity> findByTenantId(String tenantId);
}

