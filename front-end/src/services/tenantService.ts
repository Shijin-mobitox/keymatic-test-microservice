/**
 * Service for interacting with tenant-service API
 */

import { api } from '@utils/api'

export interface TenantInfo {
	tenantId: string
	tenantName: string
	slug: string
	status: string
	databaseName?: string
	subscriptionTier?: string
	createdAt: string
	updatedAt: string
}

export interface UserInfo {
	subject: string
	username: string
	email: string
	tenantId: string
	tenantIdFromJwt: string
	message: string
}

export interface TenantCurrentInfo {
	tenantId: string
	status: string
	active: boolean
}

export const tenantService = {
	/**
	 * Get current user information
	 */
	getCurrentUser: async (token: string): Promise<UserInfo> => {
		return api.get<UserInfo>('/api/me', token)
	},

	/**
	 * Get current tenant ID
	 */
	getCurrentTenant: async (token: string): Promise<string> => {
		return api.get<string>('/api/tenant/current', token)
	},

	/**
	 * Get tenant information
	 */
	getTenantInfo: async (token: string): Promise<TenantCurrentInfo> => {
		return api.get<TenantCurrentInfo>('/api/tenant/info', token)
	},

	/**
	 * List all tenants (requires admin role)
	 */
	listTenants: async (token: string, page = 0, size = 10): Promise<TenantInfo[]> => {
		return api.get<TenantInfo[]>(`/api/tenants?page=${page}&size=${size}`, token)
	},

	/**
	 * Get tenant by ID
	 */
	getTenantById: async (tenantId: string, token: string): Promise<TenantInfo> => {
		return api.get<TenantInfo>(`/api/tenants/${tenantId}`, token)
	},

	/**
	 * Get tenant by slug
	 */
	getTenantBySlug: async (slug: string, token: string): Promise<TenantInfo> => {
		return api.get<TenantInfo>(`/api/tenants/slug/${slug}`, token)
	},

	/**
	 * Create a new tenant
	 */
	createTenant: async (token: string, data: {
		tenantName: string
		slug: string
		subscriptionTier: string
		maxUsers: number
		maxStorageGb: number
		adminEmail?: string
	}): Promise<TenantInfo> => {
		return api.post<TenantInfo>('/api/tenants', data, token)
	},
}

