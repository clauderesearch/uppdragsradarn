<template>
  <div class="api-client-example">
    <h2 class="text-xl font-bold mb-4">Assignments</h2>
    
    <!-- Loading state -->
    <div v-if="loading" class="p-4 border rounded bg-gray-50">
      <p>Loading assignments...</p>
    </div>
    
    <!-- Error state -->
    <div v-else-if="error" class="p-4 border rounded bg-red-50 text-red-800">
      <p>Error loading assignments: {{ error }}</p>
      <button @click="loadAssignments" class="mt-2 px-3 py-1 bg-red-100 rounded">
        Try Again
      </button>
    </div>
    
    <!-- Success state -->
    <div v-else>
      <div class="mb-4 flex justify-between items-center">
        <div>
          <span class="text-sm text-gray-600">Found {{ totalItems }} assignments</span>
        </div>
        <div>
          <label class="mr-2 text-sm">Search:</label>
          <input 
            v-model="searchKeyword" 
            @input="debounceSearch"
            class="px-2 py-1 border rounded"
            placeholder="Enter keywords..."
          />
        </div>
      </div>
      
      <!-- Assignment list -->
      <ul v-if="assignments.length > 0" class="space-y-2">
        <li 
          v-for="assignment in assignments" 
          :key="assignment.id"
          class="p-4 border rounded hover:bg-gray-50 transition-colors"
        >
          <h3 class="font-medium">{{ assignment.title }}</h3>
          <p class="text-sm text-gray-600">{{ assignment.company }}</p>
          <div class="mt-2 flex justify-between">
            <span class="text-xs bg-blue-100 px-2 py-1 rounded">
              {{ assignment.location }}
            </span>
            <button 
              @click="viewAssignment(assignment.id)"
              class="text-xs text-blue-600 hover:underline"
            >
              View Details
            </button>
          </div>
        </li>
      </ul>
      
      <!-- Empty state -->
      <div v-else class="p-4 border rounded bg-gray-50 text-center">
        <p>No assignments found matching your criteria.</p>
      </div>
      
      <!-- Pagination -->
      <div v-if="assignments.length > 0" class="mt-4 flex justify-center space-x-2">
        <button 
          @click="loadPage(currentPage - 1)" 
          :disabled="currentPage === 0"
          class="px-3 py-1 border rounded disabled:opacity-50"
        >
          Previous
        </button>
        <span class="px-3 py-1">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button 
          @click="loadPage(currentPage + 1)" 
          :disabled="currentPage >= totalPages - 1"
          class="px-3 py-1 border rounded disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useApi } from '~/plugins/api-client'

// State
const assignments = ref<any[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(0)
const pageSize = ref(10)
const totalItems = ref(0)
const totalPages = ref(0)
const searchKeyword = ref('')
const searchTimeout = ref<NodeJS.Timeout | null>(null)

// Get API client
const api = useApi()

// Methods
const loadAssignments = async () => {
  loading.value = true
  error.value = null
  
  try {
    // If we have a search keyword, use search endpoint
    if (searchKeyword.value.trim()) {
      const response = await api.assignments.searchByKeyword(
        searchKeyword.value,
        currentPage.value,
        pageSize.value
      )
      
      // Update state with response data
      assignments.value = response.content || []
      totalItems.value = response.totalElements || 0
      totalPages.value = response.totalPages || 0
      currentPage.value = response.number || 0
    } else {
      // Otherwise load all assignments
      const response = await api.assignments.getAllAssignments(
        null,  // No search keyword
        currentPage.value,
        pageSize.value
      )
      
      // Update state with response data
      assignments.value = response.content || []
      totalItems.value = response.totalElements || 0
      totalPages.value = response.totalPages || 0
      currentPage.value = response.number || 0
    }
  } catch (err: any) {
    console.error('Error loading assignments:', err)
    error.value = err.message || 'Failed to load assignments'
    assignments.value = []
  } finally {
    loading.value = false
  }
}

// Load a specific page
const loadPage = (page: number) => {
  currentPage.value = page
  loadAssignments()
}

// View assignment details
const viewAssignment = (id: string) => {
  navigateTo(`/assignments/${id}`)
}

// Debounce search input to avoid too many API calls
const debounceSearch = () => {
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }
  
  searchTimeout.value = setTimeout(() => {
    currentPage.value = 0 // Reset to first page when searching
    loadAssignments()
  }, 500)
}

// Load assignments on component mount
onMounted(() => {
  loadAssignments()
})
</script>