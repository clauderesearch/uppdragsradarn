<template>
  <div class="flex flex-col min-h-screen">
    <!-- SEO metadata for home page -->
    <AppSeo
      :title="searchQuery ? $t('search.found_results', { count: totalItems }) + ' | Uppdragsradarn' : 'Uppdragsradarn - Hitta ditt nästa uppdrag på nolltid'"
      :description="searchQuery
        ? $t('search.found_results', { count: totalItems }) + ' ' + $t('search.for_query', { query: searchQuery })
        : 'Hitta ditt nästa uppdrag som konsult. Sök bland de senaste konsultuppdragen från olika källor samlade på ett ställe.'"
      :og-type="'website'"
      :json-ld="homePageStructuredData"
    />

    <!-- Search Form -->
    <div v-if="!hasSearched" class="flex flex-col items-center justify-center px-4 py-8 flex-grow">
      <div class="w-full max-w-3xl mx-auto mb-6 text-center">
        <div class="mb-4">
          <AppLogo size="large" />
        </div>

        <form @submit.prevent="search" class="w-full" role="search" aria-labelledby="search-form-label">
          <div class="relative">
            <label id="search-form-label" for="search-input" class="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('search.header') }}</label>
            <input
              id="search-input"
              v-model="searchQuery"
              type="search"
              :placeholder="$t('search.example_placeholder')"
              class="w-full px-5 py-4 text-lg border border-gray-300 dark:border-gray-700 rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 bg-white dark:bg-primary-950 text-gray-900 dark:text-gray-100"
              autofocus
              aria-required="true"
              minlength="2"
              aria-describedby="search-instructions"
            />
            <button
              type="submit"
              id="search-button"
              class="absolute right-2 top-1/2 transform -translate-y-1/2 bg-primary-600 dark:bg-primary-500 text-white px-6 py-2 rounded-full hover:bg-primary-700 dark:hover:bg-primary-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
              aria-label="Submit search"
              :disabled="searchQuery.trim().length < 2"
            >
              {{ $t('search.search_button') }}
            </button>
            <div id="search-instructions" class="mt-1 text-sm text-gray-500 dark:text-gray-400" aria-live="polite">
              {{ $t('search.input_instruction') }}
            </div>
          </div>
        </form>
      </div>

      <!-- Recent Assignments section -->
      <div v-if="!loading && recentAssignments.length > 0" class="w-full max-w-3xl mt-10">
        <h2 class="text-xl font-semibold mb-4 text-center text-gray-700 dark:text-gray-300" id="recent-assignments">{{ $t('search.recent_assignments') }}</h2>
        <AssignmentResultsList
          :assignments="recentAssignments"
          :loading="loading"
          :show-empty-message="false"
          :max-skills-to-show="3"
          :show-description="false"
          list-item-class="hover:bg-gray-50 dark:hover:bg-primary-900"
          aria-labelledby="recent-assignments"
        />
      </div>
    </div>

    <!-- Search Results -->
    <div v-else class="container max-w-5xl mx-auto px-4 pt-4 pb-16 flex-grow">
      <!-- Mobile-only search bar - shown only on small screens since the header has the search on larger screens -->
      <div class="sm:hidden mb-4 w-full">
        <form @submit.prevent="search" class="relative">
          <input
            v-model="searchQuery"
            type="text"
            :placeholder="$t('search.placeholder')"
            class="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-full focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 shadow-sm text-base bg-white dark:bg-primary-950 text-gray-900 dark:text-gray-100"
          />
          <button
            type="submit"
            class="absolute right-1.5 top-1/2 transform -translate-y-1/2 bg-primary-600 dark:bg-primary-500 text-white px-3 py-1 rounded-full hover:bg-primary-700 dark:hover:bg-primary-400"
          >
            <MagnifyingGlassIcon class="h-4 w-4" />
          </button>
        </form>
      </div>

      <!-- Result count - more compact -->
      <div class="max-w-5xl flex items-center justify-between text-sm mb-3">
        <div class="text-gray-600 dark:text-gray-400 font-medium text-xs">
          {{ $t('search.found_results', { count: totalItems }) }}
          {{ searchQuery ? $t('search.for_query', { query: searchQuery }) : '' }}
        </div>

        <div class="flex items-center space-x-3">
          <button
            v-if="searchQuery"
            @click="clearSearch"
            class="text-primary-600 dark:text-primary-400 hover:text-primary-800 dark:hover:text-primary-300 flex items-center text-xs"
          >
            <XMarkIcon class="h-3 w-3 mr-1" />
            {{ $t('search.clear_search') }}
          </button>
        </div>
      </div>

      <!-- Search results -->
      <div class="max-w-5xl mx-auto">
        <AssignmentResultsList
          :assignments="searchResults"
          :loading="loading"
          :show-empty-message="true"
          :empty-message="$t('search.no_matches')"
        >
          <!-- No results action slot -->
          <template #empty-actions>
            <button
              @click="clearSearch"
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
</template>

