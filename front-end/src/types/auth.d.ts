declare module 'keycloak-js' {
	interface KeycloakTokenParsed {
		sub?: string // Subject (user ID)
		name?: string
		email?: string
		preferred_username?: string
		given_name?: string
		family_name?: string
		tenant_id?: string // Tenant ID from JWT claim
		realm_access?: {
			roles?: string[]
		}
		resource_access?: Record<
			string,
			{
				roles?: string[]
			}
		>
	}
}


