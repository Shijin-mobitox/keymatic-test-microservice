import React, { useState } from 'react'
import { taskService, type TaskRequest } from '@services/taskService'
import { projectService, type Project } from '@services/projectService'
import { userService, type User } from '@services/userService'

interface CreateTaskFormProps {
	token: string
	projects: Project[]
	users: User[]
	onSuccess: () => void
	onCancel: () => void
}

export function CreateTaskForm({ token, projects, users, onSuccess, onCancel }: CreateTaskFormProps) {
	const [formData, setFormData] = useState<TaskRequest>({
		projectId: '',
		title: '',
		description: '',
		assignedTo: '',
		status: '',
		dueDate: '',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			await taskService.create(token, {
				projectId: formData.projectId,
				title: formData.title,
				description: formData.description || undefined,
				assignedTo: formData.assignedTo || undefined,
				status: formData.status || undefined,
				dueDate: formData.dueDate || undefined,
			})
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to create task')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Create New Task</h3>
			<form onSubmit={handleSubmit}>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Project *</label>
					<select
						required
						value={formData.projectId}
						onChange={(e) => setFormData({ ...formData, projectId: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					>
						<option value="">Select Project</option>
						{projects.map(project => (
							<option key={project.projectId} value={project.projectId}>
								{project.name}
							</option>
						))}
					</select>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Title *</label>
					<input
						type="text"
						required
						value={formData.title}
						onChange={(e) => setFormData({ ...formData, title: e.target.value })}
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
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Assigned To</label>
					<select
						value={formData.assignedTo}
						onChange={(e) => setFormData({ ...formData, assignedTo: e.target.value })}
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					>
						<option value="">Unassigned</option>
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
						placeholder="e.g., todo, in-progress, done"
						style={{ width: '100%', padding: '8px', borderRadius: 4, border: '1px solid #dee2e6' }}
					/>
				</div>
				<div style={{ marginBottom: 16 }}>
					<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Due Date</label>
					<input
						type="date"
						value={formData.dueDate}
						onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
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
						{loading ? 'Creating...' : 'Create Task'}
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

