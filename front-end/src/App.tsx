import React, { useCallback, useEffect, useState } from 'react'
import { useAuth } from '@auth/useAuth'
import { ProtectedRoute } from '@components/ProtectedRoute'
import { tenantService, type UserInfo, type TenantCurrentInfo } from '@services/tenantService'
import { subscriptionService, type Subscription } from '@services/subscriptionService'
import { tenantUserService, type TenantUser } from '@services/tenantUserService'
import { auditLogService, type AuditLog } from '@services/auditLogService'
import { userService, type User } from '@services/userService'
import { projectService, type Project } from '@services/projectService'
import { taskService, type Task } from '@services/taskService'
import { activityLogService, type ActivityLog } from '@services/activityLogService'
import { CreateUserForm } from '@components/CreateUserForm'
import { EditUserForm } from '@components/EditUserForm'
import { CreateProjectForm } from '@components/CreateProjectForm'
import { EditProjectForm } from '@components/EditProjectForm'
import { CreateTaskForm } from '@components/CreateTaskForm'
import { EditTaskForm } from '@components/EditTaskForm'
import { CreateSubscriptionForm } from '@components/CreateSubscriptionForm'
import { CreateTenantUserForm } from '@components/CreateTenantUserForm'
import { AdminPage } from '@pages/AdminPage'

type TabType = 'overview' | 'subscriptions' | 'tenant-users' | 'users' | 'projects' | 'tasks' | 'audit-logs' | 'activity-logs' | 'admin'

