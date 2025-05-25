// This plugin ensures CSRF token is available for API requests via the session
export default defineNuxtPlugin(() => {
  // Define a composable for CSRF state
  const csrfToken = useState<string | null>('csrf-token', () => null)
  
  // Function to fetch session info including CSRF token
  const fetchSessionInfo = async () => {
    const config = useRuntimeConfig()
    const apiBase = config.public.apiBase
    
    try {
      const response = await $fetch(`${apiBase}/session`, {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Accept': 'application/json'
        }
      })
      
      if (response && response.csrfToken) {
        // Store CSRF token in state for use in requests
        csrfToken.value = response.csrfToken
      } else {
        console.warn('No CSRF token found in session response')
      }
    } catch (error) {
      console.error('Failed to fetch session info:', error)
    }
  }
  
  // Fetch CSRF token on app initialization
  fetchSessionInfo()
  
  // Expose the CSRF token to the rest of the app
  return {
    provide: {
      csrf: {
        getToken: () => csrfToken.value,
        refreshToken: fetchSessionInfo
      }
    }
  }
})