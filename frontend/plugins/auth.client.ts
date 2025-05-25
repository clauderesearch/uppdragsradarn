import {useAuthStore} from '~/stores/auth'

export default defineNuxtPlugin(async (nuxtApp) => {
  // Only initialize auth store on client-side
  if (process.client) {
    const authStore = useAuthStore()
    
    // Initialize authentication state
    // This will also fetch the CSRF token as part of the session data
    await authStore.init()
  }
})