export default function App() {
	const { user, tenant, token, logout, isAuthenticated } = useAuth()
	const [activeTab, setActiveTab] = useState<TabType>('overview')
	
	// Overview data
	const [userInfo, setUserInfo] = useState<UserInfo | null>(null)
	const [tenantInfo, setTenantInfo] = useState<TenantCurrentInfo | null>(null)
	// Master tenant UUID (from tenant-service /api/tenants/slug/{slug})
	const [masterTenantId, setMasterTenantId] = useState<string | null>(null)
	
	// API data
	const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
	const [tenantUsers, setTenantUsers] = useState<TenantUser[]>([])
	const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
	const [users, setUsers] = useState<User[]>([])
	const [projects, setProjects] = useState<Project[]>([])
	const [tasks, setTasks] = useState<Task[]>([])
	const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([])
	
	const [loading, setLoading] = useState(true)
	const [error, setError] = useState<string | null>(null)

	const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
	const isUuid = useCallback((value?: string | null) => {
		if (!value) return false
		return uuidRegex.test(value.trim())
	}, [])

	const resolveMasterTenantId = useCallback(
		async (extraCandidates: Array<string | null | undefined> = []) => {
			// Only return cached value if it's a valid UUID
			if (masterTenantId && isUuid(masterTenantId)) {
				return masterTenantId
			}
			if (!token) throw new Error('Authentication is required to resolve tenant ID.')

			// Collect all candidates, prioritizing UUIDs
			const allCandidates = [
				...extraCandidates,
				userInfo?.tenantIdFromJwt, // Check JWT tenant ID first
				userInfo?.tenantId,        // Then user tenant ID
				tenantInfo?.tenantId,      // Then tenant info
				tenant,                    // Then detected tenant
			]
				.map((c) => (typeof c === 'string' ? c.trim() : c))
				.filter((c): c is string => !!c)

			// First pass: Check for UUIDs (no API call needed)
			for (const candidate of allCandidates) {
				if (isUuid(candidate)) {
					const validated = candidate.trim()
					setMasterTenantId(validated)
					return validated
				}
			}

			// Second pass: Try to resolve slugs to UUIDs via API
			// Only try slugs that aren't already UUIDs
			const slugCandidates = allCandidates.filter(c => !isUuid(c))
			for (const candidate of slugCandidates) {
				try {
					const tenantRecord = await tenantService.getTenantBySlug(candidate, token)
					if (tenantRecord?.tenantId && isUuid(tenantRecord.tenantId)) {
						const validated = tenantRecord.tenantId.trim()
						setMasterTenantId(validated)
						return validated
					}
				} catch (slugErr: any) {
					// 404 means tenant doesn't exist, which is fine - try next candidate
					if (slugErr?.status === 404) {
						console.warn(`Tenant with slug "${candidate}" not found in database`)
					} else {
						console.warn(`Failed to resolve tenant slug "${candidate}"`, slugErr)
					}
					// continue trying other candidates
				}
			}

			// Last resort: Try to list tenants and see if we can find a match
			try {
				const tenants = await tenantService.listTenants(token, 0, 100)
				if (tenants.length === 1) {
					// If there's only one tenant, use it
					const singleTenant = tenants[0]
					if (singleTenant?.tenantId && isUuid(singleTenant.tenantId)) {
						const validated = singleTenant.tenantId.trim()
						setMasterTenantId(validated)
						return validated
					}
				} else if (tenants.length > 1 && slugCandidates.length > 0) {
					// Try to match slug to tenant
					for (const candidate of slugCandidates) {
						const matched = tenants.find(t => t.slug === candidate)
						if (matched?.tenantId && isUuid(matched.tenantId)) {
							const validated = matched.tenantId.trim()
							setMasterTenantId(validated)
							return validated
						}
					}
				}
			} catch (listErr) {
				console.warn('Failed to list tenants as fallback', listErr)
			}

			// If we still don't have a UUID, provide helpful error message
			const triedSlugs = slugCandidates.join(', ')
			throw new Error(
				`Tenant ID could not be resolved. ` +
				`Tried slugs: ${triedSlugs || 'none'}. ` +
				`Please ensure: (1) Your account has a tenant_id attribute in Keycloak, ` +
				`(2) The tenant exists in the database, or (3) Add ?tenant=<slug> to the URL.`
			)
		},
		[isUuid, masterTenantId, tenant, tenantInfo?.tenantId, token, userInfo?.tenantId, userInfo?.tenantIdFromJwt]
	)

	const realmRoles = user?.realm_access?.roles ?? []
	const clientRoles = user?.resource_access?.['react-client']?.roles ?? []
	const safeToken = token ?? ''

	useEffect(() => {
		if (!token || !isAuthenticated) return

		const fetchOverviewData = async () => {
			setLoading(true)
			setError(null)
			try {
				const [userData, tenantData] = await Promise.all([
					tenantService.getCurrentUser(token),
					tenantService.getTenantInfo(token),
				])
			setUserInfo(userData)
			setTenantInfo(tenantData)
			
			// Only set masterTenantId if it's already a UUID (don't trust tenantData.tenantId as it might be a slug)
			if (tenantData?.tenantId && isUuid(tenantData.tenantId)) {
				setMasterTenantId(tenantData.tenantId)
			}

			// Resolve tenant slug from JWT (e.g. "tenant1") to master tenant UUID
			try {
				await resolveMasterTenantId([
					tenant,
					userData?.tenantIdFromJwt,
					userData?.tenantId,
					tenantData?.tenantId,
				])
			} catch (slugErr: any) {
				console.error('Failed to resolve tenant slug to tenant ID:', slugErr)
				// Fallback: keep masterTenantId as null; API calls that require it will be guarded
			}
			} catch (err: any) {
				setError(err.message || 'Failed to fetch data from tenant service')
				console.error('Error fetching data:', err)
			} finally {
				setLoading(false)
			}
		}

		fetchOverviewData()
	}, [token, isAuthenticated, resolveMasterTenantId])

	// Fetch data when tab changes
	const fetchTabData = async () => {
		if (!token || !isAuthenticated) return

		setLoading(true)
		setError(null)
		try {
			switch (activeTab) {
				case 'subscriptions':
					const subsTenantId = await resolveMasterTenantId()
					if (!isUuid(subsTenantId)) {
						throw new Error(`Invalid tenant ID format: ${subsTenantId}. Expected UUID.`)
					}
					const subs = await subscriptionService.getByTenant(subsTenantId, token)
					setSubscriptions(subs)
					break
				case 'tenant-users':
					const tenantUsersTenantId = await resolveMasterTenantId()
					if (!isUuid(tenantUsersTenantId)) {
						throw new Error(`Invalid tenant ID format: ${tenantUsersTenantId}. Expected UUID.`)
					}
					const tUsers = await tenantUserService.getByTenant(tenantUsersTenantId, token)
					setTenantUsers(tUsers)
					break
				case 'audit-logs':
					const aLogs = await auditLogService.getByTenant(tenant, token)
					setAuditLogs(aLogs)
					break
				case 'users':
					const u = await userService.getAll(token)
					setUsers(u)
					break
				case 'projects':
					const [p, uList] = await Promise.all([
						projectService.getAll(token),
						userService.getAll(token)
					])
					setProjects(p)
					setUsers(uList)
					break
				case 'tasks':
					const [t, pList, uList2] = await Promise.all([
						taskService.getAll(token),
						projectService.getAll(token),
						userService.getAll(token)
					])
					setTasks(t)
					setProjects(pList)
					setUsers(uList2)
					break
				case 'activity-logs':
					const al = await activityLogService.getAll(token)
					setActivityLogs(al)
					break
			}
		} catch (err: any) {
			setError(err.message || `Failed to fetch ${activeTab} data`)
			console.error('Error fetching tab data:', err)
		} finally {
			setLoading(false)
		}
	}

	useEffect(() => {
		if (activeTab !== 'overview') {
			fetchTabData()
		}
	}, [activeTab, token, isAuthenticated, tenant])

	if (!isAuthenticated) {
		return null
	}

	const tabs: { id: TabType; label: string; icon: string }[] = [
		{ id: 'overview', label: 'Overview', icon: '📊' },
		{ id: 'admin', label: 'Admin', icon: '🔧' },
		{ id: 'subscriptions', label: 'Subscriptions', icon: '💳' },
		{ id: 'tenant-users', label: 'Tenant Users', icon: '👥' },
		{ id: 'users', label: 'Users', icon: '👤' },
		{ id: 'projects', label: 'Projects', icon: '📁' },
		{ id: 'tasks', label: 'Tasks', icon: '✅' },
		{ id: 'audit-logs', label: 'Audit Logs', icon: '📋' },
		{ id: 'activity-logs', label: 'Activity Logs', icon: '📝' },
	]

	return (
		<ProtectedRoute>
			<div style={{ 
				fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif', 
				lineHeight: 1.6, 
				padding: '32px 24px', 
				maxWidth: 1400, 
				margin: '0 auto',
				backgroundColor: '#f8f9fa',
				minHeight: '100vh'
			}}>
				<header style={{ 
					display: 'flex', 
					alignItems: 'center', 
					justifyContent: 'space-between', 
					marginBottom: 24, 
					padding: '24px', 
					backgroundColor: 'white',
					borderRadius: 8,
					boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
				}}>
					<div>
						<h1 style={{ margin: 0, fontSize: 28, fontWeight: 600, color: '#212529' }}>Multi-tenant SaaS Portal</h1>
						<p style={{ margin: '8px 0 0', color: '#6c757d', fontSize: 14 }}>
							<strong>Tenant ID:</strong> <span style={{ color: '#0d6efd', fontWeight: 500 }}>{tenant || 'Not available'}</span>
						</p>
					</div>
					<button 
						onClick={logout}
						style={{
							padding: '10px 20px',
							background: '#dc3545',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontSize: 14,
							fontWeight: 500,
							transition: 'background-color 0.2s',
							boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
						}}
						onMouseOver={(e) => e.currentTarget.style.background = '#c82333'}
						onMouseOut={(e) => e.currentTarget.style.background = '#dc3545'}
					>
						Logout
					</button>
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

				{/* Tabs */}
				<div style={{
					display: 'flex',
					gap: '8px',
					marginBottom: 24,
					flexWrap: 'wrap',
					backgroundColor: 'white',
					padding: '12px',
					borderRadius: 8,
					boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
				}}>
					{tabs.map(tab => (
						<button
							key={tab.id}
							onClick={() => setActiveTab(tab.id)}
							style={{
								padding: '10px 16px',
								background: activeTab === tab.id ? '#0d6efd' : 'transparent',
								color: activeTab === tab.id ? 'white' : '#495057',
								border: '1px solid',
								borderColor: activeTab === tab.id ? '#0d6efd' : '#dee2e6',
								borderRadius: 6,
								cursor: 'pointer',
								fontSize: 14,
								fontWeight: activeTab === tab.id ? 600 : 400,
								transition: 'all 0.2s',
							}}
							onMouseOver={(e) => {
								if (activeTab !== tab.id) {
									e.currentTarget.style.background = '#f8f9fa'
								}
							}}
							onMouseOut={(e) => {
								if (activeTab !== tab.id) {
									e.currentTarget.style.background = 'transparent'
								}
							}}
						>
							{tab.icon} {tab.label}
						</button>
					))}
				</div>

				{/* Tab Content */}
				{loading && activeTab !== 'overview' ? (
					<div style={{ 
						textAlign: 'center', 
						padding: '60px 20px',
						backgroundColor: 'white',
						borderRadius: 8,
						boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
					}}>
						<div style={{ fontSize: 18, color: '#6c757d', marginBottom: 8 }}>⏳</div>
						<p style={{ fontSize: 16, color: '#6c757d', margin: 0 }}>Loading...</p>
					</div>
				) : (
					activeTab === 'admin' ? (
						<AdminPage />
					) : (
						<div style={{
							backgroundColor: 'white',
							padding: '24px',
							borderRadius: 8,
							boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
						}}>
							{activeTab === 'overview' && <OverviewTab user={user} userInfo={userInfo} tenantInfo={tenantInfo} realmRoles={realmRoles} clientRoles={clientRoles} token={safeToken} />}
							{activeTab === 'subscriptions' && <SubscriptionsTab subscriptions={subscriptions} token={safeToken} tenant={masterTenantId || ''} onRefresh={() => fetchTabData()} />}
							{activeTab === 'tenant-users' && <TenantUsersTab tenantUsers={tenantUsers} token={safeToken} tenant={masterTenantId || ''} onRefresh={() => fetchTabData()} />}
							{activeTab === 'users' && <UsersTab users={users} token={safeToken} onRefresh={() => fetchTabData()} />}
							{activeTab === 'projects' && <ProjectsTab projects={projects} users={users} token={safeToken} onRefresh={() => fetchTabData()} />}
							{activeTab === 'tasks' && <TasksTab tasks={tasks} projects={projects} users={users} token={safeToken} onRefresh={() => fetchTabData()} />}
							{activeTab === 'audit-logs' && <AuditLogsTab auditLogs={auditLogs} />}
							{activeTab === 'activity-logs' && <ActivityLogsTab activityLogs={activityLogs} />}
						</div>
					)
				)}
			</div>
		</ProtectedRoute>
	)
}

