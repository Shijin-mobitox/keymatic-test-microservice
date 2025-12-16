import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { Task, Project, User } from '../types';
import './Tasks.css';

export function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    projectId: '',
    assignedTo: '',
    status: 'pending',
    dueDate: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [tasksData, projectsData, usersData] = await Promise.all([
        apiService.listTasks(),
        apiService.listProjects(),
        apiService.listUsers(),
      ]);
      setTasks(tasksData);
      setProjects(projectsData);
      setUsers(usersData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createTask({
        ...formData,
        projectId: formData.projectId || undefined,
        assignedTo: formData.assignedTo || undefined,
        dueDate: formData.dueDate || undefined,
      });
      setShowForm(false);
      setFormData({
        title: '',
        description: '',
        projectId: '',
        assignedTo: '',
        status: 'pending',
        dueDate: '',
      });
      loadData();
    } catch (error) {
      console.error('Failed to create task:', error);
      alert('Failed to create task');
    }
  };

  const handleDelete = async (taskId: string) => {
    if (!confirm('Are you sure you want to delete this task?')) return;
    try {
      await apiService.deleteTask(taskId);
      loadData();
    } catch (error) {
      console.error('Failed to delete task:', error);
      alert('Failed to delete task');
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="tasks-page">
      <div className="page-header">
        <h1>Tasks</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ Add Task'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="task-form">
          <h2>Create New Task</h2>
          <div className="form-group">
            <label>Title *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
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
              <label>Project</label>
              <select
                value={formData.projectId}
                onChange={(e) => setFormData({ ...formData, projectId: e.target.value })}
              >
                <option value="">None</option>
                {projects.map((project) => (
                  <option key={project.projectId} value={project.projectId}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Assigned To</label>
              <select
                value={formData.assignedTo}
                onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
              >
                <option value="">Unassigned</option>
                {users.map((user) => (
                  <option key={user.userId} value={user.userId}>
                    {user.email}
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
                <option value="pending">Pending</option>
                <option value="in_progress">In Progress</option>
                <option value="completed">Completed</option>
                <option value="cancelled">Cancelled</option>
              </select>
            </div>
            <div className="form-group">
              <label>Due Date</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              />
            </div>
          </div>
          <button type="submit" className="btn-primary">Create Task</button>
        </form>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Project</th>
              <th>Assigned To</th>
              <th>Status</th>
              <th>Due Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((task) => (
              <tr key={task.taskId}>
                <td><strong>{task.title}</strong></td>
                <td>{task.projectId ? 'Project' : '-'}</td>
                <td>{task.assignedTo ? users.find(u => u.userId === task.assignedTo)?.email : 'Unassigned'}</td>
                <td>
                  <span className={`badge badge-${task.status === 'completed' ? 'success' : task.status === 'in_progress' ? 'info' : 'warning'}`}>
                    {task.status || 'pending'}
                  </span>
                </td>
                <td>{task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '-'}</td>
                <td>
                  <button
                    onClick={() => handleDelete(task.taskId)}
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

