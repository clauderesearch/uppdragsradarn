import { defineNuxtPlugin } from '#app'
import { useRuntimeConfig, useNuxtApp } from '#app/nuxt'
import { useAuthStore } from '~/stores/auth'

// Import generated API client
import { getAllAssignments1 } from '../api-client'
import { createClient } from '@hey-api/client-fetch'

export default defineNuxtPlugin((nuxtApp) => {
  const runtimeConfig = useRuntimeConfig()
  const authStore = useAuthStore()

  // Create API client with authentication
  const client = createClient({
    baseUrl: runtimeConfig.public.apiBaseUrl || '',
    headers: () => {
      const headers: Record<string, string> = {}
      
      // Add authentication token if available
      if (authStore.isAuthenticated && authStore.token) {
        headers.Authorization = `Bearer ${authStore.token}`
      }
      
      // Add CSRF token if available
      const csrfToken = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1]
        
      if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken
      }
      
      return headers
    }
  })

  // Create API methods
  const api = {
    assignments: {
      getAll: () => getAllAssignments1({ client })
    },
    // Add admin-specific API methods as needed
  }

  // Add to Nuxt context
  nuxtApp.provide('api', api)
})

// Declare the module augmentation for TypeScript
declare module '#app' {
  interface NuxtApp {
    $api: {
      assignments: {
        getAll: () => ReturnType<typeof getAllAssignments1>
      }
      // Add admin-specific API type definitions as needed
    }
  }
}

// Access via composable
export const useApi = () => {
  const nuxtApp = useNuxtApp()
  return nuxtApp.$api
}