// Tab Components
function OverviewTab({ user, userInfo, tenantInfo, realmRoles, clientRoles, token }: any) {
	return (
		<>
			<h2 style={{ marginTop: 0, marginBottom: 20, fontSize: 22, fontWeight: 600 }}>👤 User Information</h2>
			<div style={{ display: 'grid', gap: '12px', marginBottom: 24 }}>
				<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #0d6efd' }}>
					<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Name</strong>
					<div style={{ marginTop: 4, fontSize: 16 }}>{user?.name ?? user?.preferred_username ?? '—'}</div>
				</div>
				<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #0d6efd' }}>
					<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Email</strong>
					<div style={{ marginTop: 4, fontSize: 16 }}>{user?.email ?? '—'}</div>
				</div>
				<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #0d6efd' }}>
					<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Subject</strong>
					<div style={{ marginTop: 4, fontSize: 14, color: '#6c757d', fontFamily: 'monospace' }}>{userInfo?.subject ?? user?.sub ?? '—'}</div>
				</div>
				<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #28a745' }}>
					<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Tenant ID</strong>
					<div style={{ marginTop: 4, fontSize: 16, color: '#28a745', fontWeight: 500 }}>{userInfo?.tenantId || userInfo?.tenantIdFromJwt || '—'}</div>
				</div>
			</div>

			{tenantInfo && (
				<>
					<h2 style={{ marginTop: 0, marginBottom: 20, fontSize: 22, fontWeight: 600 }}>🏢 Tenant Information</h2>
					<div style={{ display: 'grid', gap: '12px', marginBottom: 24 }}>
						<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #ffc107' }}>
							<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Tenant ID</strong>
							<div style={{ marginTop: 4, fontSize: 16, fontWeight: 500 }}>{tenantInfo.tenantId}</div>
						</div>
						<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #ffc107' }}>
							<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Status</strong>
							<div style={{ marginTop: 4, fontSize: 16 }}>{tenantInfo.status}</div>
						</div>
						<div style={{ padding: '12px', background: '#f8f9fa', borderRadius: 6, borderLeft: '3px solid #ffc107' }}>
							<strong style={{ color: '#6c757d', fontSize: 12, textTransform: 'uppercase' }}>Active</strong>
							<div style={{ marginTop: 4, fontSize: 16 }}>
								<span style={{ 
									padding: '4px 12px', 
									borderRadius: 12, 
									fontSize: 12, 
									fontWeight: 500,
									backgroundColor: tenantInfo.active ? '#d4edda' : '#f8d7da',
									color: tenantInfo.active ? '#155724' : '#721c24'
								}}>
									{tenantInfo.active ? '✓ Yes' : '✗ No'}
								</span>
							</div>
						</div>
					</div>
				</>
			)}

			<h3 style={{ marginTop: 0, marginBottom: 20, fontSize: 20, fontWeight: 600 }}>🔐 Roles</h3>
			<div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '24px' }}>
				<div>
					<strong style={{ color: '#6c757d', fontSize: 14, textTransform: 'uppercase' }}>Realm Roles</strong>
					<ul style={{ listStyle: 'none', padding: 0, marginTop: 12 }}>
						{realmRoles.length ? realmRoles.map((r: string) => (
							<li key={r} style={{ padding: '8px 12px', marginBottom: 8, background: '#e7f3ff', borderRadius: 6, color: '#004085', fontSize: 14 }}>
								{r}
							</li>
						)) : <li style={{ color: '#6c757d', fontStyle: 'italic' }}>None</li>}
					</ul>
				</div>
				<div>
					<strong style={{ color: '#6c757d', fontSize: 14, textTransform: 'uppercase' }}>Client Roles</strong>
					<ul style={{ listStyle: 'none', padding: 0, marginTop: 12 }}>
						{clientRoles.length ? clientRoles.map((r: string) => (
							<li key={r} style={{ padding: '8px 12px', marginBottom: 8, background: '#fff3cd', borderRadius: 6, color: '#856404', fontSize: 14 }}>
								{r}
							</li>
						)) : <li style={{ color: '#6c757d', fontStyle: 'italic' }}>None</li>}
					</ul>
				</div>
			</div>
		</>
	)
}

