import Keycloak from 'keycloak-js';
import { KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID } from '../config/api';

export interface KeycloakInstance {
  init: (options: any) => Promise<boolean>;
  login: (options?: any) => void;
  logout: (options?: any) => void;
  updateToken: (minValidity?: number) => Promise<boolean>;
  token?: string;
  tokenParsed?: any;
  authenticated?: boolean;
  userInfo?: any;
}

let keycloakInstance: KeycloakInstance | null = null;

export function initKeycloak(): Promise<KeycloakInstance> {
  return new Promise((resolve, reject) => {
    if (keycloakInstance) {
      resolve(keycloakInstance);
      return;
    }

    const keycloak = new Keycloak({
      url: KEYCLOAK_URL,
      realm: KEYCLOAK_REALM,
      clientId: KEYCLOAK_CLIENT_ID,
    });

    keycloak
      .init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        enableLogging: true,
        flow: 'standard',
        // Ensure credentials are sent for CORS requests
        checkLoginIframeInterval: 5,
      })
      .then((authenticated) => {
        if (authenticated) {
          keycloakInstance = keycloak as any;
          (window as any).keycloak = keycloak;
          
          // Store token
          if (keycloak.token) {
            localStorage.setItem('token', keycloak.token);
          }

          // Refresh token periodically
          setInterval(() => {
            keycloak.updateToken(30).then((refreshed) => {
              if (refreshed && keycloak.token) {
                localStorage.setItem('token', keycloak.token);
              }
            });
          }, 60000);

          resolve(keycloakInstance!);
        } else {
          reject(new Error('Not authenticated'));
        }
      })
      .catch((error) => {
        console.error('Keycloak initialization failed:', error);
        reject(error);
      });
  });
}

export function getKeycloakInstance(): KeycloakInstance | null {
  return keycloakInstance || ((window as any).keycloak as KeycloakInstance);
}

export function logout() {
  const keycloak = getKeycloakInstance();
  if (keycloak) {
    keycloak.logout();
    localStorage.removeItem('token');
  }
}

