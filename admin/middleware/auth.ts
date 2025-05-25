export default defineNuxtRouteMiddleware((to, from) => {
  const authStore = useAuthStore()
  
  // Allow access to login page and auth callback
  if (to.path === '/login' || to.path === '/auth/callback' || to.path === '/') {
    return
  }
  
  // Check if user is authenticated and is admin
  if (!authStore.isAuthenticated || !authStore.isAdmin) {
    return navigateTo('/login')
  }
})