function SubscriptionsTab({ subscriptions, token, tenant, onRefresh }: { subscriptions: Subscription[], token: string, tenant: string, onRefresh: () => void }) {
	const [showCreateForm, setShowCreateForm] = useState(false)
	const [deletingSubscriptionId, setDeletingSubscriptionId] = useState<string | null>(null)

	const handleDelete = async (subscriptionId: string) => {
		if (!confirm('Are you sure you want to delete this subscription?')) return
		
		setDeletingSubscriptionId(subscriptionId)
		try {
			await subscriptionService.delete(subscriptionId, token)
			onRefresh()
		} catch (err: any) {
			alert('Failed to delete subscription: ' + (err.message || 'Unknown error'))
		} finally {
			setDeletingSubscriptionId(null)
		}
	}

	return (
		<>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
				<h2 style={{ marginTop: 0, marginBottom: 0, fontSize: 22, fontWeight: 600 }}>💳 Subscriptions</h2>
				{!showCreateForm && (
					<button
						onClick={() => setShowCreateForm(true)}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontWeight: 500,
						}}
					>
						+ Create Subscription
					</button>
				)}
			</div>

			{showCreateForm && (
				<CreateSubscriptionForm
					tenantId={tenant}
					token={token}
					onSuccess={() => {
						setShowCreateForm(false)
						onRefresh()
					}}
					onCancel={() => setShowCreateForm(false)}
				/>
			)}

			{!showCreateForm && (
				subscriptions.length === 0 ? (
					<p style={{ color: '#6c757d' }}>No subscriptions found.</p>
				) : (
					<div style={{ display: 'grid', gap: '16px' }}>
						{subscriptions.map(sub => (
							<div key={sub.subscriptionId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
								<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 8 }}>
									<div style={{ flex: 1 }}>
										<strong style={{ fontSize: 16 }}>{sub.planName}</strong>
										<div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>Status: {sub.status || 'N/A'}</div>
									</div>
									<div style={{ display: 'flex', gap: 8, alignItems: 'start' }}>
										<div style={{ textAlign: 'right' }}>
											<div style={{ fontSize: 18, fontWeight: 600 }}>{sub.amount} {sub.currency || 'USD'}</div>
											<div style={{ fontSize: 12, color: '#6c757d' }}>{sub.billingCycle || 'N/A'}</div>
										</div>
										<button
											onClick={() => handleDelete(sub.subscriptionId)}
											disabled={deletingSubscriptionId === sub.subscriptionId}
											style={{
												padding: '6px 12px',
												background: '#dc3545',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: deletingSubscriptionId === sub.subscriptionId ? 'not-allowed' : 'pointer',
												fontSize: 12,
												opacity: deletingSubscriptionId === sub.subscriptionId ? 0.6 : 1,
											}}
										>
											{deletingSubscriptionId === sub.subscriptionId ? 'Deleting...' : 'Delete'}
										</button>
									</div>
								</div>
								<div style={{ fontSize: 12, color: '#6c757d', marginTop: 8 }}>
									ID: <code style={{ background: '#e9ecef', padding: '2px 6px', borderRadius: 3 }}>{sub.subscriptionId}</code>
								</div>
							</div>
						))}
					</div>
				)
			)}
		</>
	)
}

