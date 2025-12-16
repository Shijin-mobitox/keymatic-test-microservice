import axios from 'axios';
import type { AxiosInstance } from 'axios';
import { API_BASE_URL, TENANT_SLUG } from '../config/api';
import type {
  Tenant,
  User,
  Site,
  Role,
  Permission,
  UserPermissions,
  CreateTenantRequest,
  CreateSiteRequest,
  CreateRoleRequest,
  CreatePermissionRequest,
  AssignRoleRequest,
  GrantSiteAccessRequest,
  CreateUserRequest,
  CreateProjectRequest,
  CreateTaskRequest,
  Project,
  Task,
  ActivityLog,
} from '../types';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add token to requests
    this.api.interceptors.request.use((config) => {
      const token = this.getToken();
      const tenantId = this.getTenantId();

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      if (tenantId) {
        config.headers['X-Tenant-ID'] = tenantId;
      }

      return config;
    });

    // Handle errors
    this.api.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config as typeof error.config & { _retry?: boolean };
        const status = error.response?.status;

        // Try to refresh token on 401
        if (status === 401 && !originalRequest?._retry) {
          originalRequest._retry = true;

          try {
            // Import ensureValidToken dynamically to avoid circular dependency
            const { ensureValidToken } = await import('../auth/localAuth');
            const validToken = await ensureValidToken();
            
            if (validToken) {
              originalRequest.headers = {
                ...originalRequest.headers,
                Authorization: `Bearer ${validToken}`,
              };
              return this.api(originalRequest);
            } else {
              // Token refresh failed - redirect to login
              if (typeof window !== 'undefined') {
                window.location.href = '/login';
              }
            }
          } catch (refreshError) {
            console.error('Token refresh failed:', refreshError);
            // Redirect to login on refresh failure
            if (typeof window !== 'undefined') {
              window.location.href = '/login';
            }
          }
        }

        return Promise.reject(error);
      }
    );
  }

  private getToken(): string | null {
    // Get token from localStorage
    return localStorage.getItem('token');
  }

  setToken(token: string) {
    localStorage.setItem('token', token);
  }

  private getTenantId(): string | null {
    return localStorage.getItem('tenantId') || TENANT_SLUG || null;
  }

  // ========== Tenant APIs ==========
  async createTenant(data: CreateTenantRequest): Promise<Tenant> {
    const response = await this.api.post<Tenant>('/api/tenants', data);
    return response.data;
  }

  async listTenants(): Promise<Tenant[]> {
    const response = await this.api.get<Tenant[]>('/api/tenants');
    return response.data;
  }

  async getTenant(tenantId: string): Promise<Tenant> {
    const response = await this.api.get<Tenant>(`/api/tenants/${tenantId}`);
    return response.data;
  }

  async updateTenantStatus(tenantId: string, status: string): Promise<Tenant> {
    const response = await this.api.post(`/api/tenants/${tenantId}/status`, { status });
    return response.data;
  }

  // ========== RBAC APIs ==========
  // Sites
  async createSite(data: CreateSiteRequest): Promise<Site> {
    const response = await this.api.post<Site>('/api/rbac/sites', data);
    return response.data;
  }

  async listSites(): Promise<Site[]> {
    const response = await this.api.get<Site[]>('/api/rbac/sites');
    return response.data;
  }

  // Permissions
  async createPermission(data: CreatePermissionRequest): Promise<Permission> {
    const response = await this.api.post<Permission>('/api/rbac/permissions', data);
    return response.data;
  }

  async listPermissions(): Promise<Permission[]> {
    const response = await this.api.get<Permission[]>('/api/rbac/permissions');
    return response.data;
  }

  // Roles
  async createRole(data: CreateRoleRequest): Promise<Role> {
    const response = await this.api.post<Role>('/api/rbac/roles', data);
    return response.data;
  }

  async listRoles(): Promise<Role[]> {
    const response = await this.api.get<Role[]>('/api/rbac/roles');
    return response.data;
  }

  // Role Assignments
  async assignRole(data: AssignRoleRequest): Promise<void> {
    await this.api.post('/api/rbac/roles/assignments', data);
  }

  async grantSiteAccess(data: GrantSiteAccessRequest): Promise<void> {
    await this.api.post('/api/rbac/site-access', data);
  }

  async getUserPermissions(userId: string, siteId?: string): Promise<UserPermissions> {
    const params = siteId ? { siteId } : {};
    const response = await this.api.get<UserPermissions>(`/api/rbac/users/${userId}/permissions`, { params });
    return response.data;
  }

  // ========== User APIs ==========
  async createUser(data: CreateUserRequest): Promise<User> {
    const response = await this.api.post<User>('/api/users', data);
    return response.data;
  }

  async listUsers(): Promise<User[]> {
    const response = await this.api.get<User[]>('/api/users');
    return response.data;
  }

  async getUser(userId: string): Promise<User> {
    const response = await this.api.get<User>(`/api/users/${userId}`);
    return response.data;
  }

  async getUserByEmail(email: string): Promise<User> {
    const response = await this.api.get<User>('/api/users/email', { params: { email } });
    return response.data;
  }

  async updateUser(userId: string, data: CreateUserRequest): Promise<User> {
    const response = await this.api.put<User>(`/api/users/${userId}`, data);
    return response.data;
  }

  async deleteUser(userId: string): Promise<void> {
    await this.api.delete(`/api/users/${userId}`);
  }

  // ========== Project APIs ==========
  async createProject(data: CreateProjectRequest): Promise<Project> {
    const response = await this.api.post<Project>('/api/projects', data);
    return response.data;
  }

  async listProjects(): Promise<Project[]> {
    const response = await this.api.get<Project[]>('/api/projects');
    return response.data;
  }

  async getProject(projectId: string): Promise<Project> {
    const response = await this.api.get<Project>(`/api/projects/${projectId}`);
    return response.data;
  }

  async updateProject(projectId: string, data: CreateProjectRequest): Promise<Project> {
    const response = await this.api.put<Project>(`/api/projects/${projectId}`, data);
    return response.data;
  }

  async deleteProject(projectId: string): Promise<void> {
    await this.api.delete(`/api/projects/${projectId}`);
  }

  // ========== Task APIs ==========
  async createTask(data: CreateTaskRequest): Promise<Task> {
    const response = await this.api.post<Task>('/api/tasks', data);
    return response.data;
  }

  async listTasks(): Promise<Task[]> {
    const response = await this.api.get<Task[]>('/api/tasks');
    return response.data;
  }

  async getTask(taskId: string): Promise<Task> {
    const response = await this.api.get<Task>(`/api/tasks/${taskId}`);
    return response.data;
  }

  async updateTask(taskId: string, data: CreateTaskRequest): Promise<Task> {
    const response = await this.api.put<Task>(`/api/tasks/${taskId}`, data);
    return response.data;
  }

  async deleteTask(taskId: string): Promise<void> {
    await this.api.delete(`/api/tasks/${taskId}`);
  }

  // ========== Activity Log APIs ==========
  async listActivityLogs(params?: {
    userId?: string;
    action?: string;
    entityType?: string;
    entityId?: string;
  }): Promise<ActivityLog[]> {
    const response = await this.api.get<ActivityLog[]>('/api/activity-logs', { params });
    return response.data;
  }

  async getActivityLog(logId: number): Promise<ActivityLog> {
    const response = await this.api.get<ActivityLog>(`/api/activity-logs/${logId}`);
    return response.data;
  }
}

export const apiService = new ApiService();

