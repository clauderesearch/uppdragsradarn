<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Navigation -->
    <nav class="bg-white dark:bg-gray-800 shadow">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex">
            <div class="flex-shrink-0 flex items-center">
              <h1 class="text-xl font-bold text-gray-900 dark:text-white">
                Uppdragsradarn Admin
              </h1>
            </div>
            <div class="hidden sm:ml-6 sm:flex sm:space-x-8">
              <NuxtLink
                to="/management"
                :class="route.path === '/management' ? 'border-green-500 text-gray-900 dark:text-white' : 'border-transparent text-gray-500 dark:text-gray-300 hover:border-gray-300 hover:text-gray-700 dark:hover:text-white'"
                class="inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
              >
                Management
              </NuxtLink>
              <NuxtLink
                to="/assignments"
                :class="route.path.startsWith('/assignments') ? 'border-green-500 text-gray-900 dark:text-white' : 'border-transparent text-gray-500 dark:text-gray-300 hover:border-gray-300 hover:text-gray-700 dark:hover:text-white'"
                class="inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
              >
                All Assignments
              </NuxtLink>
              <NuxtLink
                to="/profile"
                :class="route.path === '/profile' ? 'border-green-500 text-gray-900 dark:text-white' : 'border-transparent text-gray-500 dark:text-gray-300 hover:border-gray-300 hover:text-gray-700 dark:hover:text-white'"
                class="inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
              >
                Profile
              </NuxtLink>
            </div>
          </div>
          <div class="flex items-center">
            <AppDarkModeToggle class="mr-4" />
            <span v-if="authStore.isAuthenticated" class="text-sm text-gray-500 dark:text-gray-300 mr-4">
              {{ userEmail }}
            </span>
            <button
              v-if="authStore.isAuthenticated"
              @click="handleLogout"
              class="text-sm text-gray-500 dark:text-gray-300 hover:text-gray-700 dark:hover:text-white"
            >
              Sign out
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Page Content -->
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import AppDarkModeToggle from '~/components/app/DarkModeToggle.vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const userEmail = computed(() => authStore.user?.email || '')

const handleLogout = async () => {
  await authStore.logout()
  router.push('/login')
}
</script>