/**
 * Service for viewing activity logs (tenant-specific)
 */

import { api } from '@utils/api'

export interface ActivityLog {
	logId: number
	userId?: string
	action?: string
	entityType?: string
	entityId?: string
	changes?: any
	createdAt: string
}

export const activityLogService = {
	/**
	 * Get all activity logs
	 */
	getAll: async (token: string, page = 0, size = 20): Promise<ActivityLog[]> => {
		return api.get<ActivityLog[]>(`/api/activity-logs?page=${page}&size=${size}`, token)
	},

	/**
	 * Get activity log by ID
	 */
	getById: async (logId: number, token: string): Promise<ActivityLog> => {
		return api.get<ActivityLog>(`/api/activity-logs/${logId}`, token)
	},

	/**
	 * Get activity logs by user
	 */
	getByUser: async (userId: string, token: string): Promise<ActivityLog[]> => {
		return api.get<ActivityLog[]>(`/api/activity-logs/user/${userId}`, token)
	},

	/**
	 * Get activity logs by action
	 */
	getByAction: async (action: string, token: string): Promise<ActivityLog[]> => {
		return api.get<ActivityLog[]>(`/api/activity-logs/action/${action}`, token)
	},

	/**
	 * Get activity logs by entity
	 */
	getByEntity: async (entityType: string, entityId: string, token: string): Promise<ActivityLog[]> => {
		return api.get<ActivityLog[]>(`/api/activity-logs/entity/${entityType}/${entityId}`, token)
	},
}

