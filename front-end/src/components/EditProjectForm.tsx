import React, { useState, useEffect } from 'react'
import { projectService, type Project, type ProjectRequest } from '@services/projectService'
import { userService, type User } from '@services/userService'

interface EditProjectFormProps {
	project: Project
	users: User[]
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function EditProjectForm({ project, users, token, onSuccess, onCancel }: EditProjectFormProps) {
	const [formData, setFormData] = useState<ProjectRequest>({
		name: project.name,
		description: project.description || '',
		ownerId: project.ownerId || '',
		status: project.status || '',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	useEffect(() => {
		setFormData({
			name: project.name,
			description: project.description || '',
			ownerId: project.ownerId || '',
			status: project.status || '',
		})
	}, [project])

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			await projectService.update(project.projectId, {
				...formData,
				ownerId: formData.ownerId || undefined,
			}, token)
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to update project')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Edit Project</h3>
			<form onSubmit={handleSubmit}>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Name *</label>
					<input
						type="text"
						required
						value={formData.name}
						onChange={(e) => setFormData({ ...formData, name: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Description</label>
					<textarea
						value={formData.description}
						onChange={(e) => setFormData({ ...formData, description: e.target.value })}
						rows={4}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Owner</label>
					<select
						value={formData.ownerId}
						onChange={(e) => setFormData({ ...formData, ownerId: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					>
						<option value="">Select Owner</option>
						{users.map(user => (
							<option key={user.userId} value={user.userId}>
								{user.firstName} {user.lastName} ({user.email})
							</option>
						))}
					</select>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Status</label>
					<input
						type="text"
						value={formData.status}
						onChange={(e) => setFormData({ ...formData, status: e.target.value })}
						placeholder="e.g., in-progress, completed"
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
							background: '#0d6efd',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: loading ? 'not-allowed' : 'pointer',
							fontWeight: 500,
						}}
					>
						{loading ? 'Updating...' : 'Update Project'}
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

