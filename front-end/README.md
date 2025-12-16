## Multi-tenant SaaS Portal (React + TypeScript + Keycloak)

This frontend lives under `front-end/`. It detects the tenant from the subdomain and initializes Keycloak per tenant.

### Scripts
- `npm run dev` or `npm start` - start Vite dev server (HTTPS, port 3000)
- `npm run build` - build for production
- `npm run preview` - preview the production build

Run from this folder:
```
cd front-end
npm install
npm start
```

### Configure Keycloak base URL (docker-compose)
By default, the app points to a local Keycloak at `http://localhost:8085/`. To change:
```
# front-end/.env (create this file)
VITE_KEYCLOAK_BASE_URL=http://localhost:8085/
```
Restart the dev server after changes.

### Fix HTTPS certificate errors locally (recommended)
Browsers may reject the default self-signed cert. Generate and trust local certs for your domains:

1) Install mkcert (Windows via Chocolatey or Scoop):
```
choco install mkcert -y
```
or
```
scoop install mkcert
```

2) Trust the local CA:
```
mkcert -install
```

3) Create certs for your dev domains in `front-end/.cert`:
```
mkdir .cert
cd .cert
mkcert tenantA.myapp.com tenantB.myapp.com \"*.myapp.com\" localhost 127.0.0.1 ::1
```
This generates files like `tenantA.myapp.com+5-key.pem` and a matching `.pem`. Rename/copy them to:
```
key.pem
cert.pem
```
Placed under `front-end/.cert/`. Vite will automatically use them.

If you prefer to avoid HTTPS locally, you can temporarily switch to HTTP by changing `server.https` to `false` in `vite.config.ts`.

### Local Multi-tenant Testing
Add to your hosts file:
```
127.0.0.1 tenantA.myapp.com
127.0.0.1 tenantB.myapp.com
```

Or use localhost subdomains (no hosts entry needed on modern systems):
- `http://tenantA.localhost:3000`
- `http://tenantB.localhost:3000`

Update Keycloak client `react-client`:
- Allowed redirect URIs:
  - `http://*.myapp.com:3000/*`
  - `http://*.localhost:3000/*`
- Web origins:
  - `http://*.myapp.com:3000`
  - `http://*.localhost:3000`

Visit:
- `https://tenantA.myapp.com:3000`
- `https://tenantB.myapp.com:3000`

Keycloak expected at `https://keycloak.myapp.com/` with realms per tenant and client `react-client`.


