// Keycloak Form Fix - Must run BEFORE Keycloak loads
// This script intercepts Keycloak login forms and rewrites their action URLs
// to use the current origin (which goes through Vite proxy - same origin = cookies work!)

(function() {
  'use strict';
  
  console.log('[Keycloak Form Fix] Script loaded');
  
  // Function to rewrite Keycloak URLs to use current origin (proxy)
  function rewriteKeycloakUrl(url) {
    if (!url) return url;
    
    // If URL points to localhost:8085 (Keycloak), rewrite to use current origin
    if (url.includes('localhost:8085') || url.includes('127.0.0.1:8085')) {
      const newUrl = url.replace(/https?:\/\/[^/]+/, window.location.origin);
      console.log('[Keycloak Form Fix] Rewriting URL:', url, '->', newUrl);
      return newUrl;
    }
    
    return url;
  }
  
  // Intercept form submissions BEFORE they happen
  function interceptFormSubmission(form) {
    const originalSubmit = form.submit;
    form.submit = function() {
      const action = form.getAttribute('action');
      if (action) {
        const newAction = rewriteKeycloakUrl(action);
        if (newAction !== action) {
          form.setAttribute('action', newAction);
          console.log('[Keycloak Form Fix] Rewrote form action on submit');
        }
      }
      return originalSubmit.call(this);
    };
  }
  
  // Fix all forms on the page
  function fixAllForms() {
    document.querySelectorAll('form').forEach(form => {
      const action = form.getAttribute('action');
      if (action && (action.includes('localhost:8085') || action.includes('/realms/') || action.includes('/auth'))) {
        const newAction = rewriteKeycloakUrl(action);
        if (newAction !== action) {
          form.setAttribute('action', newAction);
          console.log('[Keycloak Form Fix] Fixed form:', action, '->', newAction);
        }
        interceptFormSubmission(form);
      }
    });
  }
  
  // Fix forms immediately
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', fixAllForms);
  } else {
    fixAllForms();
  }
  
  // Watch for dynamically added forms
  const observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
      mutation.addedNodes.forEach(function(node) {
        if (node.nodeType === 1) { // Element node
          if (node.tagName === 'FORM') {
            fixAllForms();
          } else if (node.querySelectorAll) {
            const forms = node.querySelectorAll('form');
            if (forms.length > 0) {
              fixAllForms();
            }
          }
        }
      });
    });
  });
  
  observer.observe(document.body || document.documentElement, {
    childList: true,
    subtree: true
  });
  
  // Also intercept beforeunload to catch any last-minute changes
  window.addEventListener('beforeunload', function() {
    fixAllForms();
  });
  
  console.log('[Keycloak Form Fix] Interceptor active');
})();