function TenantUsersTab({ tenantUsers, token, tenant, onRefresh }: { tenantUsers: TenantUser[], token: string, tenant: string, onRefresh: () => void }) {
	const [showCreateForm, setShowCreateForm] = useState(false)
	const [deletingUserId, setDeletingUserId] = useState<string | null>(null)

	const handleDelete = async (userId: string) => {
		if (!confirm('Are you sure you want to delete this tenant user?')) return
		
		setDeletingUserId(userId)
		try {
			await tenantUserService.delete(userId, token)
			onRefresh()
		} catch (err: any) {
			alert('Failed to delete tenant user: ' + (err.message || 'Unknown error'))
		} finally {
			setDeletingUserId(null)
		}
	}

	return (
		<>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
				<h2 style={{ marginTop: 0, marginBottom: 0, fontSize: 22, fontWeight: 600 }}>👥 Tenant Users</h2>
				{!showCreateForm && (
					<button
						onClick={() => setShowCreateForm(true)}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontWeight: 500,
						}}
					>
						+ Create Tenant User
					</button>
				)}
			</div>

			{showCreateForm && (
				<CreateTenantUserForm
					tenantId={tenant}
					token={token}
					onSuccess={() => {
						setShowCreateForm(false)
						onRefresh()
					}}
					onCancel={() => setShowCreateForm(false)}
				/>
			)}

			{!showCreateForm && (
				tenantUsers.length === 0 ? (
					<p style={{ color: '#6c757d' }}>No tenant users found.</p>
				) : (
					<div style={{ display: 'grid', gap: '12px' }}>
						{tenantUsers.map(user => (
							<div key={user.userId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
								<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
									<div style={{ flex: 1 }}>
										<strong style={{ fontSize: 16 }}>{user.email}</strong>
										<div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>Role: {user.role}</div>
									</div>
									<div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
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
										<button
											onClick={() => handleDelete(user.userId)}
											disabled={deletingUserId === user.userId}
											style={{
												padding: '6px 12px',
												background: '#dc3545',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: deletingUserId === user.userId ? 'not-allowed' : 'pointer',
												fontSize: 12,
												opacity: deletingUserId === user.userId ? 0.6 : 1,
											}}
										>
											{deletingUserId === user.userId ? 'Deleting...' : 'Delete'}
										</button>
									</div>
								</div>
							</div>
						))}
					</div>
				)
			)}
		</>
	)
}

