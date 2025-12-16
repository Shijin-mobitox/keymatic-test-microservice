package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.rbac.AssignRoleRequest;
import com.kymatic.tenantservice.dto.rbac.GrantSiteAccessRequest;
import com.kymatic.tenantservice.dto.rbac.UserPermissionsResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.PermissionEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.RoleEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.RolePermissionEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.SiteEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.UserEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.UserRoleEntity;
import com.kymatic.tenantservice.persistence.entity.tenant.UserSiteAccessEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.RolePermissionRepository;
import com.kymatic.tenantservice.persistence.repository.tenant.UserRepository;
import com.kymatic.tenantservice.persistence.repository.tenant.UserRoleRepository;
import com.kymatic.tenantservice.persistence.repository.tenant.UserSiteAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserSiteAccessRepository userSiteAccessRepository;
    private final RoleService roleService;
    private final SiteService siteService;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;
    private final TenantScopeService tenantScopeService;

    public RoleAssignmentService(
        UserRepository userRepository,
        UserRoleRepository userRoleRepository,
        UserSiteAccessRepository userSiteAccessRepository,
        RoleService roleService,
        SiteService siteService,
        RolePermissionRepository rolePermissionRepository,
        PermissionService permissionService,
        TenantScopeService tenantScopeService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userSiteAccessRepository = userSiteAccessRepository;
        this.roleService = roleService;
        this.siteService = siteService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionService = permissionService;
        this.tenantScopeService = tenantScopeService;
    }

    @Transactional
    public void assignRole(AssignRoleRequest request) {
        String tenantId = tenantScopeService.requireTenantId();
        UserEntity user = userRepository.findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));
        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalArgumentException("User does not belong to current tenant");
        }

        RoleEntity role = roleService.getRoleOrThrow(tenantId, request.roleKey());
        UUID siteId = request.siteId();
        SiteEntity site = null;
        if (siteId != null) {
            site = siteService.getTenantSiteOrThrow(tenantId, siteId);
            if (Boolean.FALSE.equals(site.getActive())) {
                throw new IllegalArgumentException("Site is not active");
            }
        }

        UserRoleEntity entity = userRoleRepository.findByTenantIdAndUserIdAndRoleIdAndSiteId(
                tenantId, request.userId(), role.getRoleId(), siteId)
            .orElseGet(UserRoleEntity::new);

        entity.setTenantId(tenantId);
        entity.setUserId(request.userId());
        entity.setRoleId(role.getRoleId());
        entity.setSiteId(siteId);
        entity.setAssignedAt(OffsetDateTime.now());
        entity.setExpiresAt(request.expiresAt());
        entity.setActive(true);

        userRoleRepository.save(entity);
    }

    @Transactional
    public void grantSiteAccess(GrantSiteAccessRequest request) {
        String tenantId = tenantScopeService.requireTenantId();
        UserEntity user = userRepository.findById(request.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));
        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalArgumentException("User does not belong to current tenant");
        }

        SiteEntity site = siteService.getTenantSiteOrThrow(tenantId, request.siteId());
        if (Boolean.FALSE.equals(site.getActive())) {
            throw new IllegalArgumentException("Site is not active");
        }

        UserSiteAccessEntity entity = userSiteAccessRepository.findByTenantIdAndUserIdAndSiteId(
                tenantId, request.userId(), request.siteId())
            .orElseGet(UserSiteAccessEntity::new);

        entity.setTenantId(tenantId);
        entity.setUserId(request.userId());
        entity.setSiteId(request.siteId());
        entity.setAccessLevel(request.accessLevel());
        entity.setGrantedAt(OffsetDateTime.now());
        entity.setExpiresAt(request.expiresAt());
        entity.setActive(true);

        userSiteAccessRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public UserPermissionsResponse getUserPermissions(UUID userId, UUID siteId) {
        String tenantId = tenantScopeService.requireTenantId();
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalArgumentException("User does not belong to current tenant");
        }

        List<UserRoleEntity> roles = userRoleRepository.findByTenantIdAndUserIdAndActiveTrue(tenantId, userId);
        Set<UUID> applicableRoleIds = roles.stream()
            .filter(role -> role.getSiteId() == null || (siteId != null && role.getSiteId().equals(siteId)))
            .map(UserRoleEntity::getRoleId)
            .collect(Collectors.toSet());

        Set<String> permissionKeys = new HashSet<>();
        for (UUID roleId : applicableRoleIds) {
            List<RolePermissionEntity> mappings = rolePermissionRepository.findByRoleId(roleId);
            for (RolePermissionEntity mapping : mappings) {
                PermissionEntity permission = permissionService.getPermissionById(mapping.getPermissionId());
                if (permission != null && tenantId.equals(permission.getTenantId())) {
                    permissionKeys.add(permission.getPermissionKey());
                }
            }
        }

        return new UserPermissionsResponse(userId, siteId, permissionKeys.stream().sorted().toList());
    }
}

