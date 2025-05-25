import {useAuthStore} from '~/stores/auth'

export default defineNuxtRouteMiddleware(async (to, from) => {
  const authStore = useAuthStore()
  
  // If we're running on the client, we might need to check the session
  if (process.client && !authStore.isAuthenticated) {
    // Try to check the session
    await authStore.checkSession()
  }
  
  // Check if user is authenticated
  if (!authStore.isAuthenticated) {
    // Redirect to login page
    return navigateTo('/login')
  }
})