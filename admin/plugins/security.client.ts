/**
 * Security Plugin for client-side security enhancements
 */
export default defineNuxtPlugin({
  name: 'security-plugin',
  enforce: 'pre',
  setup() {
    if (process.client) {
      // Add security-related event listeners and protections
      
      // Content Security Policy reporting (if violation monitoring is desired)
      // Uncomment if you want client-side CSP violation reporting
      /*
      document.addEventListener('securitypolicyviolation', (e) => {
        console.warn('CSP Violation:', {
          blockedURI: e.blockedURI,
          violatedDirective: e.violatedDirective,
          originalPolicy: e.originalPolicy
        });
        
        // Could send to a reporting endpoint
        // fetch('/api/csp-report', { 
        //   method: 'POST',
        //   body: JSON.stringify({
        //     blockedURI: e.blockedURI,
        //     violatedDirective: e.violatedDirective
        //   })
        // });
      });
      */
      
      // Prevent clickjacking protection (in addition to X-Frame-Options header)
      if (window.self !== window.top) {
        // Optional: Take action if site is loaded in an iframe
        console.warn('This website is not meant to be displayed in an iframe.');
      }
      
      // Add security for fetch requests (complementary to CSRF tokens)
      const originalFetch = window.fetch;
      window.fetch = function(input, init) {
        const newInit = init || {};
        newInit.credentials = 'same-origin'; // Include cookies
        // Set headers for security
        newInit.headers = {
          ...newInit.headers,
          'X-Requested-With': 'XMLHttpRequest'
        };
        return originalFetch.call(this, input, newInit);
      };
    }
  }
});