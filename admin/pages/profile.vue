<template>
  <div class="min-h-screen py-8">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
      <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-8">
        Admin Profile
      </h1>
      
      <div v-if="isLoading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
      </div>
      
      <div v-else-if="error" class="bg-red-50 dark:bg-red-900 p-4 rounded-lg">
        <p class="text-red-600 dark:text-red-300">{{ error }}</p>
      </div>
      
      <div v-else-if="userProfile" class="space-y-6">
        <!-- User Information Card -->
        <div class="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
          <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">
            User Information
          </h2>
          
          <dl class="grid grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2">
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                User ID
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.id }}
              </dd>
            </div>
            
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                Email
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.email }}
              </dd>
            </div>
            
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                First Name
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.firstName || 'Not set' }}
              </dd>
            </div>
            
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                Last Name
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.lastName || 'Not set' }}
              </dd>
            </div>
            
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                Subscription Tier
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.subscriptionTier }}
              </dd>
            </div>
            
            <div>
              <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">
                Email Notifications
              </dt>
              <dd class="mt-1 text-sm text-gray-900 dark:text-white">
                {{ userProfile.notificationEmailEnabled ? 'Enabled' : 'Disabled' }}
              </dd>
            </div>
          </dl>
        </div>
        
        <!-- Roles and Authorities Card -->
        <div class="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
          <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-4">
            Roles and Authorities
          </h2>
          
          <div v-if="userProfile.roles.length > 0">
            <ul class="space-y-2">
              <li
                v-for="role in userProfile.roles"
                :key="role"
                class="flex items-center"
              >
                <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800 dark:bg-primary-900 dark:text-primary-200">
                  {{ role }}
                </span>
              </li>
            </ul>
          </div>
          <div v-else>
            <p class="text-gray-500 dark:text-gray-400">No roles assigned</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface UserProfile {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  notificationEmailEnabled: boolean
  subscriptionTier: string
  roles: string[]
}

const userProfile = ref<UserProfile | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)

const fetchUserProfile = async () => {
  isLoading.value = true
  error.value = null
  
  try {
    const response = await fetch(`${useRuntimeConfig().public.apiBase}/admin/users/profile`, {
      credentials: 'include',
      headers: {
        'Accept': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error('Failed to fetch user profile')
    }
    
    userProfile.value = await response.json()
  } catch (err: any) {
    error.value = err.message || 'An error occurred'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchUserProfile()
})
</script>