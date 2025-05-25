<template>
  <div>
    <!-- Loading indicator -->
    <div v-if="loading" class="flex justify-center py-12" aria-live="polite">
      <div class="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-primary-600" role="status">
        <span class="sr-only">Loading assignments...</span>
      </div>
    </div>
    
    <!-- No results message -->
    <div
      v-else-if="assignments.length === 0 && showEmptyMessage"
      class="bg-white dark:bg-primary-950 rounded-lg shadow-sm p-8 text-center"
      role="status"
      aria-live="polite"
    >
      <p class="text-gray-700 dark:text-gray-300 mb-4">{{ emptyMessage }}</p>
      <slot name="empty-actions"></slot>
    </div>
    
    <!-- Results list - more compact spacing -->
    <div v-else class="space-y-2" role="feed" aria-label="Assignment search results">
      <div
        v-for="(assignment, index) in assignments"
        :key="assignment.id"
        class="bg-white dark:bg-primary-950 rounded-lg shadow-sm hover:shadow-md transition-shadow"
        :class="listItemClass"
      >
        <AssignmentExpandableCard
          :assignment="assignment"
          :is-new="isNewAssignment(assignment)"
          :is-expanded="expandedCardId === assignment.id"
          @toggle-expanded="toggleCard(assignment.id)"
        />
      </div>
      
      <!-- Load more button -->
      <slot name="load-more"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {useRoute} from 'vue-router'
import AssignmentExpandableCard from '~/components/assignment/ExpandableCard.vue'
import dayjs from 'dayjs'

const route = useRoute()
const expandedCardId = ref<string | null>(null)

const props = defineProps({
  assignments: {
    type: Array,
    required: true,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  showEmptyMessage: {
    type: Boolean,
    default: true
  },
  emptyMessage: {
    type: String,
    default: 'No results found'
  },
  listItemClass: {
    type: String,
    default: ''
  },
  showDescription: {
    type: Boolean,
    default: true
  },
  maxSkillsToShow: {
    type: Number,
    default: 5
  }
})

// Get the search query and construct a link that preserves it
function getAssignmentLink(id: string) {
  const currentQuery = route.query.q
  if (currentQuery) {
    return {
      path: `/assignments/${id}`,
      query: { from: currentQuery }
    }
  }
  return `/assignments/${id}`
}

// Assignments created in the last 24 hours are considered new
function isNewAssignment(assignment: any) {
  if (!assignment.createdAt) return false
  const createdAt = dayjs(assignment.createdAt)
  const now = dayjs()
  return now.diff(createdAt, 'hour') < 24
}

// Toggle expanded state of a card - ensure only one is expanded at a time
function toggleCard(id: string) {
  expandedCardId.value = expandedCardId.value === id ? null : id
}
</script>