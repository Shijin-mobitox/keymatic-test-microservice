import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { ActivityLog } from '../types';
import './ActivityLogs.css';

export function ActivityLogs() {
  const [logs, setLogs] = useState<ActivityLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    action: '',
    entityType: '',
  });

  useEffect(() => {
    loadLogs();
  }, [filters]);

  const loadLogs = async () => {
    try {
      const params: any = {};
      if (filters.action) params.action = filters.action;
      if (filters.entityType) params.entityType = filters.entityType;
      
      const data = await apiService.listActivityLogs(params);
      setLogs(data);
    } catch (error) {
      console.error('Failed to load activity logs:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="activity-logs-page">
      <div className="page-header">
        <h1>Activity Logs</h1>
        <div className="filters">
          <select
            value={filters.action}
            onChange={(e) => setFilters({ ...filters, action: e.target.value })}
            className="filter-select"
          >
            <option value="">All Actions</option>
            <option value="created">Created</option>
            <option value="updated">Updated</option>
            <option value="deleted">Deleted</option>
            <option value="login">Login</option>
          </select>
          <select
            value={filters.entityType}
            onChange={(e) => setFilters({ ...filters, entityType: e.target.value })}
            className="filter-select"
          >
            <option value="">All Entities</option>
            <option value="user">User</option>
            <option value="site">Site</option>
            <option value="role">Role</option>
            <option value="permission">Permission</option>
            <option value="project">Project</option>
            <option value="task">Task</option>
          </select>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Action</th>
              <th>Entity Type</th>
              <th>Entity Name</th>
              <th>User</th>
              <th>Changes</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.logId}>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
                <td>
                  <span className={`badge badge-${log.action === 'created' ? 'success' : log.action === 'deleted' ? 'danger' : 'info'}`}>
                    {log.action}
                  </span>
                </td>
                <td>{log.entityType || '-'}</td>
                <td>{log.entityName || '-'}</td>
                <td>{log.userId ? log.userId.substring(0, 8) + '...' : '-'}</td>
                <td>
                  {log.changes ? (
                    <details>
                      <summary>View Changes</summary>
                      <pre>{JSON.stringify(log.changes, null, 2)}</pre>
                    </details>
                  ) : '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

