import React, { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { KeycloakTokenParsed } from 'keycloak-js'
import { createKeycloakInstance, defaultInitOptions } from '@auth/keycloak'
import { detectTenant } from '@utils/tenantUtils'

// Get the type from the return type of createKeycloakInstance
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
	const hasProcessedCallbackRef = useRef<boolean>(false)

	const clearRefreshTimer = () => {
		if (refreshTimerRef.current) {
			window.clearInterval(refreshTimerRef.current)
			refreshTimerRef.current = null
		}
	}

	const scheduleTokenRefresh = useCallback(
		(instance: KeycloakInstance) => {
			clearRefreshTimer()
			refreshTimerRef.current = window.setInterval(() => {
				instance.updateToken(60).catch(() => {
					instance.login()
				})
			}, 15_000)
		},
		[]
	)

	const updateTenantFromToken = useCallback((tokenParsed: KeycloakTokenParsed | undefined) => {
		// Extract tenant_id from JWT token claim when available
		const tenantIdFromToken = (tokenParsed as any)?.tenant_id as string | undefined

		// Fallback: detect tenant from URL / local storage when token doesn't carry it
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

	const initKeycloak = useCallback(
		async () => {
			// Prevent multiple simultaneous initializations
			if (isInitializingRef.current) {
				console.log('Keycloak: Already initializing, skipping...')
				return
			}
			
			// Check if we've already processed a callback
			const hasCode = window.location.search.includes('code=') || window.location.hash.includes('code=')
			if (hasCode && hasProcessedCallbackRef.current) {
				console.log('Keycloak: Callback already processed, skipping init')
				return
			}
			
			isInitializingRef.current = true
			setError(null)
			
			try {
				const instance = createKeycloakInstance()
				
				// Set up event handlers BEFORE init to catch redirect callbacks
				instance.onAuthSuccess = () => {
					console.log('Keycloak: Auth success callback')
					hasProcessedCallbackRef.current = true
					setIsAuthenticated(true)
					setToken(instance.token)
					setUser(instance.tokenParsed)
					updateTenantFromToken(instance.tokenParsed)
					
					// Clear the URL hash after successful authentication
					// Delay to ensure Keycloak has fully processed the redirect
					setTimeout(() => {
						const cleanUrl = window.location.origin + window.location.pathname
						if (window.location.href !== cleanUrl) {
							window.history.replaceState(null, '', cleanUrl)
							console.log('Keycloak: URL cleaned')
						}
					}, 500)
				}
				
				instance.onAuthError = (error: any) => {
					console.error('Keycloak: Auth error', error)
					setError('Authentication error.')
				}
				
				instance.onTokenExpired = () => {
					console.log('Keycloak: Token expired, refreshing...')
					instance
						.updateToken(60)
						.then((refreshed: boolean) => {
							if (refreshed) {
								setToken(instance.token)
								setUser(instance.tokenParsed)
								updateTenantFromToken(instance.tokenParsed)
							} else {
								console.log('Keycloak: Token refresh failed, redirecting to login')
								instance.login()
							}
						})
						.catch((err: any) => {
							console.error('Keycloak: Token refresh error', err)
							instance.login()
						})
				}
				
				instance.onAuthRefreshSuccess = () => {
					console.log('Keycloak: Token refresh success')
					setToken(instance.token)
					setUser(instance.tokenParsed)
					updateTenantFromToken(instance.tokenParsed)
				}
				
				instance.onAuthLogout = () => {
					console.log('Keycloak: Logout')
					setIsAuthenticated(false)
					setToken(undefined)
					setUser(undefined)
			setTenant('')
			if (typeof window !== 'undefined') {
				try {
					window.localStorage.removeItem('tenantOverride')
				} catch {
					// ignore storage errors
				}
			}
				}
				
				setKeycloak(instance)

				// Check if we're in a redirect callback (has code in URL)
				const hasCode = window.location.search.includes('code=') || window.location.hash.includes('code=')
				
				console.log('Keycloak: Initializing...', { hasCode, hash: window.location.hash.substring(0, 100) })
				
				// If we have a code, mark that we're processing a callback
				if (hasCode) {
					hasProcessedCallbackRef.current = true
				}
				
				// Use check-sso when we have a code (redirect callback) to prevent redirect loop
				// Use login-required only when we don't have a code
				const initOptions = hasCode 
					? { ...defaultInitOptions, onLoad: 'check-sso' as const }
					: defaultInitOptions
				
				// Initialize Keycloak - it will handle the redirect callback automatically
				const authenticated = await instance.init(initOptions)
				
				console.log('Keycloak: Init result', { authenticated, hasToken: !!instance.token, hasCode })
				
				// If we have a token after init, set it
				if (instance.token) {
					setIsAuthenticated(true)
					setToken(instance.token)
					setUser(instance.tokenParsed)
					updateTenantFromToken(instance.tokenParsed)
					scheduleTokenRefresh(instance)
					
					// Clear URL after a delay to let Keycloak finish processing
					if (hasCode) {
						setTimeout(() => {
							const cleanUrl = window.location.origin + window.location.pathname
							if (window.location.href !== cleanUrl) {
								window.history.replaceState(null, '', cleanUrl)
								console.log('Keycloak: URL cleaned after callback')
							}
						}, 500)
					}
				} else if (!authenticated && !hasCode) {
					// Only redirect to login if we don't have a code (not in callback)
					console.log('Keycloak: Not authenticated and no callback, redirecting to login')
					await instance.login()
				} else if (hasCode && !authenticated) {
					// We have a code but no token - Keycloak should process it
					// Wait a bit longer for the onAuthSuccess callback to fire
					console.log('Keycloak: Has code but not authenticated yet, waiting for callback...')
					// The onAuthSuccess handler will be called by Keycloak
				}
			} catch (e: any) {
				console.error('Keycloak: Init error', e)
				setError(e?.message ?? 'Failed to initialize Keycloak.')
			} finally {
				isInitializingRef.current = false
			}
		},
		[scheduleTokenRefresh, updateTenantFromToken]
	)

	useEffect(() => {
		// Only initialize once, not on every render
		// Don't initialize if we're in a redirect callback that's already been processed
		const hasCode = window.location.search.includes('code=') || window.location.hash.includes('code=')
		
		if (hasCode && hasProcessedCallbackRef.current) {
			console.log('Keycloak: Skipping init - callback already processed')
			return
		}
		
		initKeycloak()
		
		return () => {
			clearRefreshTimer()
		}
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []) // Empty deps - only run once on mount

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


