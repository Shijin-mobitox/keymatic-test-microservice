import React, { useState, useEffect } from 'react'
import { taskService, type Task, type TaskRequest } from '@services/taskService'
import { projectService, type Project } from '@services/projectService'
import { userService, type User } from '@services/userService'

interface EditTaskFormProps {
	task: Task
	projects: Project[]
	users: User[]
	token: string
	onSuccess: () => void
	onCancel: () => void
}

export function EditTaskForm({ task, projects, users, token, onSuccess, onCancel }: EditTaskFormProps) {
	const [formData, setFormData] = useState<TaskRequest>({
		projectId: task.projectId,
		title: task.title,
		description: task.description || '',
		assignedTo: task.assignedTo || '',
		status: task.status || '',
		dueDate: task.dueDate || '',
	})
	const [loading, setLoading] = useState(false)
	const [error, setError] = useState<string | null>(null)

	useEffect(() => {
		setFormData({
			projectId: task.projectId,
			title: task.title,
			description: task.description || '',
			assignedTo: task.assignedTo || '',
			status: task.status || '',
			dueDate: task.dueDate || '',
		})
	}, [task])

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault()
		setLoading(true)
		setError(null)

		try {
			await taskService.update(task.taskId, {
				...formData,
				assignedTo: formData.assignedTo || undefined,
				dueDate: formData.dueDate || undefined,
			}, token)
			onSuccess()
		} catch (err: any) {
			setError(err.message || 'Failed to update task')
		} finally {
			setLoading(false)
		}
	}

	return (
		<div style={{ padding: '20px', background: '#f8f9fa', borderRadius: 8, marginBottom: 20 }}>
			<h3 style={{ marginTop: 0 }}>Edit Task</h3>
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
							background: '#0d6efd',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: loading ? 'not-allowed' : 'pointer',
							fontWeight: 500,
						}}
					>
						{loading ? 'Updating...' : 'Update Task'}
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

