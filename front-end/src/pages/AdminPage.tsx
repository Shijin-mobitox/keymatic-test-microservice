import React, { useState, useEffect } from 'react'
import { useAuth } from '@auth/useAuth'
import { tenantService, type TenantInfo } from '@services/tenantService'
import { tenantUserService, type TenantUser } from '@services/tenantUserService'
import { CreateTenantForm } from '@components/admin/CreateTenantForm'
import { CreateTenantUserForm } from '@components/CreateTenantUserForm'
import { Button } from '@components/common/Button'

export function AdminPage() {
	const { token, isAuthenticated } = useAuth()
	const [tenants, setTenants] = useState<TenantInfo[]>([])
	const [selectedTenant, setSelectedTenant] = useState<string | null>(null)
	const [tenantUsers, setTenantUsers] = useState<TenantUser[]>([])
	const [showCreateTenant, setShowCreateTenant] = useState(false)
	const [showCreateUser, setShowCreateUser] = useState(false)
	const [loading, setLoading] = useState(true)
	const [error, setError] = useState<string | null>(null)

	useEffect(() => {
		if (!token || !isAuthenticated) return
		loadTenants()
	}, [token, isAuthenticated])

	useEffect(() => {
		if (selectedTenant && token) {
			loadTenantUsers(selectedTenant)
		}
	}, [selectedTenant, token])

	const loadTenants = async () => {
		setLoading(true)
		setError(null)
		try {
			const data = await tenantService.listTenants(token!)
			setTenants(data)
		} catch (err: any) {
			setError(err.message || 'Failed to load tenants')
		} finally {
			setLoading(false)
		}
	}

	const loadTenantUsers = async (tenantId: string) => {
		try {
			const users = await tenantUserService.getByTenant(tenantId, token!)
			setTenantUsers(users)
		} catch (err: any) {
			console.error('Failed to load tenant users:', err)
		}
	}

	const handleTenantCreated = (tenant: TenantInfo) => {
		setShowCreateTenant(false)
		loadTenants()
		setSelectedTenant(tenant.tenantId)
	}

	const handleUserCreated = () => {
		setShowCreateUser(false)
		if (selectedTenant) {
			loadTenantUsers(selectedTenant)
		}
	}

	if (!isAuthenticated) {
		return null
	}

	return (
		<div style={{
			fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
			padding: '32px 24px',
			maxWidth: 1400,
			margin: '0 auto',
			backgroundColor: '#f8f9fa',
			minHeight: '100vh'
		}}>
			<header style={{
				display: 'flex',
				justifyContent: 'space-between',
				alignItems: 'center',
				marginBottom: 24,
				padding: '24px',
				backgroundColor: 'white',
				borderRadius: 8,
				boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
			}}>
				<h1 style={{ margin: 0, fontSize: 28, fontWeight: 600 }}>🔧 Admin Panel</h1>
			</header>

			{error && (
				<div style={{
					padding: '16px',
					background: '#f8d7da',
					border: '1px solid #f5c6cb',
					borderRadius: 8,
					marginBottom: 24,
					color: '#721c24',
				}}>
					<strong>⚠️ Error:</strong> {error}
				</div>
			)}

			{/* Create Tenant Section */}
			<div style={{
				backgroundColor: 'white',
				padding: '24px',
				borderRadius: 8,
				boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
				marginBottom: 24
			}}>
				<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
					<h2 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>Tenant Management</h2>
					{!showCreateTenant && (
						<Button variant="success" onClick={() => setShowCreateTenant(true)}>
							+ Create Tenant
						</Button>
					)}
				</div>

				{showCreateTenant && (
					<CreateTenantForm
						token={token!}
						onSuccess={handleTenantCreated}
						onCancel={() => setShowCreateTenant(false)}
					/>
				)}

				{!showCreateTenant && (
					<>
						{loading ? (
							<div style={{ textAlign: 'center', padding: '40px' }}>
								<p style={{ color: '#6c757d' }}>Loading tenants...</p>
							</div>
						) : tenants.length === 0 ? (
							<p style={{ color: '#6c757d' }}>No tenants found. Create your first tenant to get started.</p>
						) : (
							<div style={{ display: 'grid', gap: '12px' }}>
								{tenants.map(tenant => (
									<div
										key={tenant.tenantId}
										onClick={() => setSelectedTenant(tenant.tenantId)}
										style={{
											padding: '16px',
											background: selectedTenant === tenant.tenantId ? '#e7f3ff' : '#f8f9fa',
											borderRadius: 6,
											border: `2px solid ${selectedTenant === tenant.tenantId ? '#0d6efd' : '#dee2e6'}`,
											cursor: 'pointer',
											transition: 'all 0.2s',
										}}
									>
										<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
											<div>
												<strong style={{ fontSize: 16 }}>{tenant.tenantName}</strong>
												<div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>
													Slug: {tenant.slug} • Database: {tenant.databaseName || tenant.tenantName}
												</div>
												<div style={{ fontSize: 12, color: '#6c757d', marginTop: 4 }}>
													Status: {tenant.status} • Tier: {tenant.subscriptionTier || 'N/A'}
												</div>
											</div>
											<span style={{
												padding: '4px 12px',
												borderRadius: 12,
												fontSize: 12,
												fontWeight: 500,
												backgroundColor: tenant.status === 'active' ? '#d4edda' : '#f8d7da',
												color: tenant.status === 'active' ? '#155724' : '#721c24'
											}}>
												{tenant.status}
											</span>
										</div>
									</div>
								))}
							</div>
						)}
					</>
				)}
			</div>

			{/* Tenant Users Section */}
			{selectedTenant && (
				<div style={{
					backgroundColor: 'white',
					padding: '24px',
					borderRadius: 8,
					boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
				}}>
					<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
						<h2 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>Tenant Users</h2>
						{!showCreateUser && (
							<Button variant="success" onClick={() => setShowCreateUser(true)}>
								+ Create User
							</Button>
						)}
					</div>

					{showCreateUser && (
						<CreateTenantUserForm
							tenantId={selectedTenant}
							token={token!}
							onSuccess={handleUserCreated}
							onCancel={() => setShowCreateUser(false)}
						/>
					)}

					{!showCreateUser && (
						tenantUsers.length === 0 ? (
							<p style={{ color: '#6c757d' }}>No users found for this tenant.</p>
						) : (
							<div style={{ display: 'grid', gap: '12px' }}>
								{tenantUsers.map(user => (
									<div key={user.userId} style={{
										padding: '16px',
										background: '#f8f9fa',
										borderRadius: 6,
										border: '1px solid #dee2e6'
									}}>
										<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
											<div>
												<strong style={{ fontSize: 16 }}>{user.email}</strong>
												<div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>Role: {user.role}</div>
											</div>
											<span style={{
												padding: '4px 12px',
												borderRadius: 12,
												fontSize: 12,
												fontWeight: 500,
												backgroundColor: user.isActive ? '#d4edda' : '#f8d7da',
												color: user.isActive ? '#155724' : '#721c24'
											}}>
												{user.isActive ? 'Active' : 'Inactive'}
											</span>
										</div>
									</div>
								))}
							</div>
						)
					)}
				</div>
			)}
		</div>
	)
}

