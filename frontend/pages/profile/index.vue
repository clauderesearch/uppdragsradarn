<template>
  <div class="flex flex-col min-h-screen overflow-hidden">
    <div class="container max-w-5xl mx-auto px-4 pt-8 pb-16 flex-grow">
      <!-- Header -->
      <div class="mb-10 text-center">
        <h1 class="text-2xl font-bold mb-4 text-gray-900 dark:text-gray-100">{{ $t('common.your_profile') }}</h1>
      </div>

      <div v-if="loading" class="flex justify-center my-8">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>

      <div v-if="successMessage" class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded-lg mb-4 relative max-w-3xl mx-auto">
        <strong class="font-bold">Success:</strong>
        <span class="block sm:inline"> {{ successMessage }}</span>
      </div>

      <div v-if="error" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-4 relative max-w-3xl mx-auto">
        <strong class="font-bold">{{ $t('common.error') }}:</strong>
        <span class="block sm:inline"> {{ error }}</span>
      </div>

      <div v-else-if="authStore.user" class="bg-white dark:bg-primary-950 rounded-lg shadow-sm p-6 max-w-3xl mx-auto">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-gray-100">{{ $t('profile.personal_information') }}</h2>
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.email') }}</label>
                <div class="mt-1 p-2 bg-gray-50 dark:bg-primary-900 rounded-md dark:text-gray-300">{{ authStore.user.email }}</div>
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.first_name') }}</label>
                <input
                  v-model="formData.firstName"
                  type="text"
                  :aria-label="$t('profile.first_name')"
                  class="mt-1 block w-full px-5 py-3 rounded-full border-gray-300 dark:border-gray-700 shadow-sm focus:border-primary-500 focus:ring-primary-500 focus:outline-none bg-white dark:bg-primary-950 text-gray-700 dark:text-gray-300"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.last_name') }}</label>
                <input
                  v-model="formData.lastName"
                  type="text"
                  :aria-label="$t('profile.last_name')"
                  class="mt-1 block w-full px-5 py-3 rounded-full border-gray-300 dark:border-gray-700 shadow-sm focus:border-primary-500 focus:ring-primary-500 focus:outline-none bg-white dark:bg-primary-950 text-gray-700 dark:text-gray-300"
                />
              </div>
            </div>
          </div>

          <div>
            <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-gray-100">{{ $t('profile.preferences') }}</h2>
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.subscription_tier') }}</label>
                <div class="mt-1 p-2 bg-gray-50 dark:bg-primary-900 rounded-md dark:text-gray-300">
                  {{ authStore.user.subscriptionTier || $t('profile.free_tier') }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-6 flex justify-center space-x-4">
          <NuxtLink
            to="/"
            class="px-6 py-2 rounded-full border border-primary-600 dark:border-primary-400 bg-white dark:bg-primary-950 text-primary-600 dark:text-primary-400 shadow-sm hover:bg-gray-50 dark:hover:bg-primary-900 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
          >
            <span class="mr-1">←</span> {{ $t('common.back_to_search') }}
          </NuxtLink>
          <button
            @click="updateProfile"
            class="px-6 py-2 rounded-full border border-transparent bg-primary-600 dark:bg-primary-500 text-white shadow-sm hover:bg-primary-700 dark:hover:bg-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
            :disabled="updating"
          >
            {{ updating ? $t('common.loading') : $t('profile.save_changes') }}
          </button>
        </div>
      </div>

      <div v-else class="bg-white dark:bg-primary-950 rounded-lg shadow-sm p-6 text-center max-w-3xl mx-auto">
        <p class="text-gray-500 dark:text-gray-400">{{ $t('profile.login_required') }}</p>
        <button
          @click="authStore.login()"
          class="mt-4 px-6 py-2 rounded-full border border-transparent bg-primary-600 dark:bg-primary-500 text-white shadow-sm hover:bg-primary-700 dark:hover:bg-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
        >
          {{ $t('common.sign_in') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {useAuthStore} from '~/stores/auth'
import useTheme from '~/composables/useTheme'

definePageMeta({
  layout: 'default',
})

const authStore = useAuthStore()
const { isDark } = useTheme()
const loading = ref(false)
const updating = ref(false)
const error = ref('')
const successMessage = ref('')

const formData = reactive({
  firstName: '',
  lastName: '',
  notificationEmailEnabled: false
})

onMounted(async () => {
  // Load user data
  loading.value = true
  try {
    await authStore.checkSession()
    
    // Populate form with user data
    if (authStore.user) {
      formData.firstName = authStore.user.given_name || ''
      formData.lastName = authStore.user.family_name || ''
      formData.notificationEmailEnabled = authStore.user.notificationEmailEnabled || false
    }
  } catch (err: any) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})

const updateProfile = async () => {
  updating.value = true
  successMessage.value = ''
  error.value = ''
  try {
    const success = await authStore.updateUserProfile({
      firstName: formData.firstName,
      lastName: formData.lastName,
      notificationEmailEnabled: formData.notificationEmailEnabled
    })

    if (success) {
      // Show success notification
      successMessage.value = $t('profile.update_success')
      // Clear success message after 3 seconds
      setTimeout(() => {
        successMessage.value = ''
      }, 3000)
    } else {
      error.value = $t('profile.update_failed')
    }
  } catch (err: any) {
    error.value = err.message
  } finally {
    updating.value = false
  }
}
</script>