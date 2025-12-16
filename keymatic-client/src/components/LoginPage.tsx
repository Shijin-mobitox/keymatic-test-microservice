import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, parseToken } from '../auth/localAuth';
import { useAuth } from '../auth/AuthProvider';
import { TENANT_SLUG } from '../config/api';
import './LoginPage.css';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { setIsAuthenticated, setToken, setUser, setTenantId } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const response = await login({
        email,
        password,
        tenantId: TENANT_SLUG || undefined,
      });

      // Store tokens
      localStorage.setItem('token', response.access_token);
      if (response.refresh_token) {
        localStorage.setItem('refreshToken', response.refresh_token);
      }
      if (response.expires_in) {
        const expiry = Date.now() + response.expires_in * 1000;
        localStorage.setItem('tokenExpiry', expiry.toString());
      }

      // Parse token to get user info
      const tokenData = parseToken(response.access_token);
      if (tokenData) {
        const tenantFromToken =
          tokenData.tenant_id || tokenData.tenantId || tokenData.tenant || TENANT_SLUG || null;

        // Update auth context
        setToken(response.access_token);
        setUser(tokenData);
        setTenantId(tenantFromToken);
        setIsAuthenticated(true);

        // Store tenant ID
        if (tenantFromToken) {
          localStorage.setItem('tenantId', tenantFromToken);
        }

        // Redirect to dashboard
        navigate('/dashboard');
      } else {
        throw new Error('Failed to parse token');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Login failed. Please try again.';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">KyMatic</h1>
        <p className="login-subtitle">Sign in to your account</p>

        {error && (
          <div className="error-message" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              placeholder="Enter your email"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              placeholder="Enter your password"
              disabled={isLoading}
            />
          </div>

          <button type="submit" className="login-button" disabled={isLoading}>
            {isLoading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        {TENANT_SLUG && (
          <div className="login-footer">
            <p className="tenant-info">Tenant: {TENANT_SLUG}</p>
          </div>
        )}
      </div>
    </div>
  );
}

