import { defineStore } from 'pinia'
import {useRuntimeConfig} from "nuxt/app";

interface User {
  id: string
  email: string
  subscriptionTier: string
  roles?: string[]
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    isAuthenticated: false,
    isLoading: false,
    error: null as string | null
  }),

  getters: {
    isAdmin: (state) => state.user?.roles?.includes('ADMIN') || false
  },

  actions: {
    async checkAuth() {
      this.isLoading = true
      this.error = null
      
      try {
        // Check if user is authenticated with session endpoint
        const config = useRuntimeConfig()
        const apiBase = config.public.apiBase
        const sessionResponse = await fetch(`${apiBase}/session`, {
          method: 'GET',
          credentials: 'include',
          headers: {
            'Accept': 'application/json'
          }
        })
        
        if (!sessionResponse.ok) {
          throw new Error('Failed to check session')
        }
        
        const sessionData = await sessionResponse.json()
        
        if (sessionData && sessionData.authenticated && sessionData.user) {
          this.user = sessionData.user
          this.isAuthenticated = true
          
          // Check if user has admin role
          if (!sessionData.user.roles?.includes('ADMIN')) {
            throw new Error('Unauthorized: Admin access required')
          }
          
          return true
        } else {
          this.isAuthenticated = false
          this.user = null
          return false
        }
      } catch (error: any) {
        this.error = error.message || 'Failed to check authentication'
        this.isAuthenticated = false
        this.user = null
        throw error
      } finally {
        this.isLoading = false
      }
    },

    async login(username: string, password: string) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await fetch(`${useRuntimeConfig().public.apiBase}/public/auth/login`, {
          method: 'POST',
          credentials: 'include',
          body: JSON.stringify({
            username,
            password
          })
        })
        
        if (!response.ok) {
          throw new Error('Login failed')
        }
        
        const data = await response.json()
        
        if (data && data.user) {
          // Check session to get the full user data with roles
          const result = await this.checkAuth()
          
          if (!result) {
            throw new Error('Failed to authenticate after login')
          }
          
          return data
        } else {
          throw new Error('Invalid login response')
        }
      } catch (error: any) {
        this.error = error.message || 'Login failed'
        this.isAuthenticated = false
        this.user = null
        throw error
      } finally {
        this.isLoading = false
      }
    },

    async logout() {
      this.isLoading = true
      
      try {
        await fetch(`${useRuntimeConfig().public.apiBase}/logout`, {
          method: 'POST',
          credentials: 'include'
        })
      } catch (error: any) {
        console.error('Logout error:', error)
      } finally {
        this.user = null
        this.isAuthenticated = false
        this.isLoading = false
      }
    }
  }
})