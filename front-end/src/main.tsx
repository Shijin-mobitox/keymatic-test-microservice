import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { AuthProvider } from '@auth/AuthProvider'

// Clear stale Keycloak state before app initialization
// This fixes "Invalid nonce" errors after Keycloak 26.x upgrade
// BUT: Don't clear if we're in a callback (URL has state= or code=)
if (typeof window !== 'undefined') {
	try {
		// Check if we're in a callback - if so, don't clear localStorage (we need it)
		const hasCallback = window.location.search.includes('code=') || 
		                    window.location.hash.includes('code=') ||
		                    window.location.hash.includes('state=') ||
		                    window.location.hash.includes('session_state=')
		
		if (!hasCallback) {
			// Only clear if we're NOT in a callback
			// Clear all Keycloak-related localStorage items EXCEPT callback-related items
			// Keycloak-js stores PKCE state (nonce, code_verifier, state) in keys like 'kc-callback-{uuid}'
			const keysToRemove: string[] = []
			for (let i = 0; i < window.localStorage.length; i++) {
				const key = window.localStorage.key(i)
				if (key && (key.startsWith('kc-') || key.toLowerCase().includes('keycloak'))) {
					// Preserve ALL callback-related keys - they contain PKCE state needed for validation
					// Keycloak-js uses patterns like 'kc-callback-{uuid}' for PKCE state
					if (!key.includes('callback') && !key.includes('nonce') && !key.includes('code_verifier') && !key.includes('state')) {
						keysToRemove.push(key)
					}
				}
			}
			keysToRemove.forEach(key => {
				try {
					window.localStorage.removeItem(key)
					console.log('[Keycloak Cleanup] Removed stale localStorage key:', key)
				} catch (e) {
					// Ignore errors
				}
			})

			// Clear sessionStorage as well
			const sessionKeysToRemove: string[] = []
			for (let i = 0; i < window.sessionStorage.length; i++) {
				const key = window.sessionStorage.key(i)
				if (key && (key.startsWith('kc-') || key.toLowerCase().includes('keycloak'))) {
					sessionKeysToRemove.push(key)
				}
			}
			sessionKeysToRemove.forEach(key => {
				try {
					window.sessionStorage.removeItem(key)
					console.log('[Keycloak Cleanup] Removed stale sessionStorage key:', key)
				} catch (e) {
					// Ignore errors
				}
			})
		} else {
			console.log('[Keycloak Cleanup] Callback detected in URL - skipping cleanup to preserve state')
		}

		// Clear any Keycloak cookies (if accessible) - but only if not in callback
		if (!hasCallback && document.cookie) {
			const cookies = document.cookie.split(';')
			cookies.forEach(cookie => {
				const cookieName = cookie.split('=')[0].trim()
				if (cookieName.toLowerCase().includes('keycloak') || cookieName.startsWith('KC_')) {
					// Try to clear by setting expired date
					document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
					document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=${window.location.hostname};`
					console.log('[Keycloak Cleanup] Attempted to clear cookie:', cookieName)
				}
			})
		}
	} catch (error) {
		console.warn('[Keycloak Cleanup] Error during cleanup:', error)
	}
}

ReactDOM.createRoot(document.getElementById('root')!).render(
	<React.StrictMode>
		<AuthProvider>
			<App />
		</AuthProvider>
	</React.StrictMode>
)


