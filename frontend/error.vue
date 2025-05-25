<template>
  <NuxtLayout name="default">
    <div class="flex flex-col items-center justify-center px-4 py-16 flex-grow">
      <div class="w-full max-w-3xl mx-auto text-center">
        <div v-if="error.statusCode === 404" class="space-y-6">
          <h1 class="text-4xl font-bold text-gray-900 dark:text-white">
            {{ $t('error.404_title') || 'Page not found' }}
          </h1>

          <div class="text-gray-600 dark:text-gray-300 text-lg">
            <p>{{ $t('error.404_message') || 'Sorry, we couldn\'t find the page you\'re looking for.' }}</p>
          </div>

          <div class="flex justify-center mt-8">
            <NuxtLink
              to="/"
              class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              aria-label="Return to homepage"
            >
              {{ $t('error.go_home') || 'Go to Homepage' }}
            </NuxtLink>
          </div>
        </div>

        <div v-else class="space-y-6">
          <h1 class="text-4xl font-bold text-gray-900 dark:text-white">
            {{ $t('error.generic_title') || 'Something went wrong' }}
          </h1>

          <div class="text-gray-600 dark:text-gray-300 text-lg">
            <p>{{ $t('error.generic_message') || 'Sorry, an unexpected error has occurred.' }}</p>
            <p v-if="error.message" class="mt-2 p-3 bg-red-50 dark:bg-red-900/20 text-red-800 dark:text-red-300 rounded">
              {{ error.message }}
            </p>
          </div>

          <div class="flex justify-center mt-8">
            <NuxtLink
              to="/"
              class="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              aria-label="Return to homepage"
            >
              {{ $t('error.go_home') || 'Go to Homepage' }}
            </NuxtLink>
          </div>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<script setup>
defineProps({
  error: Object
})

const { t } = useI18n()
</script>