/**
 * Service for managing tenant users (master database)
 */

import { api } from '@utils/api'

export interface TenantUser {
	userId: string
	tenantId: string
	email: string
	role: string
	isActive: boolean
	lastLogin?: string
	createdAt: string
}

export interface TenantUserRequest {
	tenantId: string
	email: string
	password: string
	role: string
	isActive?: boolean
}

export const tenantUserService = {
	/**
	 * Create a new tenant user
	 */
	create: async (token: string, data: TenantUserRequest): Promise<TenantUser> => {
		return api.post<TenantUser>('/api/tenant-users', data, token)
	},

	/**
	 * Get tenant user by ID
	 */
	getById: async (userId: string, token: string): Promise<TenantUser> => {
		return api.get<TenantUser>(`/api/tenant-users/${userId}`, token)
	},

	/**
	 * Get tenant user by email
	 */
	getByEmail: async (tenantId: string, email: string, token: string): Promise<TenantUser> => {
		return api.get<TenantUser>(`/api/tenant-users/tenant/${tenantId}/email?email=${encodeURIComponent(email)}`, token)
	},

	/**
	 * Get all users for a tenant
	 */
	getByTenant: async (tenantId: string, token: string): Promise<TenantUser[]> => {
		return api.get<TenantUser[]>(`/api/tenant-users/tenant/${tenantId}`, token)
	},

	/**
	 * Update tenant user
	 */
	update: async (userId: string, data: TenantUserRequest, token: string): Promise<TenantUser> => {
		return api.put<TenantUser>(`/api/tenant-users/${userId}`, data, token)
	},

	/**
	 * Delete tenant user
	 */
	delete: async (userId: string, token: string): Promise<void> => {
		return api.delete(`/api/tenant-users/${userId}`, token)
	},
}

