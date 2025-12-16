import React, { useState, useEffect } from 'react'
import { userService, type User, type UserRequest } from '@services/userService'

interface EditUserFormProps {
	user: User
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function EditUserForm({ user, token, onSuccess, onCancel }: EditUserFormProps) {
	const [formData, setFormData] = useState<UserRequest>({
		email: user.email,
		firstName: user.firstName || '',
		lastName: user.lastName || '',
		role: user.role || '',
		isActive: user.isActive,
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	useEffect(() => {
		setFormData({
			email: user.email,
			firstName: user.firstName || '',
			lastName: user.lastName || '',
			role: user.role || '',
			isActive: user.isActive,
		})
	}, [user])

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			await userService.update(user.userId, formData, token)
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to update user')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Edit User</h3>
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
							background: '#0d6efd',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: loading ? 'not-allowed' : 'pointer',
							fontWeight: 500,
						}}
					>
						{loading ? 'Updating...' : 'Update User'}
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

