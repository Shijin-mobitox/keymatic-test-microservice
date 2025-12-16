# Tenant Database Schema - Multi-Site RBAC

## Overview

When a new tenant is provisioned, a dedicated PostgreSQL database is created and the following comprehensive schema is automatically applied via Flyway migration.

## Tables Created

### Core RBAC Tables

1. **users** - User accounts with RBAC support
   - `user_id`, `tenant_id`, `email`, `first_name`, `last_name`, `phone`
   - `is_active`, `email_verified`, `last_login`
   - Timestamps: `created_at`, `updated_at`, `deleted_at`

2. **sites** - Physical locations/sites for multi-site tenants
   - `site_id`, `tenant_id`, `site_name`, `site_code`
   - Address fields: `address`, `city`, `state`, `country`, `postal_code`
   - `is_headquarters`, `is_active`
   - Timestamps: `created_at`, `updated_at`, `deleted_at`

3. **roles** - Hierarchical RBAC roles (level 10-100)
   - `role_id`, `tenant_id`, `role_name`, `role_key`
   - `level` (10=Viewer, 20=User, 40=Manager, 60=Site Admin, 80=Administrator, 100=Super Admin)
   - `is_system_role`, `is_active`
   - Timestamps: `created_at`, `updated_at`, `deleted_at`

4. **permissions** - Granular permissions in `resource.action` format
   - `permission_id`, `tenant_id`, `permission_key` (e.g., "users.create")
   - `permission_name`, `description`, `category`, `resource`, `action`

5. **role_permissions** - Junction table: roles to permissions
   - `role_permission_id`, `role_id`, `permission_id`

6. **user_roles** - User role assignments (supports site-specific roles)
   - `user_role_id`, `user_id`, `role_id`
   - `site_id` (NULL = global role, UUID = site-specific role)
   - `assigned_by`, `assigned_at`, `expires_at` (for temporary roles)
   - `is_active`

7. **user_site_access** - Controls which sites users can access
   - `access_id`, `user_id`, `site_id`
   - `access_level` ('read', 'write', 'admin')
   - `granted_by`, `granted_at`, `expires_at`
   - `is_active`

### Organizational Structure Tables

8. **departments** - Organizational departments within sites
   - `department_id`, `tenant_id`, `site_id`
   - `department_name`, `department_code`
   - `manager_id`, `parent_department_id` (for hierarchy)
   - `is_active`, timestamps

9. **user_departments** - User-department assignments
   - `user_department_id`, `user_id`, `department_id`
   - `position`, `is_primary`
   - `joined_at`, `left_at`

10. **teams** - Teams within departments/sites
    - `team_id`, `tenant_id`, `site_id`, `department_id`
    - `team_name`, `team_code`, `leader_id`
    - `is_active`, timestamps

11. **team_members** - Team membership
    - `team_member_id`, `team_id`, `user_id`
    - `team_role` ('leader', 'member', 'contributor')
    - `joined_at`, `left_at`

### Legacy/Application Tables

12. **projects** - Projects (enhanced with site support)
    - `project_id`, `tenant_id`, `site_id`
    - `name`, `description`, `owner_id`, `status`
    - Timestamps

13. **tasks** - Tasks (enhanced with site support)
    - `task_id`, `tenant_id`, `site_id`, `project_id`
    - `title`, `description`, `assigned_to`, `status`, `due_date`
    - Timestamps

### Audit & Logging

14. **activity_log** - Comprehensive audit trail
    - `log_id` (BIGSERIAL), `tenant_id`, `site_id`, `user_id`
    - `action`, `entity_type`, `entity_id`, `entity_name`
    - `changes` (JSONB for before/after)
    - `ip_address`, `user_agent`, `created_at`

## Helper Functions

The schema includes PostgreSQL functions for RBAC operations:

- `log_activity()` - Log user activities
- `get_user_permissions(user_id, site_id)` - Get all user permissions
- `has_permission(user_id, permission_key, site_id)` - Check specific permission
- `can_access_site(user_id, site_id)` - Check site access
- `get_user_roles(user_id, site_id)` - Get user roles
- `assign_user_role(user_id, role_key, site_id, assigned_by, expires_at)` - Assign role
- `grant_site_access(user_id, site_id, access_level, granted_by, expires_at)` - Grant site access

## Indexes

All critical columns are indexed for performance:
- User lookups: `email`, `tenant_id`, `is_active`
- Site lookups: `tenant_id`, `site_code`, `is_active`
- Role/permission lookups: `role_key`, `permission_key`, `category`
- Junction tables: `user_id`, `role_id`, `site_id`
- Activity log: `user_id`, `created_at`, `entity_type`, `site_id`

## Triggers

- Automatic `updated_at` timestamp updates on all relevant tables

## Initialization

After schema creation, the application should:
1. Create default system roles (Super Admin, Administrator, Site Admin, Manager, User, Viewer)
2. Create default permissions for common resources
3. Assign permissions to system roles
4. Create a default headquarters site
5. Create the first admin user with Super Admin role

## Usage Examples

See the comprehensive guide in the specification document for:
- Multi-site operations manager setup
- Department manager configuration
- Read-only auditor access
- Team lead with limited scope
- Permission checking in Java code
- API usage with annotations

