import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import './Dashboard.css';

export function Dashboard() {
  const [stats, setStats] = useState({
    users: 0,
    sites: 0,
    roles: 0,
    projects: 0,
    tasks: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [users, sites, roles, projects, tasks] = await Promise.all([
        apiService.listUsers(),
        apiService.listSites(),
        apiService.listRoles(),
        apiService.listProjects(),
        apiService.listTasks(),
      ]);

      setStats({
        users: users.length,
        sites: sites.length,
        roles: roles.length,
        projects: projects.length,
        tasks: tasks.length,
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <h3>Users</h3>
            <p className="stat-value">{stats.users}</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🏢</div>
          <div className="stat-content">
            <h3>Sites</h3>
            <p className="stat-value">{stats.sites}</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🔐</div>
          <div className="stat-content">
            <h3>Roles</h3>
            <p className="stat-value">{stats.roles}</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">📁</div>
          <div className="stat-content">
            <h3>Projects</h3>
            <p className="stat-value">{stats.projects}</p>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">✅</div>
          <div className="stat-content">
            <h3>Tasks</h3>
            <p className="stat-value">{stats.tasks}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

