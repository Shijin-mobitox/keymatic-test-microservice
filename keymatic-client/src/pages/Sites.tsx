import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import type { Site } from '../types';
import './Sites.css';

export function Sites() {
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    siteName: '',
    siteCode: '',
    address: '',
    city: '',
    state: '',
    country: '',
    postalCode: '',
    phone: '',
    email: '',
    isHeadquarters: false,
  });

  useEffect(() => {
    loadSites();
  }, []);

  const loadSites = async () => {
    try {
      const data = await apiService.listSites();
      setSites(data);
    } catch (error) {
      console.error('Failed to load sites:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createSite(formData);
      setShowForm(false);
      setFormData({
        siteName: '',
        siteCode: '',
        address: '',
        city: '',
        state: '',
        country: '',
        postalCode: '',
        phone: '',
        email: '',
        isHeadquarters: false,
      });
      loadSites();
    } catch (error) {
      console.error('Failed to create site:', error);
      alert('Failed to create site');
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="sites-page">
      <div className="page-header">
        <h1>Sites</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ Add Site'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="site-form">
          <h2>Create New Site</h2>
          <div className="form-row">
            <div className="form-group">
              <label>Site Name *</label>
              <input
                type="text"
                value={formData.siteName}
                onChange={(e) => setFormData({ ...formData, siteName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Site Code *</label>
              <input
                type="text"
                value={formData.siteCode}
                onChange={(e) => setFormData({ ...formData, siteCode: e.target.value.toUpperCase() })}
                required
              />
            </div>
          </div>
          <div className="form-group">
            <label>Address</label>
            <input
              type="text"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>City</label>
              <input
                type="text"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>State</label>
              <input
                type="text"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Country</label>
              <input
                type="text"
                value={formData.country}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Postal Code</label>
              <input
                type="text"
                value={formData.postalCode}
                onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Phone</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
          </div>
          <div className="form-group">
            <label>
              <input
                type="checkbox"
                checked={formData.isHeadquarters}
                onChange={(e) => setFormData({ ...formData, isHeadquarters: e.target.checked })}
              />
              Is Headquarters
            </label>
          </div>
          <button type="submit" className="btn-primary">Create Site</button>
        </form>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Site Code</th>
              <th>Site Name</th>
              <th>Location</th>
              <th>Headquarters</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {sites.map((site) => (
              <tr key={site.siteId}>
                <td><strong>{site.siteCode}</strong></td>
                <td>{site.siteName}</td>
                <td>
                  {[site.city, site.state, site.country].filter(Boolean).join(', ') || '-'}
                </td>
                <td>{site.isHeadquarters ? '🏢 Yes' : 'No'}</td>
                <td>
                  <span className={`badge ${site.isActive ? 'badge-success' : 'badge-danger'}`}>
                    {site.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>{new Date(site.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

