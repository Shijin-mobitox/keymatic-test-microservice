/**
 * Service for managing projects
 */

import { api } from '@utils/api'

export interface Project {
	projectId: string
	name: string
	description?: string
	ownerId?: string
	status?: string
	createdAt: string
	updatedAt: string
}

export interface ProjectRequest {
	name: string
	description?: string
	ownerId?: string
	status?: string
}

export const projectService = {
	/**
	 * Create a new project
	 */
	create: async (token: string, data: ProjectRequest): Promise<Project> => {
		return api.post<Project>('/api/projects', data, token)
	},

	/**
	 * Get all projects
	 */
	getAll: async (token: string): Promise<Project[]> => {
		return api.get<Project[]>('/api/projects', token)
	},

	/**
	 * Get project by ID
	 */
	getById: async (projectId: string, token: string): Promise<Project> => {
		return api.get<Project>(`/api/projects/${projectId}`, token)
	},

	/**
	 * Get projects by owner
	 */
	getByOwner: async (ownerId: string, token: string): Promise<Project[]> => {
		return api.get<Project[]>(`/api/projects/owner/${ownerId}`, token)
	},

	/**
	 * Update project
	 */
	update: async (projectId: string, data: ProjectRequest, token: string): Promise<Project> => {
		return api.put<Project>(`/api/projects/${projectId}`, data, token)
	},

	/**
	 * Delete project
	 */
	delete: async (projectId: string, token: string): Promise<void> => {
		return api.delete(`/api/projects/${projectId}`, token)
	},
}

