<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

// Check auth on mount
onMounted(async () => {
  // Skip on login and auth callback pages
  if (route.path === '/login' || route.path === '/auth/callback') {
    return
  }
  
  try {
    await authStore.checkAuth()
    // If on root and authenticated, redirect to management
    if (route.path === '/' && authStore.isAuthenticated && authStore.isAdmin) {
      router.push('/management')
    }
  } catch (error) {
    // Will be handled by middleware
  }
})
</script>