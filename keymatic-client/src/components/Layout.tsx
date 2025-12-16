import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider';
import './Layout.css';

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { user, logout } = useAuth();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/users', label: 'Users', icon: '👥' },
    { path: '/sites', label: 'Sites', icon: '🏢' },
    { path: '/roles', label: 'Roles', icon: '🔐' },
    { path: '/permissions', label: 'Permissions', icon: '🔑' },
    { path: '/projects', label: 'Projects', icon: '📁' },
    { path: '/tasks', label: 'Tasks', icon: '✅' },
    { path: '/activity-logs', label: 'Activity Logs', icon: '📝' },
  ];

  return (
    <div className="layout">
      <nav className="navbar">
        <div className="navbar-brand">
          <h1>KeyMatic</h1>
        </div>
        <div className="navbar-user">
          <span>{user?.email || user?.preferred_username}</span>
          <button onClick={logout} className="btn-logout">
            Logout
          </button>
        </div>
      </nav>
      <div className="layout-content">
        <aside className="sidebar">
          <nav className="sidebar-nav">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`nav-link ${location.pathname === item.path ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                <span>{item.label}</span>
              </Link>
            ))}
          </nav>
        </aside>
        <main className="main-content">
          {children}
        </main>
      </div>
    </div>
  );
}

