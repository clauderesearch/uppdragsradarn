<template>
  <div class="min-h-screen flex flex-col">
    <div class="container max-w-5xl mx-auto px-4 pt-8 pb-16 flex-grow flex flex-col">
      <!-- Header and search bar -->
      <div class="mb-10 text-center">
        <NuxtLink to="/" class="inline-block mb-6">
          <AppLogo size="medium" />
        </NuxtLink>
        
        <form @submit.prevent="search" class="max-w-2xl mx-auto relative">
          <input 
            v-model="searchQuery" 
            type="text" 
            :placeholder="$t('search.placeholder')" 
            class="w-full px-5 py-3 border border-gray-300 dark:border-gray-700 rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 shadow-sm text-base bg-white dark:bg-primary-950 text-gray-900 dark:text-gray-100"
          />
          <button 
            type="submit"
            class="absolute right-2 top-1/2 transform -translate-y-1/2 bg-primary-600 dark:bg-primary-500 text-white px-4 py-1.5 rounded-full hover:bg-primary-700 dark:hover:bg-primary-400"
          >
            <MagnifyingGlassIcon class="h-5 w-5" />
          </button>
        </form>
      </div>
      
      <!-- Result count -->
      <div v-if="hasSearched" class="max-w-2xl mx-auto flex items-center justify-between text-sm mb-6">
        <div class="text-gray-600 dark:text-gray-400 font-medium">
          {{ $t('search.found_results', { count: totalItems }) }}
          {{ searchQuery ? $t('search.for_query', { query: searchQuery }) : '' }}
        </div>
        
        <div class="flex items-center space-x-3">
          <button 
            v-if="searchQuery" 
            @click="clearSearch" 
            class="text-primary-600 dark:text-primary-400 hover:text-primary-800 dark:hover:text-primary-300 flex items-center"
          >
            <XMarkIcon class="h-4 w-4 mr-1" />
            {{ $t('search.clear_search') }}
          </button>
        </div>
      </div>
    
      <div class="max-w-5xl mx-auto grid grid-cols-1 gap-8">
        
        <!-- Search results -->
        <div class="w-full">
          <AssignmentResultsList
            :assignments="assignments"
            :loading="loading"
            :show-empty-message="hasSearched"
            :empty-message="$t('search.no_matches')"
          >
            <!-- No results action slot -->
            <template #empty-actions>
              <button
                @click="resetFilters"
                class="px-4 py-2 bg-primary-600 dark:bg-primary-500 text-white rounded-lg text-sm hover:bg-primary-700 dark:hover:bg-primary-400"
              >
                {{ $t('search.clear_all_filters') }}
              </button>
            </template>

            <!-- Load more button slot -->
            <template #load-more>
              <div v-if="hasMorePages" class="flex justify-center py-6">
                <button
                  @click="loadMore"
                  class="px-6 py-2 border border-primary-300 dark:border-primary-700 bg-white dark:bg-primary-950 rounded-lg text-primary-600 dark:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900 shadow-sm"
                  :disabled="loadingMore"
                >
                  <div v-if="loadingMore" class="flex items-center">
                    <div class="animate-spin rounded-full h-4 w-4 border-t-2 border-b-2 border-primary-600 mr-2"></div>
                    {{ $t('search.loading_more') }}
                  </div>
                  <div v-else>
                    {{ $t('common.load_more') }}
                  </div>
                </button>
              </div>
            </template>
          </AssignmentResultsList>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {MagnifyingGlassIcon, XMarkIcon} from '@heroicons/vue/24/outline'
import {useAssignmentStore} from '~/stores/assignment'
import {useRoute, useRouter} from 'vue-router'
import useTheme from '~/composables/useTheme'
import AppLogo from '~/components/app/Logo.vue'
import AssignmentResultsList from '~/components/assignment/ResultsList.vue'

const route = useRoute()
const router = useRouter()
const { isDark } = useTheme()
const assignmentStore = useAssignmentStore()

const loading = ref(false)
const loadingMore = ref(false)
const searchQuery = ref('')
const hasSearched = ref(false)

// Simplified search filters - only keyword
const searchFilters = ref({
  keyword: ''
})

const assignments = computed(() => assignmentStore.assignments)
const totalItems = computed(() => assignmentStore.totalItems)
const hasMorePages = computed(() => assignmentStore.currentPage < assignmentStore.totalPages - 1)

// Initialize page and check for query parameters
onMounted(async () => {
  // Check if there's a query parameter 'q'
  const queryParam = route.query.q as string
  
  if (queryParam) {
    searchQuery.value = queryParam
    searchFilters.value.keyword = queryParam
    await search()
    hasSearched.value = true
  }
})

// Watch for URL query changes (for direct links or browser navigation)
watch(() => route.query.q, async (newQuery) => {
  if (newQuery && newQuery !== searchQuery.value) {
    searchQuery.value = newQuery as string
    searchFilters.value.keyword = newQuery as string
    await search()
  }
}, { immediate: true })

async function search() {
  if (searchQuery.value.trim()) {
    loading.value = true
    
    try {
      // Update filters with trimmed search query
      searchFilters.value.keyword = searchQuery.value.trim()
      
      // Update the URL
      router.push({
        query: { q: searchQuery.value.trim() }
      })
      
      // Perform the search
      await assignmentStore.searchAssignments(searchFilters.value, 0)
      hasSearched.value = true
    } finally {
      loading.value = false
    }
  }
}

async function clearSearch() {
  searchQuery.value = ''
  searchFilters.value.keyword = ''

  // Update URL to remove the query parameter
  router.push({ query: {} })
  hasSearched.value = false

  // Clear the assignments
  assignmentStore.assignments = []
  assignmentStore.totalItems = 0
}

function resetFilters() {
  clearSearch()
}

async function loadMore() {
  loadingMore.value = true
  try {
    const nextPage = assignmentStore.currentPage + 1
    
    // Since we only have keyword search now
    await assignmentStore.searchAssignments(searchFilters.value, nextPage)
  } finally {
    loadingMore.value = false
  }
}
</script>