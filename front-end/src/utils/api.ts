/**
 * API utility for making authenticated requests to backend services
 */

import { detectTenant } from '@utils/tenantUtils'

const TENANT_SERVICE_URL = (import.meta as any).env?.VITE_TENANT_SERVICE_URL || 'http://localhost:8083'

export interface ApiError {
	message: string
	status?: number
	statusText?: string
}

export async function apiRequest<T>(
	endpoint: string,
	options: RequestInit = {},
	token?: string
): Promise<T> {
	const url = endpoint.startsWith('http') ? endpoint : `${TENANT_SERVICE_URL}${endpoint}`
	
	const headers: Record<string, string> = {
		'Content-Type': 'application/json',
		...(options.headers as Record<string, string> || {}),
	}

	// Always send tenant to backend when we can detect it
	const tenant = typeof window !== 'undefined' ? detectTenant() : null
	if (tenant) {
		headers['X-Tenant-ID'] = tenant
	}

	if (token) {
		headers['Authorization'] = `Bearer ${token}`
	}

	try {
		const response = await fetch(url, {
			...options,
			headers,
		})

		if (!response.ok) {
			const error: ApiError = {
				message: `API request failed: ${response.statusText}`,
				status: response.status,
				statusText: response.statusText,
			}

			try {
				const errorData = await response.json()
				// Try to extract a meaningful error message
				if (errorData.message) {
					error.message = errorData.message
				} else if (errorData.errors && typeof errorData.errors === 'object') {
					// Handle validation errors
					const errorMessages = Object.values(errorData.errors).join(', ')
					error.message = errorMessages || error.message
				} else if (typeof errorData === 'string') {
					error.message = errorData
				}
			} catch {
				// If response is not JSON, try to get text
				try {
					const text = await response.text()
					if (text) {
						error.message = text
					}
				} catch {
					// Use default error message
				}
			}

			throw error
		}

		// Handle empty responses
		const contentType = response.headers.get('content-type')
		if (contentType && contentType.includes('application/json')) {
			return await response.json()
		}

		return (await response.text()) as unknown as T
	} catch (error) {
		if (error && typeof error === 'object' && 'status' in error) {
			throw error
		}
		throw {
			message: error instanceof Error ? error.message : 'Unknown error occurred',
		} as ApiError
	}
}

export const api = {
	get: <T>(endpoint: string, token?: string) =>
		apiRequest<T>(endpoint, { method: 'GET' }, token),

	post: <T>(endpoint: string, data?: unknown, token?: string) =>
		apiRequest<T>(
			endpoint,
			{
				method: 'POST',
				body: data ? JSON.stringify(data) : undefined,
			},
			token
		),

	put: <T>(endpoint: string, data?: unknown, token?: string) =>
		apiRequest<T>(
			endpoint,
			{
				method: 'PUT',
				body: data ? JSON.stringify(data) : undefined,
			},
			token
		),

	delete: <T>(endpoint: string, token?: string) =>
		apiRequest<T>(endpoint, { method: 'DELETE' }, token),
}