function UsersTab({ users, token, onRefresh }: { users: User[], token: string, onRefresh: () => void }) {
	const [showCreateForm, setShowCreateForm] = useState(false)
	const [editingUser, setEditingUser] = useState<User | null>(null)
	const [deletingUserId, setDeletingUserId] = useState<string | null>(null)

	const handleDelete = async (userId: string) => {
		if (!confirm('Are you sure you want to delete this user?')) return
		
		setDeletingUserId(userId)
		try {
			await userService.delete(userId, token)
			onRefresh()
		} catch (err: any) {
			alert('Failed to delete user: ' + (err.message || 'Unknown error'))
		} finally {
			setDeletingUserId(null)
		}
	}

	return (
		<>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
				<h2 style={{ marginTop: 0, marginBottom: 0, fontSize: 22, fontWeight: 600 }}>👤 Users</h2>
				{!showCreateForm && !editingUser && (
					<button
						onClick={() => setShowCreateForm(true)}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontWeight: 500,
						}}
					>
						+ Create User
					</button>
				)}
			</div>

			{showCreateForm && (
				<CreateUserForm
					token={token}
					onSuccess={() => {
						setShowCreateForm(false)
						onRefresh()
					}}
					onCancel={() => setShowCreateForm(false)}
				/>
			)}

			{editingUser && (
				<EditUserForm
					user={editingUser}
					token={token}
					onSuccess={() => {
						setEditingUser(null)
						onRefresh()
					}}
					onCancel={() => setEditingUser(null)}
				/>
			)}

			{!showCreateForm && !editingUser && (
				users.length === 0 ? (
					<p style={{ color: '#6c757d' }}>No users found.</p>
				) : (
					<div style={{ display: 'grid', gap: '12px' }}>
						{users.map(user => (
							<div key={user.userId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
								<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
									<div style={{ flex: 1 }}>
										<strong style={{ fontSize: 16 }}>{user.firstName} {user.lastName}</strong>
										<div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>{user.email} • {user.role || 'No role'}</div>
									</div>
									<div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
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
										<button
											onClick={() => setEditingUser(user)}
											style={{
												padding: '6px 12px',
												background: '#0d6efd',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: 'pointer',
												fontSize: 12,
											}}
										>
											Edit
										</button>
										<button
											onClick={() => handleDelete(user.userId)}
											disabled={deletingUserId === user.userId}
											style={{
												padding: '6px 12px',
												background: '#dc3545',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: deletingUserId === user.userId ? 'not-allowed' : 'pointer',
												fontSize: 12,
												opacity: deletingUserId === user.userId ? 0.6 : 1,
											}}
										>
											{deletingUserId === user.userId ? 'Deleting...' : 'Delete'}
										</button>
									</div>
								</div>
							</div>
						))}
					</div>
				)
			)}
		</>
	)
}

