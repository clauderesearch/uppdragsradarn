import {defineStore} from 'pinia'

interface User {
  id?: string
  email: string
  given_name?: string
  family_name?: string
  name?: string
  notificationEmailEnabled?: boolean
  subscriptionTier?: string
  roles?: string[]
}

interface AuthState {
  user: User | null
  authenticated: boolean
  loading: boolean
  error: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    authenticated: false,
    loading: false,
    error: null
  }),

  getters: {
    isAuthenticated: (state) => state.authenticated,
    getUser: (state) => state.user,
    isAdmin: (state) => state.user?.roles?.includes('ADMIN') || false
  },

  actions: {
    async init() {
      // Check session on app initialization
      await this.checkSession()
    },
    
    async checkSession() {
      try {
        this.loading = true

        // Use credentials: 'include' to include cookies in the request
        const apiBase = useRuntimeConfig().public.apiBase
        const response = await fetch(`${apiBase}/session`, {
          credentials: 'include',
          headers: {
            'Accept': 'application/json'
          }
        })

        if (!response.ok) {
          this.authenticated = false
          this.user = null
          return false
        }

        const data = await response.json()

        this.authenticated = data.authenticated

        if (data.authenticated && data.user) {
          this.user = data.user
          return true
        } else {
          this.user = null
          return false
        }
      } catch (error: any) {
        console.error('Session check error:', error)
        this.error = error.message
        this.authenticated = false
        this.user = null
        return false
      } finally {
        this.loading = false
      }
    },
    
    async updateUserProfile(userData: { firstName?: string, lastName?: string, notificationEmailEnabled?: boolean }) {
      if (!this.authenticated || !this.user?.id) return

      try {
        this.loading = true

        const response = await fetch(`${useRuntimeConfig().public.apiBase}/users/${this.user.id}`, {
          method: 'PUT',
          credentials: 'include',
          body: JSON.stringify(userData)
        })

        if (!response.ok) {
          throw new Error('Failed to update user profile')
        }

        // Update local user object with the response
        const updatedUser = await response.json()

        // Merge with existing user data
        this.user = {
          ...this.user,
          ...updatedUser
        }

        return true
      } catch (error: any) {
        this.error = error.message
        return false
      } finally {
        this.loading = false
      }
    },

    async login() {
      // Only execute in client-side environment
      if (!process.client) return
      
      // Construct the OAuth URL correctly 
      const apiBase = useRuntimeConfig().public.apiBase.replace(/\/api$/, '')
      const loginUrl = `${apiBase}/oauth2/authorization/cognito`
      window.location.href = loginUrl
    },

    async handleAuthCallback() {
      try {
        this.loading = true
        this.error = null
        
        // After OAuth callback, just check the session to see if we're logged in
        const authenticated = await this.checkSession()
        return authenticated
      } catch (error: any) {
        this.error = error.message
        return false
      } finally {
        this.loading = false
      }
    },

    async logout() {
      if (!process.client) return
      
      try {
        // Use the Spring Security logout endpoint with POST method
        const apiBase = useRuntimeConfig().public.apiBase
        await $fetch(`${apiBase}/logout`, {
          method: 'POST',
          credentials: 'include'
        })
        
        // After successful logout, redirect to home
        window.location.href = '/'
      } catch (error) {
        console.error('Logout error:', error)
        // Even if logout fails, clear local state and redirect
        window.location.href = '/'
      } finally {
        // Clear local state
        this.authenticated = false
        this.user = null
      }
    }
  }
})