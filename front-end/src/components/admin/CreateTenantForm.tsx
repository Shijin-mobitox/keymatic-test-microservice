import React, { useState } from 'react'
import { tenantService, type TenantInfo } from '@services/tenantService'
import { FormField } from '@components/common/FormField'
import { Input } from '@components/common/Input'
import { Textarea } from '@components/common/Textarea'
import { Button } from '@components/common/Button'

interface CreateTenantFormProps {
	token: string
	onSuccess: (tenant: TenantInfo) => void
	onCancel: () => void
}

export function CreateTenantForm({ token, onSuccess, onCancel }: CreateTenantFormProps) {
	const [formData, setFormData] = useState({
		tenantName: '',
		slug: '',
		subscriptionTier: 'starter',
		maxUsers: 10,
		maxStorageGb: 10,
		adminEmail: '',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			// Auto-generate slug from tenant name if not provided
			const slug = formData.slug || formData.tenantName.toLowerCase()
				.replace(/[^a-z0-9-]/g, '-')
				.replace(/-+/g, '-')
				.replace(/^-|-$/g, '')

			const tenant = await tenantService.createTenant(token, {
				tenantName: formData.tenantName,
				slug: slug,
				subscriptionTier: formData.subscriptionTier,
				maxUsers: formData.maxUsers,
				maxStorageGb: formData.maxStorageGb,
				adminEmail: formData.adminEmail || undefined,
			})
			onSuccess(tenant)
		} catch (err: any) {
			setError(err.message || 'Failed to create tenant')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '24px', background: '#f8f9fa', borderRadius: 8, marginBottom: 24 }}>
			<h3 style={{ marginTop: 0, marginBottom: 20 }}>Create New Tenant</h3>
			<form onSubmit={handleSubmit}>
				<FormField label="Tenant Name" required error={error ?? undefined}>
					<Input
						type="text"
						required
						value={formData.tenantName}
						onChange={(e) => setFormData({ ...formData, tenantName: e.target.value })}
						placeholder="e.g., Acme Corporation"
					/>
				</FormField>

				<FormField label="Slug (URL-friendly identifier)" required>
					<Input
						type="text"
						required
						value={formData.slug}
						onChange={(e) => setFormData({ ...formData, slug: e.target.value.toLowerCase() })}
						placeholder="e.g., acme (auto-generated if empty)"
						pattern="^[a-z0-9-]+$"
					/>
					<small style={{ color: '#6c757d', fontSize: 12, marginTop: 4, display: 'block' }}>
						Lowercase letters, numbers, and hyphens only. Database will be created with tenant name.
					</small>
				</FormField>

				<FormField label="Subscription Tier" required>
					<Input
						type="text"
						required
						value={formData.subscriptionTier}
						onChange={(e) => setFormData({ ...formData, subscriptionTier: e.target.value })}
						placeholder="e.g., starter, premium"
					/>
				</FormField>

				<FormField label="Max Users" required>
					<Input
						type="number"
						required
						min="1"
						value={formData.maxUsers}
						onChange={(e) => setFormData({ ...formData, maxUsers: parseInt(e.target.value) || 1 })}
					/>
				</FormField>

				<FormField label="Max Storage (GB)" required>
					<Input
						type="number"
						required
						min="1"
						value={formData.maxStorageGb}
						onChange={(e) => setFormData({ ...formData, maxStorageGb: parseInt(e.target.value) || 1 })}
					/>
				</FormField>

				<FormField label="Admin Email">
					<Input
						type="email"
						value={formData.adminEmail}
						onChange={(e) => setFormData({ ...formData, adminEmail: e.target.value })}
						placeholder="admin@example.com"
					/>
				</FormField>

				{error && (
					<div style={{ padding: '12px', background: '#f8d7da', color: '#721c24', borderRadius: 4, marginBottom: 16 }}>
						{error}
					</div>
				)}

				<div style={{ display: 'flex', gap: 12, marginTop: 20 }}>
					<Button type="submit" variant="success" disabled={loading} loading={loading}>
						Create Tenant
					</Button>
					<Button type="button" variant="secondary" onClick={onCancel}>
						Cancel
					</Button>
				</div>
			</form>
		</div>
	)
}

