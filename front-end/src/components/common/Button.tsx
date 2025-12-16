import React from 'react'

interface ButtonProps {
	children: React.ReactNode
	onClick?: () => void
	type?: 'button' | 'submit' | 'reset'
	variant?: 'primary' | 'secondary' | 'danger' | 'success'
	disabled?: boolean
	loading?: boolean
	style?: React.CSSProperties
}

export function Button({
	children,
	onClick,
	type = 'button',
	variant = 'primary',
	disabled = false,
	loading = false,
	style = {},
}: ButtonProps) {
	const baseStyle: React.CSSProperties = {
		padding: '10px 20px',
		border: 'none',
		borderRadius: 6,
		cursor: disabled || loading ? 'not-allowed' : 'pointer',
		fontSize: 14,
		fontWeight: 500,
		transition: 'all 0.2s',
		opacity: disabled || loading ? 0.6 : 1,
		...style,
	}

	const variantStyles: Record<string, React.CSSProperties> = {
		primary: { background: '#0d6efd', color: 'white' },
		secondary: { background: '#6c757d', color: 'white' },
		danger: { background: '#dc3545', color: 'white' },
		success: { background: '#28a745', color: 'white' },
	}

	return (
		<button
			type={type}
			onClick={onClick}
			disabled={disabled || loading}
			style={{ ...baseStyle, ...variantStyles[variant] }}
		>
			{loading ? 'Loading...' : children}
		</button>
	)
}

