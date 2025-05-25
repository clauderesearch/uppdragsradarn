<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
    <div class="text-center">
      <div class="inline-flex items-center">
        <svg class="animate-spin h-8 w-8 text-green-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span class="ml-3 text-lg font-medium text-gray-900 dark:text-white">Verifying your credentials...</span>
      </div>
      <p v-if="error" class="mt-4 text-red-600 dark:text-red-400">{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const authStore = useAuthStore()
const router = useRouter()
const error = ref('')

onMounted(async () => {
  try {
    // Check authentication with the backend
    await authStore.checkAuth()
    
    // If successful and user is admin, redirect to management
    if (authStore.isAuthenticated && authStore.isAdmin) {
      router.push('/management')
    } else {
      error.value = 'You do not have admin access'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    }
  } catch (err: any) {
    error.value = err.message || 'Authentication failed'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
})
</script>