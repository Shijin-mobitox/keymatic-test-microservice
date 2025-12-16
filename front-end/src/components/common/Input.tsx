import React from 'react'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
	error?: string
}

export function Input({ error, style, ...props }: InputProps) {
	return (
		<>
			<input
				{...props}
				style={{
					width: '100%',
					padding: '8px',
					borderRadius: 4,
					border: `1px solid ${error ? '#dc3545' : '#dee2e6'}`,
					fontSize: 14,
					...style,
				}}
			/>
			{error && (
				<div style={{ marginTop: 4, fontSize: 12, color: '#dc3545' }}>{error}</div>
			)}
		</>
	)
}