<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useNuxtApp, useRoute, useRouter} from '#app'
import {useAssignmentStore} from '~/stores/assignment'
import useTheme from '~/composables/useTheme'
import AppLogo from '~/components/app/Logo.vue'
import AppSeo from '~/components/app/Seo.vue'
import AssignmentResultsList from '~/components/assignment/ResultsList.vue'
import {MagnifyingGlassIcon, XMarkIcon} from '@heroicons/vue/24/outline'

const router = useRouter()
const route = useRoute()
const assignmentStore = useAssignmentStore()
const { isDark } = useTheme()
const { locale } = useI18n()

const searchQuery = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const hasSearched = ref(false)

// Search filters
const searchFilters = ref({
  keyword: ''
})

const recentAssignments = computed(() => assignmentStore.recentAssignments)
const searchResults = computed(() => assignmentStore.assignments)
const totalItems = computed(() => assignmentStore.totalItems)
const hasMorePages = computed(() => assignmentStore.currentPage < assignmentStore.totalPages - 1)

// Generate JSON-LD structured data for the home page
const homePageStructuredData = computed(() => {
  const baseUrl = process.client ? window.location.origin : 'https://uppdragsradarn.se'

  // Base website structured data
  const websiteData = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    'name': 'Uppdragsradarn',
    'url': baseUrl,
    'description': 'Hitta ditt nästa uppdrag som konsult',
    'potentialAction': {
      '@type': 'SearchAction',
      'target': {
        '@type': 'EntryPoint',
        'urlTemplate': `${baseUrl}/?q={search_term_string}`
      },
      'query-input': 'required name=search_term_string'
    }
  }

  // If we have search results, also add a JobList structured data
  if (hasSearched.value && searchResults.value.length > 0) {
    return {
      ...websiteData,
      '@graph': [
        websiteData,
        {
          '@context': 'https://schema.org',
          '@type': 'ItemList',
          'itemListElement': searchResults.value.map((job, index) => ({
            '@type': 'ListItem',
            'position': index + 1,
            'item': {
              '@type': 'JobPosting',
              'title': job.title,
              'description': job.description || `Job posting for ${job.title}`,
              'datePosted': job.publishedDate,
              'employmentType': 'CONTRACTOR',
              'hiringOrganization': {
                '@type': 'Organization',
                'name': job.companyName || 'Company'
              },
              'jobLocation': {
                '@type': 'Place',
                'address': {
                  '@type': 'PostalAddress',
                  'addressLocality': job.location || 'Sweden'
                }
              },
              'url': `${baseUrl}/assignments/${job.id}`
            }
          }))
        }
      ]
    }
  }

  return websiteData
})

// Watch for URL query changes
watch(() => route.query.q, async (newQuery) => {
  // Get nuxtApp for the language enforcement
  const nuxtApp = useNuxtApp()

  // Enforce language setting with every route change
  if (nuxtApp.$enforceLanguageSetting) {
    nuxtApp.$enforceLanguageSetting()
  }

  if (newQuery && newQuery !== searchQuery.value) {
    searchQuery.value = newQuery as string
    searchFilters.value.keyword = newQuery as string
    await performSearch()
  }
}, { immediate: true })

// Initialize page and check for query parameters
onMounted(async () => {
  // Check if there's a query parameter 'q'
  const queryParam = route.query.q as string

  // Use the enforceLanguageSetting function from the plugin
  const nuxtApp = useNuxtApp()
  if (nuxtApp.$enforceLanguageSetting) {
    nuxtApp.$enforceLanguageSetting()
  }

  // Set default locale if none exists in localStorage
  if (!localStorage.getItem('locale')) {
    localStorage.setItem('locale', 'en')
    locale.value = 'en'
  }

  if (queryParam) {
    searchQuery.value = queryParam
    searchFilters.value.keyword = queryParam
    await performSearch()
  } else {
    // Load recent assignments if not searching
    loading.value = true
    try {
      await assignmentStore.fetchRecentAssignments(5)
    } finally {
      loading.value = false
      // Ensure input gets focus on page load
      setTimeout(() => {
        const searchInput = document.querySelector('input[type="text"]')
        if (searchInput) searchInput.focus()
      }, 100)
    }
  }
})

async function search() {
  // Only search if query is at least 2 characters
  if (searchQuery.value.trim().length >= 2) {
    searchFilters.value.keyword = searchQuery.value.trim()

    // Update URL
    router.push({
      query: { q: searchQuery.value.trim() }
    })

    await performSearch()
  }
}

async function performSearch() {
  if (searchFilters.value.keyword.trim()) {
    loading.value = true

    try {
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

  // Enforce language consistency when switching views
  const nuxtApp = useNuxtApp()
  if (nuxtApp.$enforceLanguageSetting) {
    nuxtApp.$enforceLanguageSetting()
  }

  // Clear the assignments
  assignmentStore.assignments = []
  assignmentStore.totalItems = 0

  // Reload recent assignments
  loading.value = true
  try {
    await assignmentStore.fetchRecentAssignments(5)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    const nextPage = assignmentStore.currentPage + 1
    await assignmentStore.searchAssignments(searchFilters.value, nextPage)
  } finally {
    loadingMore.value = false
  }
}
</script>