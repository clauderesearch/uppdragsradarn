// Global fetch interceptor to ensure all API requests include credentials and CSRF token
// This ensures cookies like JSESSIONID and CSRF tokens are sent with all requests

export default defineNuxtPlugin((nuxtApp) => {
  globalThis.fetch = new Proxy(globalThis.fetch, {
    apply: (target, thisArg, argArray) => {
      // Check if this is an API call (modify as needed for your domains)
      if (argArray.length > 0) {
        const url = argArray[0].toString();
        // Only intercept API calls to backend, not all fetch requests
        if (url.includes('/api/') || url.includes('localhost') || url.includes('dev.uppdragsradarn.se')) {
          // If options object doesn't exist, create it
          if (argArray.length === 1) {
            argArray.push({});
          }
          
          // Add credentials: 'include' to ensure cookies are sent
          const options = argArray[1] as RequestInit;
          options.credentials = 'include';
          
          // Add CSRF token header for non-GET requests
          const method = options.method?.toUpperCase() || 'GET';
          if (method !== 'GET' && method !== 'HEAD') {
            // Get CSRF token from the plugin
            const csrfToken = nuxtApp.$csrf?.getToken();
            
            if (csrfToken) {
              options.headers = options.headers || {};
              if (options.headers instanceof Headers) {
                options.headers.set('X-CSRF-TOKEN', csrfToken);
              } else if (Array.isArray(options.headers)) {
                options.headers.push(['X-CSRF-TOKEN', csrfToken]);
              } else {
                (options.headers as Record<string, string>)['X-CSRF-TOKEN'] = csrfToken;
              }
            }
          }
          
          // Replace the options in the args array
          argArray[1] = options;
        }
      }
      
      // Call the original fetch with our modified args
      return Reflect.apply(target, thisArg, argArray);
    }
  });
});