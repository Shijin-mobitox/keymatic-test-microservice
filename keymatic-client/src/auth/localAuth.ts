// Local authentication service - bypasses Keycloak and uses direct database authentication

import { API_BASE_URL, TENANT_SLUG } from '../config/api';

export interface LoginCredentials {
  email: string;
  password: string;
  tenantId?: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
  token_type: string;
}

export interface TokenData {
  sub: string;
  preferred_username?: string;
  email?: string;
  tenant_id?: string;
  tenantId?: string;
  tenant?: string;
  exp: number;
  iat: number;
  [key: string]: any;
}

/**
 * Parse JWT token to extract user information
 */
export function parseToken(token: string): TokenData | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Failed to parse token:', error);
    return null;
  }
}

/**
 * Login with email and password using local authentication endpoint
 */
export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const loginUrl = `${API_BASE_URL}/api/auth/login`;
  
  // Determine tenant ID from credentials or use TENANT_SLUG
  const tenantId = credentials.tenantId || TENANT_SLUG || null;

  try {
    const response = await fetch(loginUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(tenantId ? { 'X-Tenant-ID': tenantId } : {}),
      },
      body: JSON.stringify({
        email: credentials.email,
        password: credentials.password,
        tenantId: tenantId,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: 'Invalid credentials' }));
      throw new Error(errorData.error || errorData.message || 'Login failed');
    }

    const data = await response.json();
    
    // Convert response to AuthResponse format
    return {
      access_token: data.accessToken || data.access_token,
      refresh_token: data.refreshToken || data.refresh_token,
      expires_in: data.expiresIn || data.expires_in || 3600,
      token_type: data.tokenType || data.token_type || 'Bearer',
    };
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Network error during login');
  }
}

/**
 * Refresh access token using refresh token
 */
export async function refreshToken(refreshToken: string): Promise<AuthResponse> {
  const refreshUrl = `${API_BASE_URL}/api/auth/refresh`;

  try {
    const response = await fetch(refreshUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        refresh_token: refreshToken,
      }),
    });

    if (!response.ok) {
      throw new Error('Token refresh failed');
    }

    const data = await response.json();
    
    // Convert response to AuthResponse format
    return {
      access_token: data.access_token || data.accessToken,
      refresh_token: refreshToken, // Keep existing refresh token
      expires_in: data.expires_in || data.expiresIn || 3600,
      token_type: data.token_type || data.tokenType || 'Bearer',
    };
  } catch (error) {
    throw new Error('Failed to refresh token');
  }
}

/**
 * Logout - clear local storage
 */
export function logout(): void {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('tokenExpiry');
  localStorage.removeItem('tenantId');
  localStorage.removeItem('user');
}

/**
 * Get stored token from localStorage
 */
export function getStoredToken(): string | null {
  return localStorage.getItem('token');
}

/**
 * Check if token is expired
 */
export function isTokenExpired(token: string): boolean {
  const tokenData = parseToken(token);
  if (!tokenData || !tokenData.exp) {
    return true;
  }
  
  // Check if token expires in less than 1 minute
  const expiryTime = tokenData.exp * 1000; // Convert to milliseconds
  const now = Date.now();
  return expiryTime - now < 60000;
}

/**
 * Auto-refresh token if needed
 */
export async function ensureValidToken(): Promise<string | null> {
  const token = getStoredToken();
  if (!token) {
    return null;
  }

  if (!isTokenExpired(token)) {
    return token;
  }

  // Try to refresh
  const refreshTokenValue = localStorage.getItem('refreshToken');
  if (!refreshTokenValue) {
    logout();
    return null;
  }

  try {
    const response = await refreshToken(refreshTokenValue);
    localStorage.setItem('token', response.access_token);
    if (response.refresh_token) {
      localStorage.setItem('refreshToken', response.refresh_token);
    }
    if (response.expires_in) {
      const expiry = Date.now() + response.expires_in * 1000;
      localStorage.setItem('tokenExpiry', expiry.toString());
    }
    return response.access_token;
  } catch (error) {
    console.error('Token refresh failed:', error);
    logout();
    return null;
  }
}

