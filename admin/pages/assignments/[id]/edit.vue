<template>
  <div>
    <div class="mb-8">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white">
        Edit Assignment
      </h2>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        Review and edit assignment content
      </p>
    </div>

    <div v-if="loading" class="text-center py-4">
      Loading assignment...
    </div>

    <div v-else-if="error" class="rounded-md bg-red-50 dark:bg-red-900 p-4">
      <p class="text-sm text-red-800 dark:text-red-200">{{ error }}</p>
    </div>

    <form v-else @submit.prevent="saveAssignment" class="space-y-6">
      <div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
        <div class="space-y-6">
          <div>
            <label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Title
            </label>
            <input
              id="title"
              v-model="assignment.title"
              type="text"
              class="mt-1 block w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-green-500 focus:border-green-500 sm:text-sm dark:bg-gray-700 dark:text-white"
            >
          </div>

          <div>
            <label for="company" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Company
            </label>
            <input
              id="company"
              v-model="assignment.companyName"
              type="text"
              class="mt-1 block w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-green-500 focus:border-green-500 sm:text-sm dark:bg-gray-700 dark:text-white"
            >
          </div>

          <div>
            <label for="location" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Location
            </label>
            <input
              id="location"
              v-model="assignment.location"
              type="text"
              class="mt-1 block w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-green-500 focus:border-green-500 sm:text-sm dark:bg-gray-700 dark:text-white"
            >
          </div>

          <div>
            <label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Description
            </label>
            <textarea
              id="description"
              v-model="assignment.description"
              rows="15"
              class="mt-1 block w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:ring-green-500 focus:border-green-500 sm:text-sm dark:bg-gray-700 dark:text-white font-mono text-xs"
            ></textarea>
          </div>

          <div v-if="assignment.piiDetected" class="rounded-md bg-yellow-50 dark:bg-yellow-900 p-4">
            <div class="flex">
              <div class="flex-shrink-0">
                <svg class="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
              </div>
              <div class="ml-3">
                <h3 class="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                  PII Detected
                </h3>
                <p class="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
                  {{ assignment.piiDetected }}
                </p>
              </div>
            </div>
          </div>

          <div class="flex items-center">
            <input
              id="needsReview"
              v-model="assignment.needsManualReview"
              type="checkbox"
              class="h-4 w-4 text-green-600 focus:ring-green-500 border-gray-300 rounded"
            >
            <label for="needsReview" class="ml-2 block text-sm text-gray-700 dark:text-gray-300">
              Needs manual review
            </label>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button
          type="button"
          @click="$router.push('/')"
          class="bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md py-2 px-4 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500"
        >
          Cancel
        </button>
        <button
          type="submit"
          :disabled="saving"
          class="bg-green-600 text-white rounded-md py-2 px-4 text-sm font-medium hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50"
        >
          {{ saving ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const router = useRouter()
const assignmentStore = useAssignmentStore()

const assignment = ref<any>({})
const loading = ref(true)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    const { $fetch } = useNuxtApp()
    const response = await $fetch(`/api/v1/assignments/${route.params.id}`, {
      credentials: 'include'
    })
    assignment.value = response
  } catch (err: any) {
    error.value = err.message || 'Failed to load assignment'
  } finally {
    loading.value = false
  }
})

const saveAssignment = async () => {
  saving.value = true
  error.value = ''
  
  try {
    await assignmentStore.updateAssignment(assignment.value.id, {
      title: assignment.value.title,
      companyName: assignment.value.companyName,
      location: assignment.value.location,
      description: assignment.value.description,
      needsManualReview: assignment.value.needsManualReview
    })
    
    router.push('/')
  } catch (err: any) {
    error.value = err.message || 'Failed to save assignment'
  } finally {
    saving.value = false
  }
}

// Set up middleware to require auth
definePageMeta({
  middleware: 'auth'
})
</script>