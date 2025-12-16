-- ============================================================================
-- Multi-Site Tenant Database with RBAC - Complete Schema
-- ============================================================================
-- This schema provides comprehensive Role-Based Access Control (RBAC) system
-- for multi-tenant SaaS application with support for:
-- - Multiple Sites per tenant
-- - Hierarchical RBAC with permission inheritance
-- - Site-specific and global role assignments
-- - Teams & Departments organizational structure
-- - Comprehensive auditing
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Users table (enhanced with RBAC support)
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active) WHERE deleted_at IS NULL;

-- Sites table (multiple physical locations per tenant)
CREATE TABLE IF NOT EXISTS sites (
    site_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    site_name VARCHAR(255) NOT NULL,
    site_code VARCHAR(50) UNIQUE NOT NULL,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    phone VARCHAR(50),
    email VARCHAR(255),
    is_headquarters BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sites_tenant ON sites(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sites_code ON sites(site_code);
CREATE INDEX IF NOT EXISTS idx_sites_active ON sites(is_active) WHERE deleted_at IS NULL;

-- Roles table (hierarchical RBAC roles)
CREATE TABLE IF NOT EXISTS roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    description TEXT,
    level INTEGER NOT NULL, -- Hierarchical level (10-100)
    is_system_role BOOLEAN DEFAULT false, -- System roles vs custom roles
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(tenant_id, role_key)
);

CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_key ON roles(role_key);
CREATE INDEX IF NOT EXISTS idx_roles_level ON roles(level);

-- Permissions table (granular resource.action permissions)
CREATE TABLE IF NOT EXISTS permissions (
    permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    permission_key VARCHAR(100) NOT NULL, -- e.g., "users.create", "sites.manage"
    permission_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50), -- e.g., "users", "sites", "reports"
    resource VARCHAR(50), -- e.g., "users", "sites"
    action VARCHAR(50), -- e.g., "create", "read", "update", "delete", "manage"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, permission_key)
);

CREATE INDEX IF NOT EXISTS idx_permissions_tenant ON permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permissions_key ON permissions(permission_key);
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions(category);
CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions(resource);

-- Role-Permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_perms_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_perms_perm ON role_permissions(permission_id);

