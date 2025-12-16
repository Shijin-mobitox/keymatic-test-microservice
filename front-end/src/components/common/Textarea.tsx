import React from 'react'

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
	error?: string
}

export function Textarea({ error, style, ...props }: TextareaProps) {
	return (
		<>
			<textarea
				{...props}
				style={{
					width: '100%',
					padding: '8px',
					borderRadius: 4,
					border: `1px solid ${error ? '#dc3545' : '#dee2e6'}`,
					fontSize: 14,
					fontFamily: 'inherit',
					...style,
				}}
			/>
			{error && (
				<div style={{ marginTop: 4, fontSize: 12, color: '#dc3545' }}>{error}</div>
			)}
		</>
	)
}

