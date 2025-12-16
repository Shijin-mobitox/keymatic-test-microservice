package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByTenantIdAndRoleKeyAndDeletedAtIsNull(String tenantId, String roleKey);
    boolean existsByTenantIdAndRoleKeyAndDeletedAtIsNull(String tenantId, String roleKey);
    List<RoleEntity> findByTenantIdAndDeletedAtIsNull(String tenantId);
}

