<template>
  <div>
    <div class="mb-8">
      <h2 class="text-2xl font-bold text-gray-900 dark:text-white">
        Assignments Pending Review
      </h2>
      <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
        Assignments flagged for PII or requiring manual approval
      </p>
    </div>

    <!-- Loading state -->
    <div v-if="assignmentStore.isLoading" class="text-center py-4">
      <div class="inline-flex items-center">
        <svg class="animate-spin h-5 w-5 mr-3 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        Loading assignments...
      </div>
    </div>

    <!-- Error state -->
    <div v-else-if="assignmentStore.error" class="rounded-md bg-red-50 dark:bg-red-900 p-4">
      <p class="text-sm text-red-800 dark:text-red-200">{{ assignmentStore.error }}</p>
    </div>

    <!-- Empty state -->
    <div v-else-if="!assignmentStore.pendingReview.length" class="text-center py-12">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <h3 class="mt-2 text-sm font-medium text-gray-900 dark:text-white">No pending assignments</h3>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">All assignments have been reviewed.</p>
    </div>

    <!-- Assignment list -->
    <div v-else class="space-y-4">
      <div
        v-for="assignment in assignmentStore.pendingReview"
        :key="assignment.id"
        class="bg-white dark:bg-gray-800 shadow rounded-lg overflow-hidden"
      >
        <div class="p-6">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white">
                {{ assignment.title }}
              </h3>
              <p class="mt-1 text-sm text-gray-600 dark:text-gray-400">
                {{ assignment.companyName }} • {{ assignment.location }}
              </p>
              <div v-if="assignment.piiDetected" class="mt-2">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200">
                  PII Detected
                </span>
                <p class="mt-1 text-sm text-red-600 dark:text-red-400">
                  {{ assignment.piiDetected }}
                </p>
              </div>
            </div>
            <span class="ml-4 text-sm text-gray-500 dark:text-gray-400">
              {{ formatDate(assignment.createdAt) }}
            </span>
          </div>

          <div class="mt-4">
            <div class="prose prose-sm max-w-none dark:prose-dark">
              <div v-html="assignment.description" class="line-clamp-3"></div>
            </div>
          </div>

          <div class="mt-6 flex space-x-3">
            <button
              @click="editAssignment(assignment)"
              class="flex-1 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md py-2 px-4 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              Edit
            </button>
            <button
              @click="approveAssignment(assignment.id)"
              class="flex-1 bg-green-600 text-white rounded-md py-2 px-4 text-sm font-medium hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              Approve
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="assignmentStore.totalPages > 1" class="mt-8 flex justify-center">
      <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
        <button
          @click="previousPage"
          :disabled="assignmentStore.currentPage === 0"
          class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          Previous
        </button>
        <span class="relative inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300">
          Page {{ assignmentStore.currentPage + 1 }} of {{ assignmentStore.totalPages }}
        </span>
        <button
          @click="nextPage"
          :disabled="assignmentStore.currentPage === assignmentStore.totalPages - 1"
          class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          Next
        </button>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
const assignmentStore = useAssignmentStore()
const router = useRouter()

// Load pending assignments on mount
onMounted(() => {
  assignmentStore.fetchPendingReview()
})

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const editAssignment = (assignment: any) => {
  router.push(`/assignments/${assignment.id}/edit`)
}

const approveAssignment = async (id: string) => {
  if (confirm('Are you sure you want to approve this assignment?')) {
    try {
      await assignmentStore.approveAssignment(id)
    } catch (error) {
      console.error('Error approving assignment:', error)
    }
  }
}

const previousPage = () => {
  if (assignmentStore.currentPage > 0) {
    assignmentStore.fetchPendingReview(assignmentStore.currentPage - 1)
  }
}

const nextPage = () => {
  if (assignmentStore.currentPage < assignmentStore.totalPages - 1) {
    assignmentStore.fetchPendingReview(assignmentStore.currentPage + 1)
  }
}

// Set up middleware to require auth
definePageMeta({
  middleware: 'auth'
})
</script>
