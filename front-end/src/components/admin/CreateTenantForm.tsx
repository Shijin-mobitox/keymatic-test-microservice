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
		adminPassword: 'SecurePass123!',
		adminFirstName: 'Admin',
		adminLastName: 'User',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)
	const [retryAttempts, setRetryAttempts] = useState(0)
	const MAX_RETRY_ATTEMPTS = 2

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		const attemptCreateTenant = async (attempt: number = 0): Promise<void> => {
			try {
				// Auto-generate slug from tenant name if not provided
				const baseSlug = formData.slug || formData.tenantName.toLowerCase()
					.replace(/[^a-z0-9-]/g, '-')
					.replace(/-+/g, '-')
					.replace(/^-|-$/g, '')

				// Add suffix for retry attempts to make slug unique
				const slug = attempt > 0 ? `${baseSlug}-${Date.now()}` : baseSlug

				// Validate required fields
				if (!formData.adminEmail) {
					throw new Error('Admin email is required')
				}
				if (!formData.adminPassword || formData.adminPassword.length < 8) {
					throw new Error('Admin password must be at least 8 characters')
				}

				const tenant = await tenantService.createTenant(token, {
					tenantName: formData.tenantName,
					slug: slug,
					subscriptionTier: formData.subscriptionTier,
					maxUsers: formData.maxUsers,
					maxStorageGb: formData.maxStorageGb,
					adminUser: {
						email: formData.adminEmail || `admin@${slug}.com`,
						password: formData.adminPassword,
						firstName: formData.adminFirstName,
						lastName: formData.adminLastName,
						emailVerified: true,
					},
					metadata: {},
				})
				
				// Success! Reset retry attempts and call success callback
				setRetryAttempts(0)
				onSuccess(tenant)

			} catch (err: any) {
				const isConflictError = err.status === 409 || err.message?.includes('already exists')
				const canRetry = attempt < MAX_RETRY_ATTEMPTS && isConflictError

				if (canRetry) {
					// Set user-friendly retry message
					setError(`Tenant name "${formData.tenantName}" is already in use. Trying with a unique identifier... (Attempt ${attempt + 1}/${MAX_RETRY_ATTEMPTS})`)
					setRetryAttempts(attempt + 1)
					
					// Wait a moment before retrying
					await new Promise(resolve => setTimeout(resolve, 1000))
					return attemptCreateTenant(attempt + 1)
				} else {
					// Final error - provide helpful message
					if (isConflictError) {
						setError(`Tenant "${formData.tenantName}" already exists. Please choose a different name or slug, or try again in a moment.`)
					} else {
						setError(err.message || 'Failed to create tenant')
					}
					setRetryAttempts(0)
				}
			}
		}

		try {
			await attemptCreateTenant()
		} catch (err: any) {
			// This catch is for any errors in the retry logic itself
			setError('An unexpected error occurred. Please try again.')
		} finally {
			setLoading(false)
		}
	}

	return (
		<>
			<style>{`
				@keyframes spin {
					0% { transform: rotate(0deg); }
					100% { transform: rotate(360deg); }
				}
			`}</style>
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

				<FormField label="Admin Email" required>
					<Input
						type="email"
						required
						value={formData.adminEmail}
						onChange={(e) => setFormData({ ...formData, adminEmail: e.target.value })}
						placeholder="admin@example.com"
					/>
					<small style={{ color: '#6c757d', fontSize: 12, marginTop: 4, display: 'block' }}>
						Email for the tenant administrator account
					</small>
				</FormField>

				<FormField label="Admin Password" required>
					<Input
						type="password"
						required
						value={formData.adminPassword}
						onChange={(e) => setFormData({ ...formData, adminPassword: e.target.value })}
						placeholder="Secure password for admin"
						minLength={8}
					/>
					<small style={{ color: '#6c757d', fontSize: 12, marginTop: 4, display: 'block' }}>
						Minimum 8 characters
					</small>
				</FormField>

				<div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
					<FormField label="Admin First Name" required>
						<Input
							type="text"
							required
							value={formData.adminFirstName}
							onChange={(e) => setFormData({ ...formData, adminFirstName: e.target.value })}
							placeholder="Admin"
						/>
					</FormField>

					<FormField label="Admin Last Name" required>
						<Input
							type="text"
							required
							value={formData.adminLastName}
							onChange={(e) => setFormData({ ...formData, adminLastName: e.target.value })}
							placeholder="User"
						/>
					</FormField>
				</div>

				{error && (
					<div style={{ 
						padding: '12px', 
						background: retryAttempts > 0 ? '#fff3cd' : '#f8d7da', 
						color: retryAttempts > 0 ? '#856404' : '#721c24', 
						borderRadius: 4, 
						marginBottom: 16,
						border: retryAttempts > 0 ? '1px solid #ffeaa7' : '1px solid #f5c6cb'
					}}>
						{retryAttempts > 0 && (
							<div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
								<div 
									style={{ 
										width: 16, 
										height: 16, 
										border: '2px solid #856404', 
										borderTop: '2px solid transparent', 
										borderRadius: '50%', 
										animation: 'spin 1s linear infinite' 
									}} 
								/>
								<span>Retrying...</span>
							</div>
						)}
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
		</>
	)
}

