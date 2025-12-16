/**
 * Service for managing subscriptions
 */

import { api } from '@utils/api'

export interface Subscription {
	subscriptionId: string
	tenantId: string
	planName: string
	billingCycle?: string
	amount: number
	currency?: string
	status?: string
	currentPeriodStart?: string
	currentPeriodEnd?: string
	createdAt: string
}

export interface SubscriptionRequest {
	tenantId: string
	planName: string
	billingCycle?: string
	amount: number
	currency?: string
	status?: string
	currentPeriodStart?: string
	currentPeriodEnd?: string
}

export const subscriptionService = {
	/**
	 * Create a new subscription
	 */
	create: async (token: string, data: SubscriptionRequest): Promise<Subscription> => {
		return api.post<Subscription>('/api/subscriptions', data, token)
	},

	/**
	 * Get subscription by ID
	 */
	getById: async (subscriptionId: string, token: string): Promise<Subscription> => {
		return api.get<Subscription>(`/api/subscriptions/${subscriptionId}`, token)
	},

	/**
	 * Get all subscriptions for a tenant
	 */
	getByTenant: async (tenantId: string, token: string): Promise<Subscription[]> => {
		return api.get<Subscription[]>(`/api/subscriptions/tenant/${tenantId}`, token)
	},

	/**
	 * Update subscription
	 */
	update: async (subscriptionId: string, data: SubscriptionRequest, token: string): Promise<Subscription> => {
		return api.put<Subscription>(`/api/subscriptions/${subscriptionId}`, data, token)
	},

	/**
	 * Delete subscription
	 */
	delete: async (subscriptionId: string, token: string): Promise<void> => {
		return api.delete(`/api/subscriptions/${subscriptionId}`, token)
	},
}

