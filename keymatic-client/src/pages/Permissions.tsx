import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { Permission } from '../types';
import './Permissions.css';

export function Permissions() {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    permissionKey: '',
    permissionName: '',
    description: '',
    category: '',
    resource: '',
    action: '',
  });

  useEffect(() => {
    loadPermissions();
  }, []);

  const loadPermissions = async () => {
    try {
      const data = await apiService.listPermissions();
      setPermissions(data);
    } catch (error) {
      console.error('Failed to load permissions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createPermission(formData);
      setShowForm(false);
      setFormData({
        permissionKey: '',
        permissionName: '',
        description: '',
        category: '',
        resource: '',
        action: '',
      });
      loadPermissions();
    } catch (error) {
      console.error('Failed to create permission:', error);
      alert('Failed to create permission');
    }
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
    <div className="permissions-page">
      <div className="page-header">
        <h1>Permissions</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ Add Permission'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="permission-form">
          <h2>Create New Permission</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Permission Key *</label>
              <input
                type="text"
                value={formData.permissionKey}
                onChange={(e) => setFormData({ ...formData, permissionKey: e.target.value })}
                required
                placeholder="e.g., users.create"
              />
            </div>
            <div className="form-group">
              <label>Permission Name *</label>
              <input
                type="text"
                value={formData.permissionName}
                onChange={(e) => setFormData({ ...formData, permissionName: e.target.value })}
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Category</label>
              <input
                type="text"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                placeholder="e.g., users"
              />
            </div>
            <div className="form-group">
              <label>Resource</label>
              <input
                type="text"
                value={formData.resource}
                onChange={(e) => setFormData({ ...formData, resource: e.target.value })}
                placeholder="e.g., users"
              />
            </div>
            <div className="form-group">
              <label>Action</label>
              <input
                type="text"
                value={formData.action}
                onChange={(e) => setFormData({ ...formData, action: e.target.value })}
                placeholder="e.g., create"
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
          <button type="submit" className="btn-primary">Create Permission</button>
        </form>
      )}

      <div className="permissions-list">
        {Object.entries(permissionsByCategory).map(([category, perms]) => (
          <div key={category} className="permission-category-section">
            <h2>{category}</h2>
            <div className="permissions-grid">
              {perms.map((perm) => (
                <div key={perm.permissionId} className="permission-card">
                  <div className="permission-header">
                    <h3>{perm.permissionName}</h3>
                    <code>{perm.permissionKey}</code>
                  </div>
                  {perm.description && (
                    <p className="permission-description">{perm.description}</p>
                  )}
                  <div className="permission-meta">
                    {perm.resource && <span>Resource: {perm.resource}</span>}
                    {perm.action && <span>Action: {perm.action}</span>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

