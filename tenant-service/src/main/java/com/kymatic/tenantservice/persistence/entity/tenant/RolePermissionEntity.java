package com.kymatic.tenantservice.persistence.entity.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {

    @Id
    @Column(name = "role_permission_id", nullable = false)
    private UUID rolePermissionId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public RolePermissionEntity() {
        this.rolePermissionId = UUID.randomUUID();
    }

    public UUID getRolePermissionId() {
        return rolePermissionId;
    }

    public void setRolePermissionId(UUID rolePermissionId) {
        this.rolePermissionId = rolePermissionId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

