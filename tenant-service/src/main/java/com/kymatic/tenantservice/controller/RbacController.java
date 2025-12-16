package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.rbac.AssignRoleRequest;
import com.kymatic.tenantservice.dto.rbac.CreatePermissionRequest;
import com.kymatic.tenantservice.dto.rbac.CreateRoleRequest;
import com.kymatic.tenantservice.dto.rbac.CreateSiteRequest;
import com.kymatic.tenantservice.dto.rbac.GrantSiteAccessRequest;
import com.kymatic.tenantservice.dto.rbac.PermissionResponse;
import com.kymatic.tenantservice.dto.rbac.RoleResponse;
import com.kymatic.tenantservice.dto.rbac.SiteResponse;
import com.kymatic.tenantservice.dto.rbac.UserPermissionsResponse;
import com.kymatic.tenantservice.service.PermissionService;
import com.kymatic.tenantservice.service.RoleAssignmentService;
import com.kymatic.tenantservice.service.RoleService;
import com.kymatic.tenantservice.service.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/rbac", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "RBAC", description = "Role-based access control APIs for multi-site tenants")
public class RbacController {

    private final SiteService siteService;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final RoleAssignmentService roleAssignmentService;

    public RbacController(
        SiteService siteService,
        PermissionService permissionService,
        RoleService roleService,
        RoleAssignmentService roleAssignmentService
    ) {
        this.siteService = siteService;
        this.permissionService = permissionService;
        this.roleService = roleService;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Operation(summary = "Create site")
    @PostMapping(path = "/sites", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody CreateSiteRequest request) {
        SiteResponse response = siteService.createSite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List sites")
    @GetMapping("/sites")
    public ResponseEntity<List<SiteResponse>> listSites() {
        return ResponseEntity.ok(siteService.listSites());
    }

    @Operation(summary = "Create permission")
    @PostMapping(path = "/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionResponse> createPermission(
        @Valid @RequestBody CreatePermissionRequest request
    ) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List permissions")
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }

    @Operation(summary = "Create role")
    @PostMapping(path = "/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List roles")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles());
    }

    @Operation(summary = "Assign role to user")
    @PostMapping(path = "/roles/assignments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        roleAssignmentService.assignRole(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Grant site access to user")
    @PostMapping(path = "/site-access", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> grantSiteAccess(@Valid @RequestBody GrantSiteAccessRequest request) {
        roleAssignmentService.grantSiteAccess(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get user permissions")
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<UserPermissionsResponse> getUserPermissions(
        @PathVariable UUID userId,
        @RequestParam(required = false) UUID siteId
    ) {
        return ResponseEntity.ok(roleAssignmentService.getUserPermissions(userId, siteId));
    }
}

