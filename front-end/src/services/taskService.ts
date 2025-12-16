/**
 * Service for managing tasks
 */

import { api } from '@utils/api'

export interface Task {
	taskId: string
	projectId: string
	title: string
	description?: string
	assignedTo?: string
	status?: string
	dueDate?: string
	createdAt: string
	updatedAt: string
}

export interface TaskRequest {
	projectId: string
	title: string
	description?: string
	assignedTo?: string
	status?: string
	dueDate?: string
}

export const taskService = {
	/**
	 * Create a new task
	 */
	create: async (token: string, data: TaskRequest): Promise<Task> => {
		return api.post<Task>('/api/tasks', data, token)
	},

	/**
	 * Get all tasks
	 */
	getAll: async (token: string): Promise<Task[]> => {
		return api.get<Task[]>('/api/tasks', token)
	},

	/**
	 * Get task by ID
	 */
	getById: async (taskId: string, token: string): Promise<Task> => {
		return api.get<Task>(`/api/tasks/${taskId}`, token)
	},

	/**
	 * Get tasks by project
	 */
	getByProject: async (projectId: string, token: string): Promise<Task[]> => {
		return api.get<Task[]>(`/api/tasks/project/${projectId}`, token)
	},

	/**
	 * Get tasks by assignee
	 */
	getByAssignee: async (assignedTo: string, token: string): Promise<Task[]> => {
		return api.get<Task[]>(`/api/tasks/assignee/${assignedTo}`, token)
	},

	/**
	 * Update task
	 */
	update: async (taskId: string, data: TaskRequest, token: string): Promise<Task> => {
		return api.put<Task>(`/api/tasks/${taskId}`, data, token)
	},

	/**
	 * Delete task
	 */
	delete: async (taskId: string, token: string): Promise<void> => {
		return api.delete(`/api/tasks/${taskId}`, token)
	},
}

