import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { Role, Permission } from '../types';
import './Roles.css';

export function Roles() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    roleName: '',
    roleKey: '',
    description: '',
    level: 10,
    systemRole: false,
    permissionKeys: [] as string[],
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [rolesData, permissionsData] = await Promise.all([
        apiService.listRoles(),
        apiService.listPermissions(),
      ]);
      setRoles(rolesData);
      setPermissions(permissionsData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createRole(formData);
      setShowForm(false);
      setFormData({
        roleName: '',
        roleKey: '',
        description: '',
        level: 10,
        systemRole: false,
        permissionKeys: [],
      });
      loadData();
    } catch (error) {
      console.error('Failed to create role:', error);
      alert('Failed to create role');
    }
  };

  const togglePermission = (permissionKey: string) => {
    setFormData((prev) => ({
      ...prev,
      permissionKeys: prev.permissionKeys.includes(permissionKey)
        ? prev.permissionKeys.filter((k) => k !== permissionKey)
        : [...prev.permissionKeys, permissionKey],
    }));
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  const permissionsByCategory = permissions.reduce((acc, perm) => {
    const category = perm.category || 'Other';
    if (!acc[category]) acc[category] = [];
    acc[category].push(perm);
    return acc;
  }, {} as Record<string, Permission[]>);

  return (
    <div className="roles-page">
      <div className="page-header">
        <h1>Roles</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ Add Role'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="role-form">
          <h2>Create New Role</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Role Name *</label>
              <input
                type="text"
                value={formData.roleName}
                onChange={(e) => setFormData({ ...formData, roleName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Role Key *</label>
              <input
                type="text"
                value={formData.roleKey}
                onChange={(e) => setFormData({ ...formData, roleKey: e.target.value.toLowerCase().replace(/\s+/g, '_') })}
                required
                placeholder="e.g., site_admin"
              />
            </div>
            <div className="form-group">
              <label>Level *</label>
              <input
                type="number"
                value={formData.level}
                onChange={(e) => setFormData({ ...formData, level: parseInt(e.target.value) })}
                min="10"
                max="100"
                required
              />
            </div>
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={3}
            />
          </div>
          <div className="form-group">
            <label>
              <input
                type="checkbox"
                checked={formData.systemRole}
                onChange={(e) => setFormData({ ...formData, systemRole: e.target.checked })}
              />
              System Role
            </label>
          </div>
          <div className="form-group">
            <label>Permissions</label>
            <div className="permissions-grid">
              {Object.entries(permissionsByCategory).map(([category, perms]) => (
                <div key={category} className="permission-category">
                  <h4>{category}</h4>
                  {perms.map((perm) => (
                    <label key={perm.permissionId} className="permission-item">
                      <input
                        type="checkbox"
                        checked={formData.permissionKeys.includes(perm.permissionKey)}
                        onChange={() => togglePermission(perm.permissionKey)}
                      />
                      <span>{perm.permissionName}</span>
                      <small>{perm.permissionKey}</small>
                    </label>
                  ))}
                </div>
              ))}
            </div>
          </div>
          <button type="submit" className="btn-primary">Create Role</button>
        </form>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Role Name</th>
              <th>Key</th>
              <th>Level</th>
              <th>Type</th>
              <th>Permissions</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {roles.map((role) => (
              <tr key={role.roleId}>
                <td><strong>{role.roleName}</strong></td>
                <td><code>{role.roleKey}</code></td>
                <td>{role.level}</td>
                <td>{role.systemRole ? 'System' : 'Custom'}</td>
                <td>
                  <span className="badge badge-info">
                    {role.permissionKeys.length} permissions
                  </span>
                </td>
                <td>
                  <span className={`badge ${role.active ? 'badge-success' : 'badge-danger'}`}>
                    {role.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

