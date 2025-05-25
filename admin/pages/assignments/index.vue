<template>
  <div class="container mx-auto p-6">
    <h1 class="text-3xl font-bold mb-6 text-gray-900 dark:text-white">All Assignments</h1>
    
    <div v-if="assignmentStore.isLoading" class="flex justify-center p-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
    </div>
    
    <div v-else-if="assignmentStore.error" class="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded mb-4">
      {{ assignmentStore.error }}
    </div>
    
    <div v-else-if="assignmentStore.assignments.length === 0" class="text-gray-500 dark:text-gray-400 text-center py-12">
      No assignments found.
    </div>
    
    <div v-else class="grid gap-4">
      <div 
        v-for="assignment in assignmentStore.assignments" 
        :key="assignment.id"
        class="bg-white dark:bg-gray-800 shadow rounded-lg p-6 hover:shadow-lg transition-shadow"
      >
        <div class="flex justify-between items-start mb-4">
          <div>
            <h2 class="text-xl font-semibold text-gray-900 dark:text-white">{{ assignment.title }}</h2>
            <p class="text-gray-600 dark:text-gray-400">{{ assignment.companyName }}</p>
          </div>
          <div class="flex items-center space-x-2">
            <span
              v-if="assignment.active"
              class="px-3 py-1 text-sm font-medium bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 rounded-full"
            >
              Active
            </span>
            <span
              v-else
              class="px-3 py-1 text-sm font-medium bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200 rounded-full"
            >
              Inactive
            </span>
            <span 
              v-if="assignment.needsManualReview" 
              class="px-3 py-1 text-sm font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200 rounded-full"
            >
              Needs Review
            </span>
          </div>
        </div>
        
        <p class="text-gray-700 dark:text-gray-300 mb-4">{{ assignment.description }}</p>
        
        <div class="flex items-center justify-between text-sm">
          <div class="text-gray-500 dark:text-gray-400">
            <span class="mr-4">📍 {{ assignment.location }}</span>
            <span>🏷️ {{ assignment.source.name }}</span>
          </div>
          <div class="space-x-2">
            <button 
              v-if="assignment.needsManualReview"
              @click="handleApprove(assignment.id)"
              class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition-colors"
            >
              Approve
            </button>
            <NuxtLink 
              :to="`/assignments/${assignment.id}/edit`"
              class="inline-block px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              Edit
            </NuxtLink>
          </div>
        </div>
        
        <div v-if="assignment.piiDetected" class="mt-4 p-3 bg-red-50 dark:bg-red-900 rounded border border-red-200 dark:border-red-700">
          <p class="text-sm text-red-700 dark:text-red-200 font-medium">PII Detected:</p>
          <p class="text-sm text-red-600 dark:text-red-300">{{ assignment.piiDetected }}</p>
        </div>
      </div>
    </div>
    
    <div v-if="assignmentStore.totalPagesAll > 1" class="mt-6 flex justify-center">
      <nav class="flex space-x-2">
        <button 
          v-for="page in assignmentStore.totalPagesAll" 
          :key="page"
          @click="fetchPage(page - 1)"
          :class="[
            'px-4 py-2 rounded',
            assignmentStore.currentPageAll === page - 1 
              ? 'bg-blue-600 text-white' 
              : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
          ]"
        >
          {{ page }}
        </button>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAssignmentStore } from '~/stores/assignment'

const assignmentStore = useAssignmentStore()

const fetchPage = async (page: number) => {
  await assignmentStore.fetchAllAssignments(page)
}

const handleApprove = async (id: string) => {
  if (confirm('Are you sure you want to approve this assignment?')) {
    try {
      await assignmentStore.approveAssignment(id)
      await fetchPage(assignmentStore.currentPageAll)
    } catch (error) {
      console.error('Failed to approve assignment:', error)
    }
  }
}

onMounted(async () => {
  await fetchPage(0)
})

// Set up middleware to require auth
definePageMeta({
  middleware: 'auth'
})
</script>