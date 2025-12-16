import React from 'react'

interface FormFieldProps {
	label: string
	required?: boolean
	children: React.ReactNode
	error?: string
}

export function FormField({ label, required, children, error }: FormFieldProps) {
	return (
		<div style={{ marginBottom: 16 }}>
			<label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
				{label} {required && <span style={{ color: '#dc3545' }}>*</span>}
			</label>
			{children}
			{error && (
				<div style={{ marginTop: 4, fontSize: 12, color: '#dc3545' }}>{error}</div>
			)}
		</div>
	)
}

