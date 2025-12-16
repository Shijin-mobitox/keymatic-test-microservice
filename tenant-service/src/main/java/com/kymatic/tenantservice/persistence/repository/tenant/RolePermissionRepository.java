package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {
    List<RolePermissionEntity> findByRoleId(UUID roleId);
    void deleteByRoleId(UUID roleId);
}

