import Keycloak from 'keycloak-js'

export type KeycloakInstance = any

/**
 * Creates a Keycloak instance with proper configuration for Keycloak 26.2.0
 */
export function createKeycloakInstance(): KeycloakInstance {
	const baseUrl =
		(import.meta as any).env?.VITE_KEYCLOAK_BASE_URL?.toString() || 'http://localhost:8085/'
	
	const config = {
		url: baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl,
		realm: 'kymatic',
		clientId: 'react-client'
	}
	
	const KC = Keycloak as any
	return new KC(config)
}

/**
 * Default initialization options for Keycloak 26.2.0
 * 
 * Key points for Keycloak 26.x:
 * - PKCE is required for public clients
 * - Nonce validation changed - use useNonce: false for compatibility
 * - Use 'standard' flow for PKCE
 */
export const defaultInitOptions = {
	onLoad: 'login-required',
	pkceMethod: 'S256',
	checkLoginIframe: false,
	enableLogging: true,
	responseMode: 'fragment' as const,
	flow: 'standard' as const,
	useNonce: false // Disable nonce validation for Keycloak 26.x compatibility
}
