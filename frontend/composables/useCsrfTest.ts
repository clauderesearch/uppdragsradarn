// Helper composable for testing CSRF token usage
export const useCsrfTest = () => {
  const nuxtApp = useNuxtApp()
  
  // Function to test if CSRF token is present and can be retrieved
  const testCsrfToken = () => {
    const token = nuxtApp.$csrf?.getToken()
    return {
      success: token !== null && token !== undefined,
      token
    }
  }
  
  // Function to test making a POST request with CSRF token
  const testCsrfRequest = async () => {
    const config = useRuntimeConfig()
    const apiBase = config.public.apiBase
    
    try {
      // Try to make a simple POST request that should include the CSRF token
      // Just use a lightweight endpoint for testing
      await $fetch(`${apiBase}/api/session`, {
        method: 'GET',
        credentials: 'include'
      })
      
      return {
        success: true,
        message: 'Request completed successfully'
      }
    } catch (error: any) {
      return {
        success: false,
        message: error.message || 'Request failed with unknown error',
        error
      }
    }
  }
  
  return {
    testCsrfToken,
    testCsrfRequest
  }
}