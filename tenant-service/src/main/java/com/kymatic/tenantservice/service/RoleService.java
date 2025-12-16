package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.rbac.CreateRoleRequest;
import com.kymatic.tenantservice.dto.rbac.PermissionResponse;
import com.kymatic.tenantservice.dto.rbac.RoleResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.PermissionEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.RoleEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.RolePermissionEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.RolePermissionRepository;
import com.kymatic.tenantservice.persistence.repository.tenant.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;
    private final TenantScopeService tenantScopeService;

    public RoleService(
        RoleRepository roleRepository,
        RolePermissionRepository rolePermissionRepository,
        PermissionService permissionService,
        TenantScopeService tenantScopeService
    ) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionService = permissionService;
        this.tenantScopeService = tenantScopeService;
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String tenantId = tenantScopeService.requireTenantId();
        if (roleRepository.existsByTenantIdAndRoleKeyAndDeletedAtIsNull(tenantId, request.roleKey())) {
            throw new IllegalArgumentException("Role key already exists: " + request.roleKey());
        }

        List<PermissionEntity> permissions = permissionService.findPermissionsByKeys(
            tenantId,
            request.permissionKeys().stream().toList()
        );
        if (permissions.size() != request.permissionKeys().size()) {
            throw new IllegalArgumentException("One or more permissions were not found");
        }

        RoleEntity role = new RoleEntity();
        role.setTenantId(tenantId);
        role.setRoleName(request.roleName());
        role.setRoleKey(request.roleKey());
        role.setDescription(request.description());
        role.setLevel(request.level() != null ? request.level() : 10);
        role.setSystemRole(Boolean.TRUE.equals(request.systemRole()));
        role.setActive(true);

        RoleEntity saved = roleRepository.save(role);
        for (PermissionEntity permission : permissions) {
            RolePermissionEntity mapping = new RolePermissionEntity();
            mapping.setRoleId(saved.getRoleId());
            mapping.setPermissionId(permission.getPermissionId());
            rolePermissionRepository.save(mapping);
        }

        return toResponse(saved, request.permissionKeys());
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        String tenantId = tenantScopeService.requireTenantId();
        var permissionsById = permissionService.listPermissions().stream()
            .collect(Collectors.toMap(PermissionResponse::permissionId, PermissionResponse::permissionKey));

        return roleRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
            .map(role -> {
                Set<String> permissionKeys = rolePermissionRepository.findByRoleId(role.getRoleId()).stream()
                    .map(RolePermissionEntity::getPermissionId)
                    .map(permissionsById::get)
                    .filter(key -> key != null)
                    .collect(Collectors.toSet());
                return toResponse(role, permissionKeys);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public RoleEntity getRoleOrThrow(String tenantId, String roleKey) {
        return roleRepository.findByTenantIdAndRoleKeyAndDeletedAtIsNull(tenantId, roleKey)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleKey));
    }

    private RoleResponse toResponse(RoleEntity role, Set<String> permissionKeys) {
        return new RoleResponse(
            role.getRoleId(),
            role.getRoleName(),
            role.getRoleKey(),
            role.getDescription(),
            role.getLevel(),
            role.getSystemRole(),
            role.getActive(),
            permissionKeys,
            role.getCreatedAt(),
            role.getUpdatedAt()
        );
    }

    private RoleResponse toResponse(RoleEntity role, java.util.Collection<String> permissionKeys) {
        return toResponse(role, Set.copyOf(permissionKeys));
    }
}

