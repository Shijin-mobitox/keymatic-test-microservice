// Utility to fix Keycloak cookie issues by intercepting form submissions
// This ensures all Keycloak form submissions go through the Vite proxy (same-origin)

export function fixKeycloakFormSubmissions() {
  if (typeof window === 'undefined') return;

  // Intercept form submissions to Keycloak and rewrite URLs to use proxy
  document.addEventListener('submit', (event) => {
    const form = event.target as HTMLFormElement;
    if (!form || form.tagName !== 'FORM') return;

    const action = form.getAttribute('action');
    if (!action) return;

    // Check if this is a Keycloak form (submitting to localhost:8085)
    if (action.includes('localhost:8085') || action.includes('/realms/') || action.includes('/auth')) {
      // Rewrite the action URL to use the current origin (which goes through Vite proxy)
      const newAction = action.replace(/https?:\/\/[^/]+/, window.location.origin);
      form.setAttribute('action', newAction);
      console.log('[Keycloak Fix] Rewrote form action:', action, '->', newAction);
    }
  }, true); // Use capture phase to intercept early

  // Also intercept any dynamically created forms
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          const element = node as HTMLElement;
          
          // Check for forms
          if (element.tagName === 'FORM') {
            fixFormAction(element as HTMLFormElement);
          }
          
          // Check for nested forms
          const forms = element.querySelectorAll?.('form');
          forms?.forEach((form) => fixFormAction(form));
        }
      });
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true,
  });

  // Fix any existing forms on the page
  document.querySelectorAll('form').forEach((form) => fixFormAction(form));
}

function fixFormAction(form: HTMLFormElement) {
  const action = form.getAttribute('action');
  if (!action) return;

  // Rewrite Keycloak URLs to use proxy
  if (action.includes('localhost:8085') || action.includes('/realms/') || action.includes('/auth')) {
    const newAction = action.replace(/https?:\/\/[^/]+/, window.location.origin);
    form.setAttribute('action', newAction);
    console.log('[Keycloak Fix] Fixed form action:', action, '->', newAction);
  }
}

// Also fix any iframe src attributes that point to Keycloak
export function fixKeycloakIframes() {
  if (typeof window === 'undefined') return;

  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          const element = node as HTMLElement;
          
          if (element.tagName === 'IFRAME') {
            fixIframeSrc(element as HTMLIFrameElement);
          }
          
          const iframes = element.querySelectorAll?.('iframe');
          iframes?.forEach((iframe) => fixIframeSrc(iframe));
        }
      });
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true,
  });

  // Fix existing iframes
  document.querySelectorAll('iframe').forEach((iframe) => fixIframeSrc(iframe));
}

function fixIframeSrc(iframe: HTMLIFrameElement) {
  const src = iframe.getAttribute('src');
  if (!src) return;

  if (src.includes('localhost:8085') || src.includes('/realms/') || src.includes('/auth')) {
    const newSrc = src.replace(/https?:\/\/[^/]+/, window.location.origin);
    iframe.setAttribute('src', newSrc);
    console.log('[Keycloak Fix] Fixed iframe src:', src, '->', newSrc);
  }
}

