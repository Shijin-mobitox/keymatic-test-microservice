import React, { useEffect, useState } from 'react'

export interface ToastProps {
	message: string
	type: 'success' | 'error' | 'warning' | 'info'
	duration?: number
	onClose: () => void
}

export function Toast({ message, type, duration = 5000, onClose }: ToastProps) {
	const [visible, setVisible] = useState(true)

	useEffect(() => {
		const timer = setTimeout(() => {
			setVisible(false)
			setTimeout(onClose, 300) // Wait for fade animation
		}, duration)

		return () => clearTimeout(timer)
	}, [duration, onClose])

	const getBackgroundColor = () => {
		switch (type) {
			case 'success': return '#d4edda'
			case 'error': return '#f8d7da'
			case 'warning': return '#fff3cd'
			case 'info': return '#d1ecf1'
		}
	}

	const getTextColor = () => {
		switch (type) {
			case 'success': return '#155724'
			case 'error': return '#721c24'
			case 'warning': return '#856404'
			case 'info': return '#0c5460'
		}
	}

	const getBorderColor = () => {
		switch (type) {
			case 'success': return '#c3e6cb'
			case 'error': return '#f5c6cb'
			case 'warning': return '#ffeaa7'
			case 'info': return '#bee5eb'
		}
	}

	return (
		<div
			style={{
				position: 'fixed',
				top: '20px',
				right: '20px',
				zIndex: 1000,
				padding: '12px 16px',
				backgroundColor: getBackgroundColor(),
				color: getTextColor(),
				border: `1px solid ${getBorderColor()}`,
				borderRadius: '4px',
				boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
				maxWidth: '400px',
				opacity: visible ? 1 : 0,
				transform: visible ? 'translateX(0)' : 'translateX(100%)',
				transition: 'all 0.3s ease-in-out',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'space-between',
				gap: '12px'
			}}
		>
			<span>{message}</span>
			<button
				onClick={() => {
					setVisible(false)
					setTimeout(onClose, 300)
				}}
				style={{
					background: 'none',
					border: 'none',
					color: getTextColor(),
					cursor: 'pointer',
					fontSize: '18px',
					padding: '0',
					lineHeight: 1
				}}
			>
				×
			</button>
		</div>
	)
}

export interface ToastContextType {
	showToast: (message: string, type: ToastProps['type']) => void
}

export const ToastContext = React.createContext<ToastContextType | null>(null)

export function ToastProvider({ children }: { children: React.ReactNode }) {
	const [toasts, setToasts] = useState<Array<{ id: string; message: string; type: ToastProps['type'] }>>([])

	const showToast = (message: string, type: ToastProps['type']) => {
		const id = Date.now().toString()
		setToasts(prev => [...prev, { id, message, type }])
	}

	const removeToast = (id: string) => {
		setToasts(prev => prev.filter(toast => toast.id !== id))
	}

	return (
		<ToastContext.Provider value={{ showToast }}>
			{children}
			{toasts.map(toast => (
				<Toast
					key={toast.id}
					message={toast.message}
					type={toast.type}
					onClose={() => removeToast(toast.id)}
				/>
			))}
		</ToastContext.Provider>
	)
}

export function useToast() {
	const context = React.useContext(ToastContext)
	if (!context) {
		throw new Error('useToast must be used within a ToastProvider')
	}
	return context
}
