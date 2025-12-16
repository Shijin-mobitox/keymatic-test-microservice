const LOCALHOST_HOSTNAMES = new Set(['localhost', '127.0.0.1', '::1']);

function extractSubdomain(hostname: string): string | null {
  if (!hostname) return null;

  const normalized = hostname.toLowerCase();
  if (LOCALHOST_HOSTNAMES.has(normalized)) {
    return null;
  }

  const parts = normalized.split('.');
  if (parts.length <= 1) {
    return null;
  }

  // For domains like tenant.localhost or tenant.dev.local
  if (parts[parts.length - 1] === 'localhost') {
    return parts.slice(0, -1).join('.');
  }

  // For domains like tenant.example.com
  return parts[0];
}

export function getTenantFromHostname(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  return extractSubdomain(window.location.hostname);
}

