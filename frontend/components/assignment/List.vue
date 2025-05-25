<template>
  <div>
    <div v-if="loading" class="flex justify-center items-center h-40">
      <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
    </div>
    
    <div v-else-if="assignments.length === 0" class="bg-white rounded-lg shadow p-8 text-center">
      <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-gray-100">
        <InformationCircleIcon class="h-6 w-6 text-gray-600" />
      </div>
      <h3 class="mt-2 text-lg font-medium text-gray-900">No assignments found</h3>
      <p class="mt-1 text-sm text-gray-500">
        Try adjusting your search filters or check back later for new assignments.
      </p>
      <div class="mt-6">
        <button @click="clearFilters" class="btn btn-primary">
          Clear Filters
        </button>
      </div>
    </div>
    
    <div v-else class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
      <AssignmentCard 
        v-for="assignment in assignments" 
        :key="assignment.id" 
        :assignment="assignment" 
        :is-new="isNewAssignment(assignment)"
      />
    </div>
    
    <div v-if="assignments.length > 0 && hasMorePages" class="mt-8 flex justify-center">
      <button 
        @click="loadMore" 
        class="btn btn-secondary px-6 py-2"
        :disabled="loadingMore"
      >
        <span v-if="loadingMore">Loading...</span>
        <span v-else>Load More</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {InformationCircleIcon} from '@heroicons/vue/24/outline'
import {useAssignmentStore} from '~/stores/assignment'
import {useSearchStore} from '~/stores/search'
import dayjs from 'dayjs'

const assignmentStore = useAssignmentStore()
const searchStore = useSearchStore()

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  assignments: {
    type: Array,
    default: () => []
  },
  hasMorePages: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['loadMore', 'clearFilters'])

const loadingMore = ref(false)

function loadMore() {
  loadingMore.value = true
  emit('loadMore')
  setTimeout(() => {
    loadingMore.value = false
  }, 500)
}

function clearFilters() {
  emit('clearFilters')
}

// Assignments created in the last 24 hours are considered new
function isNewAssignment(assignment: any) {
  if (!assignment.createdAt) return false
  const createdAt = dayjs(assignment.createdAt)
  const now = dayjs()
  return now.diff(createdAt, 'hour') < 24
}
</script>