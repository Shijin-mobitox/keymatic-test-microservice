import Keycloak from 'keycloak-js'

export type KeycloakInstance = any // Keycloak instance type
type KeycloakConfig = {
	url: string
	realm: string
	clientId: string
}
type KeycloakInitOptions = any // Keycloak init options

export function createKeycloakInstance(): KeycloakInstance {
	// Support overriding Keycloak base URL via Vite env for local docker-compose
	const baseUrl =
		(import.meta as any).env?.VITE_KEYCLOAK_BASE_URL?.toString() || 'http://localhost:8085/'
	const config: KeycloakConfig = {
		url: baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`,
		realm: 'kymatic', // Single realm for all tenants
		clientId: 'react-client'
	}
	// Keycloak can be used as a constructor or function
	const KC = Keycloak as any
	return new KC(config) as KeycloakInstance
}

// Legacy function for backward compatibility
export function createKeycloakForTenant(tenant: string): KeycloakInstance {
	return createKeycloakInstance()
}

export const defaultInitOptions: KeycloakInitOptions = {
	onLoad: 'login-required',
	pkceMethod: 'S256',
	checkLoginIframe: false,
	enableLogging: true, // Enable logging to debug redirect issues
	responseMode: 'fragment', // Use fragment mode for redirects
	// Explicitly set redirectUri to match Keycloak configuration
	redirectUri: typeof window !== 'undefined' ? window.location.origin + window.location.pathname : undefined
}


