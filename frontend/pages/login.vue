<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div>
        <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
          Sign in to your account
        </h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          Or
          <a href="#" class="font-medium text-primary-600 hover:text-primary-500">
            contact us to get started
          </a>
        </p>
      </div>
      
      <div class="mt-8 bg-white py-8 px-4 shadow-md rounded-lg">
        <div class="flex flex-col items-center">
          <button 
            @click="login" 
            class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none"
          >
            Sign in with AWS Cognito
          </button>
          
          <div class="mt-6 text-center">
            <p class="text-sm text-gray-600">
              By signing in, you agree to our
              <a href="#" class="font-medium text-primary-600 hover:text-primary-500">
                Terms of Service
              </a>
              and
              <a href="#" class="font-medium text-primary-600 hover:text-primary-500">
                Privacy Policy
              </a>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted} from 'vue'
import {useAuthStore} from '~/stores/auth'
import {useRouter} from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

// Check if already authenticated
onMounted(() => {
  if (process.client && authStore.isAuthenticated) {
    router.push('/dashboard')
  }
})

function login() {
  // The login() method in the auth store now uses serverUrl
  authStore.login()
}
</script>