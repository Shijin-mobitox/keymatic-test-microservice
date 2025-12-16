import React, { useEffect, useState } from 'react'
import { tenantUserService, type TenantUserRequest } from '@services/tenantUserService'
import { FormField } from '@components/common/FormField'
import { Input } from '@components/common/Input'
import { Button } from '@components/common/Button'

interface CreateTenantUserFormProps {
	tenantId: string
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function CreateTenantUserForm({ tenantId, token, onSuccess, onCancel }: CreateTenantUserFormProps) {
	const [formData, setFormData] = useState<TenantUserRequest>({
		tenantId: tenantId,
		email: '',
		password: '',
		role: '',
		isActive: true,
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
				throw new Error('Tenant ID is not resolved yet. Please retry once the page finishes loading.')
			}
			await tenantUserService.create(token, formData)
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to create tenant user')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Create New Tenant User</h3>
			{!tenantId && (
				<div style={{ padding: '12px', background: '#fff3cd', color: '#856404', borderRadius: 6, marginBottom: 16 }}>
					Tenant context is still loading. Please wait and try again once the page resolves the tenant.
				</div>
			)}
			<form onSubmit={handleSubmit}>
				<FormField label="Email" required error={error ?? undefined}>
					<Input
						type="email"
						required
						value={formData.email}
						onChange={(e) => setFormData({ ...formData, email: e.target.value })}
					/>
				</FormField>

				<FormField label="Password" required>
					<Input
						type="password"
						required
						value={formData.password}
						onChange={(e) => setFormData({ ...formData, password: e.target.value })}
					/>
				</FormField>

				<FormField label="Role" required>
					<Input
						type="text"
						required
						value={formData.role}
						onChange={(e) => setFormData({ ...formData, role: e.target.value })}
						placeholder="e.g., admin, user"
					/>
				</FormField>

				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
						<input
							type="checkbox"
							checked={formData.isActive ?? true}
							onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
						/>
						<span>Active</span>
					</label>
				</div>

				{error && (
					<div style={{ padding: '12px', background: '#f8d7da', color: '#721c24', borderRadius: 4, marginBottom: 16 }}>
						{error}
					</div>
				)}

				<div style={{ display: 'flex', gap: 12 }}>
					<Button type="submit" variant="success" disabled={loading} loading={loading}>
						Create User
					</Button>
					<Button type="button" variant="secondary" onClick={onCancel}>
						Cancel
					</Button>
				</div>
			</form>
		</div>
	)
}

