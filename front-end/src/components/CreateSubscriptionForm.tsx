import React, { useEffect, useState } from 'react'
import { subscriptionService, type SubscriptionRequest } from '@services/subscriptionService'

interface CreateSubscriptionFormProps {
	tenantId: string
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function CreateSubscriptionForm({ tenantId, token, onSuccess, onCancel }: CreateSubscriptionFormProps) {
	const [formData, setFormData] = useState<SubscriptionRequest>({
		tenantId: tenantId,
		planName: '',
		billingCycle: 'monthly',
		amount: 0,
		currency: 'USD',
		status: 'active',
		currentPeriodStart: '',
		currentPeriodEnd: '',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	useEffect(() => {
		setFormData((prev) => ({
			...prev,
			tenantId,
		}))
	}, [tenantId])

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			if (!tenantId) {
				throw new Error('Tenant ID is not resolved yet. Please retry once the overview finishes loading.')
			}
			await subscriptionService.create(token, {
				...formData,
				currentPeriodStart: formData.currentPeriodStart || undefined,
				currentPeriodEnd: formData.currentPeriodEnd || undefined,
			})
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to create subscription')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Create New Subscription</h3>
			{!tenantId && (
				<div style={{ padding: '12px', background: '#fff3cd', color: '#856404', borderRadius: 6, marginBottom: 16 }}>
					Tenant context is still loading. Please wait a moment and try again once the page finishes resolving your tenant.
				</div>
			)}
			<form onSubmit={handleSubmit}>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Plan Name *</label>
					<input
						type="text"
						required
						value={formData.planName}
						onChange={(e) => setFormData({ ...formData, planName: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Billing Cycle</label>
					<select
						value={formData.billingCycle}
						onChange={(e) => setFormData({ ...formData, billingCycle: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					>
						<option value="monthly">Monthly</option>
						<option value="annual">Annual</option>
					</select>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Amount *</label>
					<input
						type="number"
						step="0.01"
						required
						value={formData.amount}
						onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Currency</label>
					<input
						type="text"
						value={formData.currency}
						onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Status</label>
					<input
						type="text"
						value={formData.status}
						onChange={(e) => setFormData({ ...formData, status: e.target.value })}
						placeholder="e.g., active, cancelled"
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Period Start</label>
					<input
						type="date"
						value={formData.currentPeriodStart}
						onChange={(e) => setFormData({ ...formData, currentPeriodStart: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Period End</label>
					<input
						type="date"
						value={formData.currentPeriodEnd}
						onChange={(e) => setFormData({ ...formData, currentPeriodEnd: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				{error && (
					<div style={{ padding: '12px', background: '#f8d7da', color: '#721c24', borderRadius: 4, marginBottom: 16 }}>
						{error}
					</div>
				)}
				<div style={{ display: 'flex', gap: 12 }}>
					<button
						type="submit"
						disabled={loading}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: loading ? 'not-allowed' : 'pointer',
							fontWeight: 500,
						}}
					>
						{loading ? 'Creating...' : 'Create Subscription'}
					</button>
					<button
						type="button"
						onClick={onCancel}
						style={{
							padding: '10px 20px',
							background: '#6c757d',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
						}}
					>
						Cancel
					</button>
				</div>
			</form>
		</div>
	)
}

