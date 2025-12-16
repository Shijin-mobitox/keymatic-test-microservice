// API Response Types
export interface Tenant {
  tenantId: string;
  tenantName: string;
  slug: string;
  status: string;
  subscriptionTier?: string;
  databaseName: string;
  maxUsers?: number;
  maxStorageGb?: number;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  userId: string;
  tenantId: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  isActive: boolean;
  emailVerified: boolean;
  lastLogin?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Site {
  siteId: string;
  tenantId: string;
  siteName: string;
  siteCode: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  phone?: string;
  email?: string;
  isHeadquarters: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Role {
  roleId: string;
  roleName: string;
  roleKey: string;
  description?: string;
  level: number;
  systemRole: boolean;
  active: boolean;
  permissionKeys: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Permission {
  permissionId: string;
  permissionKey: string;
  permissionName: string;
  description?: string;
  category?: string;
  resource?: string;
  action?: string;
  createdAt: string;
}

export interface UserPermissions {
  userId: string;
  permissions: Permission[];
  roles: Role[];
  siteAccess: SiteAccess[];
}

export interface SiteAccess {
  accessId: string;
  userId: string;
  siteId: string;
  accessLevel: string;
  isActive: boolean;
  grantedAt: string;
}

export interface Project {
  projectId: string;
  tenantId: string;
  siteId?: string;
  name: string;
  description?: string;
  ownerId?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Task {
  taskId: string;
  tenantId: string;
  siteId?: string;
  projectId?: string;
  title: string;
  description?: string;
  assignedTo?: string;
  status?: string;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityLog {
  logId: number;
  tenantId: string;
  siteId?: string;
  userId?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  entityName?: string;
  changes?: any;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

// Request Types
export interface CreateTenantRequest {
  tenantName: string;
  slug: string;
  subscriptionTier: string;
  maxUsers: number;
  maxStorageGb: number;
  adminEmail: string;
  metadata?: any;
}

export interface CreateSiteRequest {
  siteName: string;
  siteCode: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
  phone?: string;
  email?: string;
  isHeadquarters?: boolean;
}

export interface CreateRoleRequest {
  roleName: string;
  roleKey: string;
  description?: string;
  level?: number;
  systemRole?: boolean;
  permissionKeys: string[];
}

export interface CreatePermissionRequest {
  permissionKey: string;
  permissionName: string;
  description?: string;
  category?: string;
  resource?: string;
  action?: string;
}

export interface AssignRoleRequest {
  userId: string;
  roleKey: string;
  siteId?: string;
  expiresAt?: string;
}

export interface GrantSiteAccessRequest {
  userId: string;
  siteId: string;
  accessLevel: string;
  expiresAt?: string;
}

export interface CreateUserRequest {
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  siteId?: string;
  ownerId?: string;
  status?: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  projectId?: string;
  siteId?: string;
  assignedTo?: string;
  status?: string;
  dueDate?: string;
}

