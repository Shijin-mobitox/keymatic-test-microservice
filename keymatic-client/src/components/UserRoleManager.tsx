import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { User, Role, Site, UserPermissions } from '../types';
import './UserRoleManager.css';

interface UserRoleManagerProps {
  userId: string;
  onClose: () => void;
}

export function UserRoleManager({ userId, onClose }: UserRoleManagerProps) {
  const [user, setUser] = useState<User | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [permissions, setPermissions] = useState<UserPermissions | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedRole, setSelectedRole] = useState('');
  const [selectedSite, setSelectedSite] = useState('');
  const [selectedSiteForAccess, setSelectedSiteForAccess] = useState('');
  const [accessLevel, setAccessLevel] = useState('read');

  useEffect(() => {
    loadData();
  }, [userId]);

  const loadData = async () => {
    try {
      const [userData, rolesData, sitesData, permissionsData] = await Promise.all([
        apiService.getUser(userId),
        apiService.listRoles(),
        apiService.listSites(),
        apiService.getUserPermissions(userId),
      ]);

      setUser(userData);
      setRoles(rolesData);
      setSites(sitesData);
      setPermissions(permissionsData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAssignRole = async () => {
    if (!selectedRole) return;

    try {
      await apiService.assignRole({
        userId,
        roleKey: selectedRole,
        siteId: selectedSite || undefined,
      });
      setSelectedRole('');
      setSelectedSite('');
      loadData();
    } catch (error) {
      console.error('Failed to assign role:', error);
      alert('Failed to assign role');
    }
  };

  const handleGrantSiteAccess = async () => {
    if (!selectedSiteForAccess) return;

    try {
      await apiService.grantSiteAccess({
        userId,
        siteId: selectedSiteForAccess,
        accessLevel,
      });
      setSelectedSiteForAccess('');
      setAccessLevel('read');
      loadData();
    } catch (error) {
      console.error('Failed to grant site access:', error);
      alert('Failed to grant site access');
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="user-role-manager-overlay" onClick={onClose}>
      <div className="user-role-manager" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Manage Roles & Permissions: {user?.email}</h2>
          <button onClick={onClose} className="btn-close">
            ×
          </button>
        </div>

        <div className="modal-content">
          <div className="section">
            <h3>Assign Role</h3>
            <div className="form-row">
              <select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                className="form-select"
              >
                <option value="">Select Role</option>
                {roles.map((role) => (
                  <option key={role.roleId} value={role.roleKey}>
                    {role.roleName} (Level {role.level})
                  </option>
                ))}
              </select>
              <select
                value={selectedSite}
                onChange={(e) => setSelectedSite(e.target.value)}
                className="form-select"
              >
                <option value="">Global (All Sites)</option>
                {sites.map((site) => (
                  <option key={site.siteId} value={site.siteId}>
                    {site.siteName}
                  </option>
                ))}
              </select>
              <button onClick={handleAssignRole} className="btn-primary" disabled={!selectedRole}>
                Assign Role
              </button>
            </div>
          </div>

          <div className="section">
            <h3>Grant Site Access</h3>
            <div className="form-row">
              <select
                value={selectedSiteForAccess}
                onChange={(e) => setSelectedSiteForAccess(e.target.value)}
                className="form-select"
              >
                <option value="">Select Site</option>
                {sites.map((site) => (
                  <option key={site.siteId} value={site.siteId}>
                    {site.siteName}
                  </option>
                ))}
              </select>
              <select
                value={accessLevel}
                onChange={(e) => setAccessLevel(e.target.value)}
                className="form-select"
              >
                <option value="read">Read</option>
                <option value="write">Write</option>
                <option value="admin">Admin</option>
              </select>
              <button
                onClick={handleGrantSiteAccess}
                className="btn-primary"
                disabled={!selectedSiteForAccess}
              >
                Grant Access
              </button>
            </div>
          </div>

          {permissions && (
            <>
              <div className="section">
                <h3>Current Roles</h3>
                <div className="roles-list">
                  {permissions.roles.map((role) => (
                    <div key={role.roleId} className="role-item">
                      <strong>{role.roleName}</strong>
                      <span className="role-level">Level {role.level}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="section">
                <h3>Current Permissions</h3>
                <div className="permissions-list">
                  {permissions.permissions.map((perm) => (
                    <div key={perm.permissionId} className="permission-item">
                      <code>{perm.permissionKey}</code>
                      <span>{perm.permissionName}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="section">
                <h3>Site Access</h3>
                <div className="sites-list">
                  {permissions.siteAccess.map((access) => {
                    const site = sites.find((s) => s.siteId === access.siteId);
                    return (
                      <div key={access.accessId} className="site-access-item">
                        <strong>{site?.siteName || access.siteId}</strong>
                        <span
                          className={`badge badge-${
                            access.accessLevel === 'admin'
                              ? 'danger'
                              : access.accessLevel === 'write'
                              ? 'info'
                              : 'success'
                          }`}
                        >
                          {access.accessLevel}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

