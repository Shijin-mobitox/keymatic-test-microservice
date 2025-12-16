import React from 'react'

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
	error?: string
	options: Array<{ value: string; label: string }>
}

export function Select({ error, options, style, ...props }: SelectProps) {
	return (
		<>
			<select
				{...props}
				style={{
					width: '100%',
					padding: '8px',
					borderRadius: 4,
					border: `1px solid ${error ? '#dc3545' : '#dee2e6'}`,
					fontSize: 14,
					...style,
				}}
			>
				{options.map(option => (
					<option key={option.value} value={option.value}>
						{option.label}
					</option>
				))}
			</select>
			{error && (
				<div style={{ marginTop: 4, fontSize: 12, color: '#dc3545' }}>{error}</div>
			)}
		</>
	)
}

