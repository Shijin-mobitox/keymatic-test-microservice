import React, { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { KeycloakTokenParsed } from 'keycloak-js'
import { createKeycloakInstance, defaultInitOptions } from '@auth/keycloak'
import { detectTenant } from '@utils/tenantUtils'

type KeycloakInstance = ReturnType<typeof createKeycloakInstance>

type AuthContextValue = {
	isAuthenticated: boolean
	token?: string
	user?: KeycloakTokenParsed
	tenant: string
	login: () => void
	logout: () => void
	hasRole: (role: string) => boolean
	error?: string | null
	retry: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function extractRoles(user?: KeycloakTokenParsed, clientId: string = 'react-client'): Set<string> {
	const set = new Set<string>()
	if (!user) return set
	const realmRoles = user.realm_access?.roles ?? []
	realmRoles.forEach((r) => set.add(r))
	const clientRoles = user.resource_access?.[clientId]?.roles ?? []
	clientRoles.forEach((r) => set.add(r))
	return set
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const [keycloak, setKeycloak] = useState<KeycloakInstance | null>(null)
	const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)
	const [token, setToken] = useState<string | undefined>(undefined)
	const [user, setUser] = useState<KeycloakTokenParsed | undefined>(undefined)
	const [tenant, setTenant] = useState<string>('')
	const [error, setError] = useState<string | null>(null)
	const refreshTimerRef = useRef<number | null>(null)
	const isInitializingRef = useRef<boolean>(false)

	const clearRefreshTimer = () => {
		if (refreshTimerRef.current) {
			window.clearInterval(refreshTimerRef.current)
			refreshTimerRef.current = null
		}
	}

	const scheduleTokenRefresh = useCallback((instance: KeycloakInstance) => {
		clearRefreshTimer()
		refreshTimerRef.current = window.setInterval(() => {
			instance.updateToken(60).catch(() => {
				instance.login()
			})
		}, 15_000)
	}, [])

	const updateTenantFromToken = useCallback((tokenParsed: KeycloakTokenParsed | undefined) => {
		const tenantIdFromToken = (tokenParsed as any)?.tenant_id as string | undefined
		let effectiveTenant = tenantIdFromToken
		if (!effectiveTenant && typeof window !== 'undefined') {
			effectiveTenant = detectTenant() || ''
		}
		const normalizedTenant = effectiveTenant || ''
		setTenant(normalizedTenant)
		if (typeof window !== 'undefined') {
			try {
				if (normalizedTenant) {
					window.localStorage.setItem('tenantOverride', normalizedTenant)
				} else {
					window.localStorage.removeItem('tenantOverride')
				}
			} catch {
				// ignore storage errors
			}
		}
	}, [])

	const initKeycloak = useCallback(async () => {
		// Prevent multiple simultaneous initializations
		if (isInitializingRef.current) {
			return
		}

		// If already authenticated, skip
		if (isAuthenticated && token && keycloak?.token) {
			return
		}

		isInitializingRef.current = true
		setError(null)

		try {
			// Create or reuse Keycloak instance
			let instance = keycloak
			if (!instance) {
				instance = createKeycloakInstance()
				setKeycloak(instance)
			}

			// Set up event handlers (only once)
			if (!instance.onAuthSuccess) {
				instance.onAuthSuccess = () => {
					console.log('Keycloak: Authentication successful')
					setIsAuthenticated(true)
					setToken(instance.token)
					setUser(instance.tokenParsed)
					updateTenantFromToken(instance.tokenParsed)
					scheduleTokenRefresh(instance)
					
					// Clean URL after successful authentication
					const cleanUrl = window.location.origin + window.location.pathname
					if (window.location.href !== cleanUrl) {
						window.history.replaceState(null, '', cleanUrl)
					}
				}

				instance.onAuthError = (error: any) => {
					console.error('Keycloak: Authentication error', error)
					setError(error?.errorDescription || error?.error || 'Authentication failed')
					setIsAuthenticated(false)
					setToken(undefined)
					setUser(undefined)
				}

				instance.onTokenExpired = () => {
					console.log('Keycloak: Token expired, refreshing...')
					instance.updateToken(60).then((refreshed: boolean) => {
						if (refreshed) {
							setToken(instance.token)
							setUser(instance.tokenParsed)
							updateTenantFromToken(instance.tokenParsed)
						} else {
							instance.login()
						}
					}).catch(() => {
						instance.login()
					})
				}

				instance.onAuthRefreshSuccess = () => {
					console.log('Keycloak: Token refresh successful')
					setToken(instance.token)
					setUser(instance.tokenParsed)
					updateTenantFromToken(instance.tokenParsed)
				}

				instance.onAuthLogout = () => {
					console.log('Keycloak: Logged out')
					setIsAuthenticated(false)
					setToken(undefined)
					setUser(undefined)
					setTenant('')
					clearRefreshTimer()
					if (typeof window !== 'undefined') {
						try {
							window.localStorage.removeItem('tenantOverride')
						} catch {
							// ignore
						}
					}
				}
			}

			// Initialize Keycloak
			console.log('Keycloak: Initializing...')
			const authenticated = await instance.init(defaultInitOptions)
			
			console.log('Keycloak: Initialization complete', { authenticated, hasToken: !!instance.token })

			if (authenticated && instance.token) {
				setIsAuthenticated(true)
				setToken(instance.token)
				setUser(instance.tokenParsed)
				updateTenantFromToken(instance.tokenParsed)
				scheduleTokenRefresh(instance)
				
				// Clean URL if it has callback parameters
				const hasCallback = window.location.search.includes('code=') || 
				                    window.location.hash.includes('code=') ||
				                    window.location.hash.includes('state=')
				if (hasCallback) {
					const cleanUrl = window.location.origin + window.location.pathname
					window.history.replaceState(null, '', cleanUrl)
				}
			} else if (!authenticated) {
				// Not authenticated - Keycloak will redirect to login automatically
				console.log('Keycloak: Not authenticated, redirecting to login...')
			}
		} catch (e: any) {
			console.error('Keycloak: Initialization error', e)
			setError(e?.message || e?.error || 'Failed to initialize authentication')
		} finally {
			isInitializingRef.current = false
		}
	}, [keycloak, isAuthenticated, token, scheduleTokenRefresh, updateTenantFromToken])

	useEffect(() => {
		initKeycloak()
		return () => {
			clearRefreshTimer()
		}
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []) // Only run once on mount

	const login = useCallback(() => {
		keycloak?.login()
	}, [keycloak])

	const logout = useCallback(() => {
		keycloak?.logout()
	}, [keycloak])

	const hasRole = useCallback(
		(role: string) => {
			const roles = extractRoles(user)
			return roles.has(role)
		},
		[user]
	)

	const retry = useCallback(() => {
		setError(null)
		initKeycloak()
	}, [initKeycloak])

	const value = useMemo(
		() => ({
			isAuthenticated,
			token,
			user,
			tenant,
			login,
			logout,
			hasRole,
			error,
			retry
		}),
		[error, hasRole, isAuthenticated, login, logout, retry, tenant, token, user]
	)

	if (error) {
		return (
			<div style={{ fontFamily: 'sans-serif', padding: 24 }}>
				<h2>Authentication Error</h2>
				<p>{error}</p>
				<button onClick={retry}>Retry</button>
			</div>
		)
	}

	if (!isAuthenticated) {
		return (
			<div style={{ fontFamily: 'sans-serif', padding: 24, textAlign: 'center' }}>
				<p>Initializing authentication...</p>
			</div>
		)
	}

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
