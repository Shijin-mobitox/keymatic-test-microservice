/**
 * Service for viewing audit logs (master database)
 */

import { api } from '@utils/api'

export interface AuditLog {
	logId: number
	tenantId: string
	action: string
	performedBy?: string
	details?: any
	createdAt: string
}

export const auditLogService = {
	/**
	 * Get audit log by ID
	 */
	getById: async (logId: number, token: string): Promise<AuditLog> => {
		return api.get<AuditLog>(`/api/audit-logs/${logId}`, token)
	},

	/**
	 * Get all audit logs for a tenant
	 */
	getByTenant: async (tenantId: string, token: string, page = 0, size = 20): Promise<AuditLog[]> => {
		return api.get<AuditLog[]>(`/api/audit-logs/tenant/${tenantId}?page=${page}&size=${size}`, token)
	},

	/**
	 * Get audit logs by action
	 */
	getByAction: async (tenantId: string, action: string, token: string): Promise<AuditLog[]> => {
		return api.get<AuditLog[]>(`/api/audit-logs/tenant/${tenantId}/action/${action}`, token)
	},
}

