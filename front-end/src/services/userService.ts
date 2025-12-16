/**
 * Service for managing tenant-specific users
 */

import { api } from '@utils/api'

export interface User {
	userId: string
	email: string
	firstName?: string
	lastName?: string
	role?: string
	isActive: boolean
	createdAt: string
	updatedAt: string
}

export interface UserRequest {
	email: string
	firstName?: string
	lastName?: string
	role?: string
	isActive?: boolean
}

export const userService = {
	/**
	 * Create a new user
	 */
	create: async (token: string, data: UserRequest): Promise<User> => {
		return api.post<User>('/api/users', data, token)
	},

	/**
	 * Get all users
	 */
	getAll: async (token: string): Promise<User[]> => {
		return api.get<User[]>('/api/users', token)
	},

	/**
	 * Get user by ID
	 */
	getById: async (userId: string, token: string): Promise<User> => {
		return api.get<User>(`/api/users/${userId}`, token)
	},

	/**
	 * Get user by email
	 */
	getByEmail: async (email: string, token: string): Promise<User> => {
		return api.get<User>(`/api/users/email?email=${encodeURIComponent(email)}`, token)
	},

	/**
	 * Update user
	 */
	update: async (userId: string, data: UserRequest, token: string): Promise<User> => {
		return api.put<User>(`/api/users/${userId}`, data, token)
	},

	/**
	 * Delete user
	 */
	delete: async (userId: string, token: string): Promise<void> => {
		return api.delete(`/api/users/${userId}`, token)
	},
}

