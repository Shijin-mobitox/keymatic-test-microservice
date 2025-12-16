import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { ServerResponse } from 'http'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Proxy all Keycloak requests to avoid CORS issues
      // This handles GET, POST, and all HTTP methods
      '/realms': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 60000, // 60 seconds timeout
        proxyTimeout: 60000,
        configure: (proxy, _options) => {
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            // Set headers so Keycloak recognizes the frontend origin
            const origin = req.headers.origin || req.headers.host;
            if (origin) {
              proxyReq.setHeader('X-Forwarded-Host', origin);
              proxyReq.setHeader('X-Forwarded-Proto', 'http');
              proxyReq.setHeader('X-Forwarded-For', req.socket.remoteAddress || '127.0.0.1');
            }
            // Log proxied requests for debugging
            console.log(`[Vite Proxy] ${req.method} ${req.url} -> http://localhost:8085${req.url}`);
          });
          
          proxy.on('error', (err, _req, res) => {
            console.error('[Vite Proxy] Error:', err.message);
            if (res instanceof ServerResponse && !res.headersSent) {
              res.writeHead(502, {
                'Content-Type': 'text/plain',
              });
              res.end('Bad Gateway: Keycloak connection failed');
            }
          });
          
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log(`[Vite Proxy] ${req.method} ${req.url} -> ${proxyRes.statusCode}`);
          });
        },
      },
      // Proxy Keycloak static resources (JS, CSS, images, etc.)
      '/resources': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 30000,
        proxyTimeout: 30000,
      },
      // Proxy Keycloak auth endpoints
      '/auth': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 60000,
        proxyTimeout: 60000,
      },
      // Proxy other Keycloak paths that might be used
      '/js': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 30000,
        proxyTimeout: 30000,
      },
      '/css': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 30000,
        proxyTimeout: 30000,
      },
      '/theme': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
        timeout: 30000,
        proxyTimeout: 30000,
      },
    },
  },
})
