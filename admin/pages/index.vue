<template>
  <div />
</template>

<script setup lang="ts">
const authStore = useAuthStore()
const router = useRouter()

// Check auth status on mount
onMounted(async () => {
  try {
    await authStore.checkAuth()
    if (authStore.isAuthenticated && authStore.isAdmin) {
      // Redirect authenticated admin users to management
      router.push('/management')
    } else {
      // Redirect to login
      router.push('/login')
    }
  } catch (error) {
    // Redirect to login on error
    router.push('/login')
  }
})
</script>