-- User-Roles junction table (supports site-specific roles)
CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    site_id UUID REFERENCES sites(site_id) ON DELETE CASCADE, -- NULL = global role
    assigned_by UUID REFERENCES users(user_id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- For temporary role assignments
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_site ON user_roles(site_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_active ON user_roles(is_active) WHERE expires_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_roles_expires ON user_roles(expires_at) WHERE expires_at IS NOT NULL;

-- Unique index to prevent duplicate user-role-site combinations (handles NULL site_id)
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_roles_unique ON user_roles(user_id, role_id, COALESCE(site_id, '00000000-0000-0000-0000-000000000000'::UUID));

-- User-Site Access table (controls which sites users can access)
CREATE TABLE IF NOT EXISTS user_site_access (
    access_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    site_id UUID NOT NULL REFERENCES sites(site_id) ON DELETE CASCADE,
    access_level VARCHAR(50) NOT NULL, -- 'read', 'write', 'admin'
    granted_by UUID REFERENCES users(user_id),
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, site_id)
);

CREATE INDEX IF NOT EXISTS idx_user_site_access_user ON user_site_access(user_id);
CREATE INDEX IF NOT EXISTS idx_user_site_access_site ON user_site_access(site_id);
CREATE INDEX IF NOT EXISTS idx_user_site_access_active ON user_site_access(is_active) WHERE expires_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_site_access_expires ON user_site_access(expires_at) WHERE expires_at IS NOT NULL;

-- ============================================================================
-- ORGANIZATIONAL STRUCTURE
-- ============================================================================

-- Departments table
CREATE TABLE IF NOT EXISTS departments (
    department_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    site_id UUID NOT NULL REFERENCES sites(site_id) ON DELETE CASCADE,
    department_name VARCHAR(255) NOT NULL,
    department_code VARCHAR(50),
    manager_id UUID REFERENCES users(user_id),
    parent_department_id UUID REFERENCES departments(department_id),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_departments_site ON departments(site_id);
CREATE INDEX IF NOT EXISTS idx_departments_tenant ON departments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_departments_manager ON departments(manager_id);

-- User-Departments junction table
CREATE TABLE IF NOT EXISTS user_departments (
    user_department_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(department_id) ON DELETE CASCADE,
    position VARCHAR(100),
    is_primary BOOLEAN DEFAULT false,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, department_id)
);

CREATE INDEX IF NOT EXISTS idx_user_depts_user ON user_departments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_depts_dept ON user_departments(department_id);

-- Teams table
CREATE TABLE IF NOT EXISTS teams (
    team_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    site_id UUID NOT NULL REFERENCES sites(site_id) ON DELETE CASCADE,
    department_id UUID REFERENCES departments(department_id) ON DELETE SET NULL,
    team_name VARCHAR(255) NOT NULL,
    team_code VARCHAR(50),
    leader_id UUID REFERENCES users(user_id),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teams_site ON teams(site_id);
CREATE INDEX IF NOT EXISTS idx_teams_tenant ON teams(tenant_id);
CREATE INDEX IF NOT EXISTS idx_teams_dept ON teams(department_id);
CREATE INDEX IF NOT EXISTS idx_teams_leader ON teams(leader_id);

-- Team Members junction table
CREATE TABLE IF NOT EXISTS team_members (
    team_member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(team_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    team_role VARCHAR(50), -- 'leader', 'member', 'contributor'
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(team_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_team_members_team ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);

-- ============================================================================
-- LEGACY TABLES (for backward compatibility)
-- ============================================================================

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    project_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    site_id UUID REFERENCES sites(site_id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID REFERENCES users(user_id),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_projects_tenant ON projects(tenant_id);
CREATE INDEX IF NOT EXISTS idx_projects_site ON projects(site_id);
CREATE INDEX IF NOT EXISTS idx_projects_owner ON projects(owner_id);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    site_id UUID REFERENCES sites(site_id) ON DELETE SET NULL,
    project_id UUID REFERENCES projects(project_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to UUID REFERENCES users(user_id),
    status VARCHAR(50),
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tasks_tenant ON tasks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tasks_site ON tasks(site_id);
CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned ON tasks(assigned_to);

-- ============================================================================
-- AUDIT & ACTIVITY LOGGING
-- ============================================================================

-- Enhanced Activity Log table
CREATE TABLE IF NOT EXISTS activity_log (
    log_id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    site_id UUID REFERENCES sites(site_id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- 'created', 'updated', 'deleted', 'login', etc.
    entity_type VARCHAR(50), -- 'user', 'site', 'role', 'permission', etc.
    entity_id UUID,
    entity_name VARCHAR(255), -- Human-readable name for quick reference
    changes JSONB, -- Before/after changes
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_log_user ON activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_created ON activity_log(created_at);
CREATE INDEX IF NOT EXISTS idx_activity_log_entity ON activity_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_site ON activity_log(site_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_tenant ON activity_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_action ON activity_log(action);

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Function to log activity
CREATE OR REPLACE FUNCTION log_activity(
    p_user_id UUID,
    p_action VARCHAR,
    p_entity_type VARCHAR,
    p_entity_id UUID,
    p_entity_name VARCHAR,
    p_site_id UUID,
    p_changes JSONB
) RETURNS BIGINT AS $$
DECLARE
    v_log_id BIGINT;
    v_tenant_id VARCHAR;
BEGIN
    -- Get tenant_id from user
    SELECT tenant_id INTO v_tenant_id FROM users WHERE user_id = p_user_id;
    
    INSERT INTO activity_log (
        tenant_id, site_id, user_id, action, entity_type, 
        entity_id, entity_name, changes
    ) VALUES (
        v_tenant_id, p_site_id, p_user_id, p_action, p_entity_type,
        p_entity_id, p_entity_name, p_changes
    ) RETURNING log_id INTO v_log_id;
    
    RETURN v_log_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get user permissions (with site context)
CREATE OR REPLACE FUNCTION get_user_permissions(
    p_user_id UUID,
    p_site_id UUID DEFAULT NULL
) RETURNS TABLE (
    permission_key VARCHAR,
    permission_name VARCHAR,
    category VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        p.permission_key,
        p.permission_name,
        p.category
    FROM permissions p
    JOIN role_permissions rp ON p.permission_id = rp.permission_id
    JOIN roles r ON rp.role_id = r.role_id
    JOIN user_roles ur ON r.role_id = ur.role_id
    WHERE ur.user_id = p_user_id
        AND ur.is_active = true
        AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
        AND (ur.site_id = p_site_id OR ur.site_id IS NULL) -- Site-specific or global
        AND r.is_active = true
        AND r.deleted_at IS NULL
    ORDER BY p.category, p.permission_key;
END;
$$ LANGUAGE plpgsql;

-- Function to check if user has permission
CREATE OR REPLACE FUNCTION has_permission(
    p_user_id UUID,
    p_permission_key VARCHAR,
    p_site_id UUID DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_has_permission BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM permissions p
        JOIN role_permissions rp ON p.permission_id = rp.permission_id
        JOIN roles r ON rp.role_id = r.role_id
        JOIN user_roles ur ON r.role_id = ur.role_id
        WHERE ur.user_id = p_user_id
            AND p.permission_key = p_permission_key
            AND ur.is_active = true
            AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
            AND (ur.site_id = p_site_id OR ur.site_id IS NULL)
            AND r.is_active = true
            AND r.deleted_at IS NULL
    ) INTO v_has_permission;
    
    RETURN COALESCE(v_has_permission, false);
END;
$$ LANGUAGE plpgsql;

-- Function to check if user can access site
CREATE OR REPLACE FUNCTION can_access_site(
    p_user_id UUID,
    p_site_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_can_access BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM user_site_access usa
        WHERE usa.user_id = p_user_id
            AND usa.site_id = p_site_id
            AND usa.is_active = true
            AND (usa.expires_at IS NULL OR usa.expires_at > CURRENT_TIMESTAMP)
    ) INTO v_can_access;
    
    RETURN COALESCE(v_can_access, false);
END;
$$ LANGUAGE plpgsql;

-- Function to get user roles (with site context)
CREATE OR REPLACE FUNCTION get_user_roles(
    p_user_id UUID,
    p_site_id UUID DEFAULT NULL
) RETURNS TABLE (
    role_id UUID,
    role_name VARCHAR,
    role_key VARCHAR,
    level INTEGER,
    site_id UUID
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.role_id,
        r.role_name,
        r.role_key,
        r.level,
        ur.site_id
    FROM roles r
    JOIN user_roles ur ON r.role_id = ur.role_id
    WHERE ur.user_id = p_user_id
        AND ur.is_active = true
        AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
        AND (ur.site_id = p_site_id OR ur.site_id IS NULL)
        AND r.is_active = true
        AND r.deleted_at IS NULL
    ORDER BY r.level DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to assign role to user
CREATE OR REPLACE FUNCTION assign_user_role(
    p_user_id UUID,
    p_role_key VARCHAR,
    p_site_id UUID DEFAULT NULL,
    p_assigned_by UUID DEFAULT NULL,
    p_expires_at TIMESTAMP DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_role_id UUID;
    v_user_role_id UUID;
    v_tenant_id VARCHAR;
BEGIN
    -- Get role_id and tenant_id
    SELECT r.role_id, r.tenant_id INTO v_role_id, v_tenant_id
    FROM roles r
    WHERE r.role_key = p_role_key
        AND r.is_active = true
        AND r.deleted_at IS NULL;
    
    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Role % not found', p_role_key;
    END IF;
    
    -- Verify user belongs to same tenant
    SELECT tenant_id INTO v_tenant_id FROM users WHERE user_id = p_user_id;
    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'User not found';
    END IF;
    
    -- Insert or update user_role
    INSERT INTO user_roles (user_id, role_id, site_id, assigned_by, expires_at)
    VALUES (p_user_id, v_role_id, p_site_id, p_assigned_by, p_expires_at)
    ON CONFLICT (user_id, role_id, COALESCE(site_id, '00000000-0000-0000-0000-000000000000'::UUID))
    DO UPDATE SET
        is_active = true,
        assigned_by = p_assigned_by,
        expires_at = p_expires_at,
        updated_at = CURRENT_TIMESTAMP
    RETURNING user_role_id INTO v_user_role_id;
    
    RETURN v_user_role_id;
END;
$$ LANGUAGE plpgsql;

-- Function to grant site access
CREATE OR REPLACE FUNCTION grant_site_access(
    p_user_id UUID,
    p_site_id UUID,
    p_access_level VARCHAR,
    p_granted_by UUID DEFAULT NULL,
    p_expires_at TIMESTAMP DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_access_id UUID;
BEGIN
    INSERT INTO user_site_access (user_id, site_id, access_level, granted_by, expires_at)
    VALUES (p_user_id, p_site_id, p_access_level, p_granted_by, p_expires_at)
    ON CONFLICT (user_id, site_id)
    DO UPDATE SET
        access_level = p_access_level,
        granted_by = p_granted_by,
        expires_at = p_expires_at,
        is_active = true,
        updated_at = CURRENT_TIMESTAMP
    RETURNING access_id INTO v_access_id;
    
    RETURN v_access_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- INITIAL DATA: System Roles
-- ============================================================================

-- Note: These will be inserted per tenant during tenant provisioning
-- The application should call a service to initialize default roles and permissions

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sites_updated_at BEFORE UPDATE ON sites
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_roles_updated_at BEFORE UPDATE ON user_roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_site_access_updated_at BEFORE UPDATE ON user_site_access
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_teams_updated_at BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE users IS 'User accounts with RBAC support';
COMMENT ON TABLE sites IS 'Physical locations/sites for multi-site tenants';
COMMENT ON TABLE roles IS 'Hierarchical RBAC roles (level 10-100)';
COMMENT ON TABLE permissions IS 'Granular permissions in resource.action format';
COMMENT ON TABLE role_permissions IS 'Junction table: roles to permissions';
COMMENT ON TABLE user_roles IS 'User role assignments (supports site-specific roles)';
COMMENT ON TABLE user_site_access IS 'Controls which sites users can access';
COMMENT ON TABLE departments IS 'Organizational departments within sites';
COMMENT ON TABLE teams IS 'Teams within departments/sites';
COMMENT ON TABLE activity_log IS 'Comprehensive audit trail for all operations';
