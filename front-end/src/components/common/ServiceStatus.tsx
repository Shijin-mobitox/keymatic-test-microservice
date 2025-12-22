import React, { useState, useEffect } from 'react'
import { StatusBadge } from './StatusBadge'

interface ServiceStatusProps {
	token?: string
}

interface ServiceHealth {
	tenantService: 'online' | 'offline' | 'checking'
	keycloak: 'online' | 'offline' | 'checking'
}

export function ServiceStatus({ token }: ServiceStatusProps) {
	const [health, setHealth] = useState<ServiceHealth>({
		tenantService: 'checking',
		keycloak: 'checking'
	})
	const [lastCheck, setLastCheck] = useState<Date | null>(null)

	const checkServices = async () => {
		setHealth(prev => ({
			tenantService: 'checking',
			keycloak: 'checking'
		}))

		// Check Tenant Service
		try {
			const response = await fetch('http://localhost:8083/actuator/health', {
				method: 'GET',
				headers: token ? { 'Authorization': `Bearer ${token}` } : {}
			})
			setHealth(prev => ({
				...prev,
				tenantService: response.ok ? 'online' : 'offline'
			}))
		} catch {
			setHealth(prev => ({
				...prev,
				tenantService: 'offline'
			}))
		}

		// Check Keycloak (through tenant service or direct)
		try {
			// Try to check through tenant service first
			const response = await fetch('http://localhost:8085/realms/kymatic', {
				method: 'GET'
			})
			setHealth(prev => ({
				...prev,
				keycloak: response.ok ? 'online' : 'offline'
			}))
		} catch {
			setHealth(prev => ({
				...prev,
				keycloak: 'offline'
			}))
		}

		setLastCheck(new Date())
	}

	useEffect(() => {
		checkServices()
		const interval = setInterval(checkServices, 30000) // Check every 30 seconds
		return () => clearInterval(interval)
	}, [token])

	const getStatusText = (status: string) => {
		switch (status) {
			case 'online': return 'Online'
			case 'offline': return 'Offline'
			case 'checking': return 'Checking...'
			default: return 'Unknown'
		}
	}

	const allOnline = health.tenantService === 'online' && health.keycloak === 'online'
	const anyOffline = health.tenantService === 'offline' || health.keycloak === 'offline'

	return (
		<div style={{
			padding: '12px',
			backgroundColor: allOnline ? '#d4edda' : anyOffline ? '#f8d7da' : '#fff3cd',
			border: `1px solid ${allOnline ? '#c3e6cb' : anyOffline ? '#f5c6cb' : '#ffeaa7'}`,
			borderRadius: '4px',
			marginBottom: '16px'
		}}>
			<div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
				<h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>Service Status</h4>
				<button
					onClick={checkServices}
					style={{
						background: 'none',
						border: '1px solid #6c757d',
						borderRadius: '4px',
						padding: '4px 8px',
						fontSize: '12px',
						cursor: 'pointer',
						color: '#6c757d'
					}}
				>
					Refresh
				</button>
			</div>
			
			<div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
				<div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
					<span style={{ fontSize: '12px', color: '#6c757d' }}>Tenant Service:</span>
					<StatusBadge 
						status={getStatusText(health.tenantService)} 
						size="small"
					/>
				</div>
				
				<div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
					<span style={{ fontSize: '12px', color: '#6c757d' }}>Keycloak:</span>
					<StatusBadge 
						status={getStatusText(health.keycloak)} 
						size="small"
					/>
				</div>
				
				{lastCheck && (
					<span style={{ fontSize: '11px', color: '#6c757d', marginLeft: 'auto' }}>
						Last check: {lastCheck.toLocaleTimeString()}
					</span>
				)}
			</div>

			{anyOffline && (
				<div style={{ marginTop: '8px', fontSize: '12px', color: '#721c24' }}>
					⚠️ Some services are offline. Tenant creation may fail.
				</div>
			)}
		</div>
	)
}
