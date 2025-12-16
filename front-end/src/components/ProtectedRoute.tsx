import React from 'react'
import { useAuth } from '@auth/useAuth'

type Props = {
	children: React.ReactNode
	requiredRole?: string
}

export const ProtectedRoute: React.FC<Props> = ({ children, requiredRole }) => {
	const { isAuthenticated, hasRole } = useAuth()

	if (!isAuthenticated) {
		return <div style={{ padding: 24, fontFamily: 'sans-serif' }}>Authenticating...</div>
	}

	if (requiredRole && !hasRole(requiredRole)) {
		return (
			<div style={{ padding: 24, fontFamily: 'sans-serif' }}>
				<h3>Access Denied</h3>
				<p>You do not have the required role: {requiredRole}</p>
			</div>
		)
	}

	return <>{children}</>
}


