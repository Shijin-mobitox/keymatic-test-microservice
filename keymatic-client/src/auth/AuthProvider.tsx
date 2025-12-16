import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { getStoredToken, parseToken, ensureValidToken, logout as localLogout } from './localAuth';
import { TENANT_SLUG } from '../config/api';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: any;
  token: string | null;
  tenantId: string | null;
  logout: () => void;
  setIsAuthenticated: (value: boolean) => void;
  setToken: (token: string | null) => void;
  setUser: (user: any) => void;
  setTenantId: (tenantId: string | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<any>(null);
  const [token, setToken] = useState<string | null>(null);
  const [tenantId, setTenantId] = useState<string | null>(localStorage.getItem('tenantId'));

  useEffect(() => {
    // Check for existing token on mount
    const initAuth = async () => {
      try {
        const storedToken = getStoredToken();
        if (storedToken) {
          // Ensure token is still valid and refresh if needed
          const validToken = await ensureValidToken();
          if (validToken) {
            const tokenData = parseToken(validToken);
            if (tokenData) {
              const tenantFromToken =
                tokenData.tenant_id || tokenData.tenantId || tokenData.tenant || null;
              const resolvedTenant = tenantFromToken || TENANT_SLUG || localStorage.getItem('tenantId') || null;

              setIsAuthenticated(true);
              setToken(validToken);
              setUser(tokenData);
              setTenantId(resolvedTenant);

              if (resolvedTenant) {
                localStorage.setItem('tenantId', resolvedTenant);
              }
            } else {
              // Invalid token
              localLogout();
            }
          } else {
            // Token refresh failed
            localLogout();
          }
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        localLogout();
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();

    // Set up token refresh interval (check every 5 minutes)
    const refreshInterval = setInterval(async () => {
      const storedToken = getStoredToken();
      if (storedToken) {
        const validToken = await ensureValidToken();
        if (validToken && validToken !== storedToken) {
          setToken(validToken);
          const tokenData = parseToken(validToken);
          if (tokenData) {
            setUser(tokenData);
            const tenantFromToken =
              tokenData.tenant_id || tokenData.tenantId || tokenData.tenant || null;
            if (tenantFromToken) {
              setTenantId(tenantFromToken);
              localStorage.setItem('tenantId', tenantFromToken);
            }
          }
        } else if (!validToken) {
          // Token refresh failed - logout
          handleLogout();
        }
      }
    }, 5 * 60 * 1000); // 5 minutes

    return () => clearInterval(refreshInterval);
  }, []); // Only run on mount

  const handleLogout = () => {
    localLogout();
    setIsAuthenticated(false);
    setUser(null);
    setToken(null);
    setTenantId(null);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        user,
        token,
        tenantId,
        logout: handleLogout,
        setIsAuthenticated,
        setToken,
        setUser,
        setTenantId,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

