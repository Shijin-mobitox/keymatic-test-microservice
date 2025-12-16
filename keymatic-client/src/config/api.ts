import { getTenantFromHostname } from '../utils/tenant';

const FALLBACK_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';
// Always use localhost:8083 for API calls - tenant is identified by X-Tenant-ID header
// Subdomain routing is only for the frontend, backend uses header-based tenant resolution
const API_HOST_TEMPLATE = import.meta.env.VITE_API_HOST_TEMPLATE || 'http://localhost:8083';

export const TENANT_SLUG = getTenantFromHostname();

export const API_BASE_URL =
  TENANT_SLUG && API_HOST_TEMPLATE.includes('{tenant}')
    ? API_HOST_TEMPLATE.replace('{tenant}', TENANT_SLUG)
    : FALLBACK_API_BASE;

// Use relative path for Keycloak to go through Vite proxy (avoids CORS)
// Vite proxy will forward /realms/* requests to http://localhost:8085
// In production, use full URL from env
const getKeycloakUrl = () => {
  const envUrl = import.meta.env.VITE_KEYCLOAK_BASE_URL;
  // If env URL is set and is absolute, use it (for production)
  if (envUrl && (envUrl.startsWith('http://') || envUrl.startsWith('https://'))) {
    return envUrl;
  }
  // In development, use current origin (Vite proxy will handle /realms/*)
  return typeof window !== 'undefined' ? window.location.origin : '';
};

export const KEYCLOAK_URL = getKeycloakUrl();
export const KEYCLOAK_REALM = 'kymatic';
export const KEYCLOAK_CLIENT_ID = 'react-client';

