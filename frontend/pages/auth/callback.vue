<template>
  <div class="min-h-screen flex items-center justify-center">
    <div class="bg-white p-8 rounded-lg shadow-md text-center max-w-md w-full">
      <div v-if="error" class="text-red-600 mb-4">
        {{ error }}
      </div>
      <div v-else>
        <div class="flex justify-center items-center mb-4">
          <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
        </div>
        <p class="text-gray-600">Completing authentication...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useAuthStore} from '~/stores/auth'
import {useRouter} from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()

const error = ref('')

onMounted(async () => {
  // Only process authentication on the client side
  if (!process.client) return
  
  try {
    // Check if we're authenticated after the OAuth2 callback
    const success = await authStore.handleAuthCallback()
    
    if (success) {
      // Redirect to homepage instead of dashboard
      await router.push('/')
    } else {
      error.value = 'Authentication failed. Please try again.'
    }
  } catch (err: any) {
    error.value = err.message || 'Authentication failed. Please try again.'
  }
})
</script>