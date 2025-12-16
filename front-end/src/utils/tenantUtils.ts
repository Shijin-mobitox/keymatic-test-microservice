export function getTenantFromHostname(hostname: string): string | null {
	const baseHost = hostname.split(':')[0]
	if (!baseHost) return null
	const parts = baseHost.split('.').filter(Boolean)
	// myapp domains like tenantA.myapp.com
	if (parts.length >= 3) {
		return parts[0] || null
	}
	// localhost subdomain like tenantA.localhost
	if (parts.length >= 2 && parts[parts.length - 1]?.toLowerCase() === 'localhost') {
		return parts[0] || null
	}
	return null
}

function getTenantFromQueryParam(): string | null {
	try {
		const params = new URLSearchParams(window.location.search)
		const t = params.get('tenant')
		return t && t.trim().length > 0 ? t.trim() : null
	} catch {
		return null
	}
}

function getTenantFromLocalStorage(): string | null {
	try {
		const t = window.localStorage.getItem('tenantOverride')
		return t && t.trim().length > 0 ? t.trim() : null
	} catch {
		return null
	}
}

// Detect tenant with fallbacks:
// 1) ?tenant=tenantA (stored to localStorage for refresh/navigation)
// 2) localStorage 'tenantOverride'
// 3) subdomain: tenantA.myapp.com
export function detectTenant(): string | null {
	const fromQuery = getTenantFromQueryParam()
	if (fromQuery) {
		try {
			window.localStorage.setItem('tenantOverride', fromQuery)
		} catch {
			// ignore storage errors
		}
		return fromQuery
	}
	const fromStorage = getTenantFromLocalStorage()
	if (fromStorage) return fromStorage
	return getTenantFromHostname(window.location.hostname)
}

export function requireTenantFromLocation(): string {
	const tenant = detectTenant()
	if (!tenant) {
		throw new Error('Tenant could not be detected from subdomain.')
	}
	return tenant
}


