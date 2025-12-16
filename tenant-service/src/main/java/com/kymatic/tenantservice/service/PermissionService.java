package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.rbac.CreatePermissionRequest;
import com.kymatic.tenantservice.dto.rbac.PermissionResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.PermissionEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final TenantScopeService tenantScopeService;

    public PermissionService(PermissionRepository permissionRepository, TenantScopeService tenantScopeService) {
        this.permissionRepository = permissionRepository;
        this.tenantScopeService = tenantScopeService;
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        String tenantId = tenantScopeService.requireTenantId();
        permissionRepository.findByTenantIdAndPermissionKey(tenantId, request.permissionKey())
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Permission already exists: " + request.permissionKey());
            });

        PermissionEntity entity = new PermissionEntity();
        entity.setTenantId(tenantId);
        entity.setPermissionKey(request.permissionKey());
        entity.setPermissionName(request.permissionName());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        entity.setResource(request.resource());
        entity.setAction(request.action());

        return toResponse(permissionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        String tenantId = tenantScopeService.requireTenantId();
        return permissionRepository.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionEntity> findPermissionsByKeys(String tenantId, List<String> keys) {
        return permissionRepository.findByTenantIdAndPermissionKeyIn(tenantId, keys);
    }

    @Transactional(readOnly = true)
    public PermissionEntity getPermissionById(UUID permissionId) {
        return permissionRepository.findById(permissionId).orElse(null);
    }

    private PermissionResponse toResponse(PermissionEntity entity) {
        return new PermissionResponse(
            entity.getPermissionId(),
            entity.getPermissionKey(),
            entity.getPermissionName(),
            entity.getDescription(),
            entity.getCategory(),
            entity.getResource(),
            entity.getAction(),
            entity.getCreatedAt()
        );
    }
}