function ProjectsTab({ projects, users, token, onRefresh }: { projects: Project[], users: User[], token: string, onRefresh: () => void }) {
	const [showCreateForm, setShowCreateForm] = useState(false)
	const [editingProject, setEditingProject] = useState<Project | null>(null)
	const [deletingProjectId, setDeletingProjectId] = useState<string | null>(null)

	const handleDelete = async (projectId: string) => {
		if (!confirm('Are you sure you want to delete this project? This will also delete all associated tasks.')) return
		
		setDeletingProjectId(projectId)
		try {
			await projectService.delete(projectId, token)
			onRefresh()
		} catch (err: any) {
			alert('Failed to delete project: ' + (err.message || 'Unknown error'))
		} finally {
			setDeletingProjectId(null)
		}
	}

	return (
		<>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
				<h2 style={{ marginTop: 0, marginBottom: 0, fontSize: 22, fontWeight: 600 }}>📁 Projects</h2>
				{!showCreateForm && !editingProject && (
					<button
						onClick={() => setShowCreateForm(true)}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontWeight: 500,
						}}
					>
						+ Create Project
					</button>
				)}
			</div>

			{showCreateForm && (
				<CreateProjectForm
					token={token}
					users={users}
					onSuccess={() => {
						setShowCreateForm(false)
						onRefresh()
					}}
					onCancel={() => setShowCreateForm(false)}
				/>
			)}

			{editingProject && (
				<EditProjectForm
					project={editingProject}
					users={users}
					token={token}
					onSuccess={() => {
						setEditingProject(null)
						onRefresh()
					}}
					onCancel={() => setEditingProject(null)}
				/>
			)}

			{!showCreateForm && !editingProject && (
				projects.length === 0 ? (
					<p style={{ color: '#6c757d' }}>No projects found.</p>
				) : (
					<div style={{ display: 'grid', gap: '12px' }}>
						{projects.map(project => (
							<div key={project.projectId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
								<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
									<div style={{ flex: 1 }}>
										<strong style={{ fontSize: 16 }}>{project.name}</strong>
										{project.description && <div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>{project.description}</div>}
										<div style={{ fontSize: 12, color: '#6c757d', marginTop: 8 }}>Status: {project.status || 'N/A'}</div>
									</div>
									<div style={{ display: 'flex', gap: 8 }}>
										<button
											onClick={() => setEditingProject(project)}
											style={{
												padding: '6px 12px',
												background: '#0d6efd',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: 'pointer',
												fontSize: 12,
											}}
										>
											Edit
										</button>
										<button
											onClick={() => handleDelete(project.projectId)}
											disabled={deletingProjectId === project.projectId}
											style={{
												padding: '6px 12px',
												background: '#dc3545',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: deletingProjectId === project.projectId ? 'not-allowed' : 'pointer',
												fontSize: 12,
												opacity: deletingProjectId === project.projectId ? 0.6 : 1,
											}}
										>
											{deletingProjectId === project.projectId ? 'Deleting...' : 'Delete'}
										</button>
									</div>
								</div>
							</div>
						))}
					</div>
				)
			)}
		</>
	)
}

