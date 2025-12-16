import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { Project, Site } from '../types';
import './Projects.css';

export function Projects() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    siteId: '',
    status: 'active',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [projectsData, sitesData] = await Promise.all([
        apiService.listProjects(),
        apiService.listSites(),
      ]);
      setProjects(projectsData);
      setSites(sitesData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createProject({
        ...formData,
        siteId: formData.siteId || undefined,
      });
      setShowForm(false);
      setFormData({ name: '', description: '', siteId: '', status: 'active' });
      loadData();
    } catch (error) {
      console.error('Failed to create project:', error);
      alert('Failed to create project');
    }
  };

  const handleDelete = async (projectId: string) => {
    if (!confirm('Are you sure you want to delete this project?')) return;
    try {
      await apiService.deleteProject(projectId);
      loadData();
    } catch (error) {
      console.error('Failed to delete project:', error);
      alert('Failed to delete project');
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="projects-page">
      <div className="page-header">
        <h1>Projects</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ Add Project'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="project-form">
          <h2>Create New Project</h2>
          <div className="form-group">
            <label>Project Name *</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={3}
            />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Site</label>
              <select
                value={formData.siteId}
                onChange={(e) => setFormData({ ...formData, siteId: e.target.value })}
              >
                <option value="">None</option>
                {sites.map((site) => (
                  <option key={site.siteId} value={site.siteId}>
                    {site.siteName}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Status</label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
                <option value="completed">Completed</option>
              </select>
            </div>
          </div>
          <button type="submit" className="btn-primary">Create Project</button>
        </form>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {projects.map((project) => (
              <tr key={project.projectId}>
                <td><strong>{project.name}</strong></td>
                <td>{project.description || '-'}</td>
                <td>
                  <span className={`badge badge-${project.status === 'active' ? 'success' : 'info'}`}>
                    {project.status || 'active'}
                  </span>
                </td>
                <td>{new Date(project.createdAt).toLocaleDateString()}</td>
                <td>
                  <button
                    onClick={() => handleDelete(project.projectId)}
                    className="btn-danger btn-sm"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

