import React, { useState } from 'react'
import { userService, type UserRequest } from '@services/userService'

interface CreateUserFormProps {
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function CreateUserForm({ token, onSuccess, onCancel }: CreateUserFormProps) {
	const [formData, setFormData] = useState<UserRequest>({
		email: '',
		firstName: '',
		lastName: '',
		role: '',
		isActive: true,
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			await userService.create(token, {
				email: formData.email,
				firstName: formData.firstName || undefined,
				lastName: formData.lastName || undefined,
				role: formData.role || undefined,
				isActive: formData.isActive ?? true,
			})
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to create user')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Create New User</h3>
			<form onSubmit={handleSubmit}>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Email *</label>
					<input
						type="email"
						required
						value={formData.email}
						onChange={(e) => setFormData({ ...formData, email: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>First Name</label>
					<input
						type="text"
						value={formData.firstName}
						onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Last Name</label>
					<input
						type="text"
						value={formData.lastName}
						onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Role</label>
					<input
						type="text"
						value={formData.role}
						onChange={(e) => setFormData({ ...formData, role: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
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
						{loading ? 'Creating...' : 'Create User'}
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