function TasksTab({ tasks, projects, users, token, onRefresh }: { tasks: Task[], projects: Project[], users: User[], token: string, onRefresh: () => void }) {
	const [showCreateForm, setShowCreateForm] = useState(false)
	const [editingTask, setEditingTask] = useState<Task | null>(null)
	const [deletingTaskId, setDeletingTaskId] = useState<string | null>(null)

	const handleDelete = async (taskId: string) => {
		if (!confirm('Are you sure you want to delete this task?')) return
		
		setDeletingTaskId(taskId)
		try {
			await taskService.delete(taskId, token)
			onRefresh()
		} catch (err: any) {
			alert('Failed to delete task: ' + (err.message || 'Unknown error'))
		} finally {
			setDeletingTaskId(null)
		}
	}

	return (
		<>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
				<h2 style={{ marginTop: 0, marginBottom: 0, fontSize: 22, fontWeight: 600 }}>✅ Tasks</h2>
				{!showCreateForm && !editingTask && (
					<button
						onClick={() => setShowCreateForm(true)}
						style={{
							padding: '10px 20px',
							background: '#28a745',
							color: 'white',
							border: 'none',
							borderRadius: 6,
							cursor: 'pointer',
							fontWeight: 500,
						}}
					>
						+ Create Task
					</button>
				)}
			</div>

			{showCreateForm && (
				<CreateTaskForm
					token={token}
					projects={projects}
					users={users}
					onSuccess={() => {
						setShowCreateForm(false)
						onRefresh()
					}}
					onCancel={() => setShowCreateForm(false)}
				/>
			)}

			{editingTask && (
				<EditTaskForm
					task={editingTask}
					projects={projects}
					users={users}
					token={token}
					onSuccess={() => {
						setEditingTask(null)
						onRefresh()
					}}
					onCancel={() => setEditingTask(null)}
				/>
			)}

			{!showCreateForm && !editingTask && (
				tasks.length === 0 ? (
					<p style={{ color: '#6c757d' }}>No tasks found.</p>
				) : (
					<div style={{ display: 'grid', gap: '12px' }}>
						{tasks.map(task => (
							<div key={task.taskId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
								<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
									<div style={{ flex: 1 }}>
										<strong style={{ fontSize: 16 }}>{task.title}</strong>
										{task.description && <div style={{ fontSize: 14, color: '#6c757d', marginTop: 4 }}>{task.description}</div>}
										<div style={{ fontSize: 12, color: '#6c757d', marginTop: 8 }}>
											Status: {task.status || 'N/A'} {task.dueDate && `• Due: ${task.dueDate}`}
										</div>
									</div>
									<div style={{ display: 'flex', gap: 8 }}>
										<button
											onClick={() => setEditingTask(task)}
											style={{
												padding: '6px 12px',
												background: '#0d6efd',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: 'pointer',
												fontSize: 12,
											}}
										>
											Edit
										</button>
										<button
											onClick={() => handleDelete(task.taskId)}
											disabled={deletingTaskId === task.taskId}
											style={{
												padding: '6px 12px',
												background: '#dc3545',
												color: 'white',
												border: 'none',
												borderRadius: 4,
												cursor: deletingTaskId === task.taskId ? 'not-allowed' : 'pointer',
												fontSize: 12,
												opacity: deletingTaskId === task.taskId ? 0.6 : 1,
											}}
										>
											{deletingTaskId === task.taskId ? 'Deleting...' : 'Delete'}
										</button>
									</div>
								</div>
							</div>
						))}
					</div>
				)
			)}
		</>
	)
}

function AuditLogsTab({ auditLogs }: { auditLogs: AuditLog[] }) {
	return (
		<>
			<h2 style={{ marginTop: 0, marginBottom: 20, fontSize: 22, fontWeight: 600 }}>📋 Audit Logs</h2>
			{auditLogs.length === 0 ? (
				<p style={{ color: '#6c757d' }}>No audit logs found.</p>
			) : (
				<div style={{ display: 'grid', gap: '12px' }}>
					{auditLogs.map(log => (
						<div key={log.logId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
							<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
								<div>
									<strong style={{ fontSize: 16 }}>{log.action}</strong>
									<div style={{ fontSize: 12, color: '#6c757d', marginTop: 4 }}>{new Date(log.createdAt).toLocaleString()}</div>
								</div>
								<div style={{ fontSize: 12, color: '#6c757d' }}>ID: {log.logId}</div>
							</div>
						</div>
					))}
				</div>
			)}
		</>
	)
}

function ActivityLogsTab({ activityLogs }: { activityLogs: ActivityLog[] }) {
	return (
		<>
			<h2 style={{ marginTop: 0, marginBottom: 20, fontSize: 22, fontWeight: 600 }}>📝 Activity Logs</h2>
			{activityLogs.length === 0 ? (
				<p style={{ color: '#6c757d' }}>No activity logs found.</p>
			) : (
				<div style={{ display: 'grid', gap: '12px' }}>
					{activityLogs.map(log => (
						<div key={log.logId} style={{ padding: '16px', background: '#f8f9fa', borderRadius: 6, border: '1px solid #dee2e6' }}>
							<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
								<div>
									<strong style={{ fontSize: 16 }}>{log.action || 'N/A'}</strong>
									<div style={{ fontSize: 12, color: '#6c757d', marginTop: 4 }}>
										{log.entityType && `${log.entityType} • `}{new Date(log.createdAt).toLocaleString()}
									</div>
								</div>
								<div style={{ fontSize: 12, color: '#6c757d' }}>ID: {log.logId}</div>
							</div>
						</div>
					))}
				</div>
			)}
		</>
	)
}
