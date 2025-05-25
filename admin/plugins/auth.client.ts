// Admin authentication plugin
export default defineNuxtPlugin(async (nuxtApp) => {
  const authStore = useAuthStore()
  const route = useRoute()
  
  // Skip auth check on login and callback pages
  if (route.path === '/login' || route.path === '/auth/callback') {
    return
  }
  
  // Check if user is authenticated on app startup
  try {
    await authStore.checkAuth()
    
    // If authenticated and admin, redirect root to management
    if (route.path === '/' && authStore.isAuthenticated && authStore.isAdmin) {
      await navigateTo('/management')
    }
  } catch (error) {
    // If not authenticated, the middleware will handle redirects
    console.error('Auth check failed:', error)
    
    // Only redirect if we're not already on the login page
    if (route.path !== '/login') {
      await navigateTo('/login')
    }
  }
})