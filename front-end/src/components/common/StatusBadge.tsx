import React from 'react'

export interface StatusBadgeProps {
	status: string
	size?: 'small' | 'medium' | 'large'
}

export function StatusBadge({ status, size = 'medium' }: StatusBadgeProps) {
	const getStatusColor = (status: string) => {
		const normalizedStatus = status.toLowerCase()
		switch (normalizedStatus) {
			case 'active':
			case 'success':
			case 'completed':
				return { bg: '#d4edda', color: '#155724', border: '#c3e6cb' }
			case 'inactive':
			case 'disabled':
			case 'suspended':
				return { bg: '#f8d7da', color: '#721c24', border: '#f5c6cb' }
			case 'pending':
			case 'processing':
			case 'in-progress':
				return { bg: '#fff3cd', color: '#856404', border: '#ffeaa7' }
			case 'maintenance':
			case 'info':
				return { bg: '#d1ecf1', color: '#0c5460', border: '#bee5eb' }
			default:
				return { bg: '#e2e3e5', color: '#383d41', border: '#d6d8db' }
		}
	}

	const getSizeStyles = (size: string) => {
		switch (size) {
			case 'small':
				return { padding: '2px 6px', fontSize: '10px' }
			case 'large':
				return { padding: '6px 12px', fontSize: '14px' }
			default:
				return { padding: '4px 8px', fontSize: '12px' }
		}
	}

	const colors = getStatusColor(status)
	const sizeStyles = getSizeStyles(size)

	return (
		<span
			style={{
				...sizeStyles,
				backgroundColor: colors.bg,
				color: colors.color,
				border: `1px solid ${colors.border}`,
				borderRadius: '12px',
				fontWeight: 500,
				textTransform: 'capitalize',
				display: 'inline-block',
				lineHeight: 1
			}}
		>
			{status}
		</span>
	)
}
