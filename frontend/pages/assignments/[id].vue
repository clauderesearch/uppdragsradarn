<template>
  <div class="container mx-auto px-4 py-8">
    <!-- SEO metadata for assignment details -->
    <AppSeo
      v-if="assignment"
      :title="assignment.title + ' | Uppdragsradarn'"
      :description="assignment.description ? assignment.description.substring(0, 160) + '...' : 'Uppdrag hos ' + (assignment.companyName || 'företag') + ' i ' + (assignment.location || 'Sverige')"
      :og-type="'article'"
      :twitter-card="'summary_large_image'"
      :json-ld="assignmentStructuredData"
    />

    <div v-if="loading" class="flex justify-center items-center py-16" aria-live="polite">
      <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary" aria-label="Loading" role="status">
        <span class="sr-only">{{ $t('common.loading') }}</span>
      </div>
    </div>

    <!-- Error handling moved to script section using redirection -->

    <template v-else-if="assignment">
      <div class="flex items-center mb-6">
        <NuxtLink
          :to="searchQuery ? { path: '/', query: { q: searchQuery } } : '/'"
          class="text-primary hover:text-primary-dark flex items-center focus:outline-none focus:ring-2 focus:ring-primary-500 rounded-md px-2 py-1"
          :aria-label="$t('common.back_to_search')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fill-rule="evenodd" d="M9.707 14.707a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 1.414L7.414 9H15a1 1 0 110 2H7.414l2.293 2.293a1 1 0 010 1.414z" clip-rule="evenodd" />
          </svg>
          {{ $t('common.back_to_search') }}
        </NuxtLink>
      </div>

      <div
        class="bg-white dark:bg-primary-950 rounded-lg shadow-lg p-6 mb-8"
        :class="{ 'border-2 border-yellow-500': assignment.premiumOnly && assignment.limitedVersion }"
      >
        <header class="mb-6">
          <div class="flex justify-between items-start">
            <h1 id="assignment-title" class="text-2xl font-bold text-gray-900 dark:text-gray-100" tabindex="0">
              {{ assignment.title }}
            </h1>
            <div class="flex space-x-2">
              <!-- Premium Badge -->
              <div
                v-if="assignment.premiumOnly && assignment.limitedVersion"
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800"
                role="status"
                aria-label="Premium assignment"
              >
                {{ $t('assignment.premium_only') }}
              </div>

              <!-- New Badge -->
              <div
                v-if="isNew"
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-900"
                role="status"
                aria-label="New assignment"
              >
                {{ $t('common.new') }}
              </div>
            </div>
          </div>

          <!-- Company and location info -->
          <div class="mt-4 text-gray-700 dark:text-gray-300" tabindex="0" aria-labelledby="assignment-title">
            <!-- Company name with placeholder for premium content -->
            <div class="flex items-center mb-2">
              <BuildingOfficeIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
              <span v-if="!assignment.limitedVersion">
                {{ assignment.companyName || $t('assignment.company_unknown') }}
              </span>
              <span v-else class="italic">
                {{ $t('assignment.company_hidden') }}
              </span>
            </div>

            <!-- Location is shown for all assignments -->
            <div class="flex items-center">
              <MapPinIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
              <span>
                {{ assignment.location || $t('assignment.location_not_specified') }}
                <span v-if="!assignment.limitedVersion && assignment.remotePercentage" class="ml-2">
                  ({{ $t('assignment.remote_percentage', { percentage: assignment.remotePercentage }) }})
                </span>
              </span>
            </div>
          </div>
        </header>
        
        <!-- Premium-only content banner -->
        <section v-if="assignment.premiumOnly && assignment.limitedVersion" class="mb-8">
          <div class="bg-yellow-50 dark:bg-yellow-900 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 text-yellow-700 dark:text-yellow-200">
            <div class="flex items-start">
              <div class="flex-shrink-0">
                <svg class="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                </svg>
              </div>
              <div class="ml-3">
                <h3 class="text-sm font-medium text-yellow-800 dark:text-yellow-200">{{ $t('assignment.premium_content') }}</h3>
                <div class="mt-2 text-sm text-yellow-700 dark:text-yellow-300">
                  <p>{{ $t('assignment.upgrade_required') }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Main content sections - Only visible for non-limited or premium users -->
        <section v-if="!assignment.limitedVersion" class="mb-8">
          <h2 id="assignment-overview" class="text-xl font-semibold mb-3 text-gray-800 dark:text-gray-200">{{ $t('assignment.overview') }}</h2>
          <Markdown
            :content="assignment.description"
            :empty-message="$t('assignment.no_description')"
            aria-labelledby="assignment-overview"
            tabindex="0"
          />
        </section>
        
        <section v-if="!assignment.limitedVersion" class="mb-8 grid md:grid-cols-2 gap-6">
          <div>
            <h2 id="assignment-skills" class="text-xl font-semibold mb-3 text-gray-800 dark:text-gray-200">{{ $t('assignment.skills') }}</h2>
            <div v-if="assignment.skills && assignment.skills.length" class="flex flex-wrap gap-2" role="list" aria-labelledby="assignment-skills" tabindex="0">
              <span
                v-for="skill in assignment.skills"
                :key="skill"
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-900 dark:text-blue-100"
                role="listitem"
              >
                {{ skill }}
              </span>
            </div>
            <div v-else class="text-gray-600 dark:text-gray-400 italic" tabindex="0" aria-labelledby="assignment-skills">
              {{ $t('assignment.no_skills') }}
            </div>
          </div>

          <div>
            <h2 id="assignment-details" class="text-xl font-semibold mb-3 text-gray-800 dark:text-gray-200">{{ $t('assignment.details') }}</h2>
            <ul class="space-y-2" tabindex="0" aria-labelledby="assignment-details">
              <li class="flex items-center">
                <CurrencyDollarIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
                <span class="text-gray-700 dark:text-gray-300">{{ formatRate(assignment.hourlyRateMin, assignment.hourlyRateMax, assignment.currency) }}</span>
              </li>
              <li class="flex items-center">
                <CalendarIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
                <span class="text-gray-700 dark:text-gray-300">{{ formatDuration(assignment.durationMonths) }}</span>
              </li>
              <li v-if="assignment.startDate" class="flex items-center">
                <ClockIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
                <span class="text-gray-700 dark:text-gray-300">{{ $t('assignment.start_date', { date: formatDate(assignment.startDate) }) }}</span>
              </li>
              <li v-if="assignment.publishedDate" class="flex items-center">
                <DocumentIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
                <span class="text-gray-700 dark:text-gray-300">{{ $t('assignment.published', { date: formatDate(assignment.publishedDate) }) }}</span>
              </li>
              <li v-if="assignment.source" class="flex items-center">
                <GlobeAltIcon class="h-5 w-5 mr-2 text-gray-500 dark:text-gray-400" aria-hidden="true" />
                <span class="text-gray-700 dark:text-gray-300">{{ $t('assignment.source', { source: assignment.source.name }) }}</span>
              </li>
            </ul>
          </div>
        </section>
        
        <div class="border-t pt-6 flex justify-end gap-4">
          <!-- Only show action buttons for non-limited assignments -->
          <template v-if="!assignment.limitedVersion">
            <a
              v-if="assignment.applicationUrl"
              :href="assignment.applicationUrl"
              target="_blank"
              class="btn btn-secondary focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
              rel="noopener"
              :aria-label="$t('assignment.apply_now_aria', { title: assignment.title }) || `${$t('assignment.apply_now')} - ${assignment.title}`"
            >
              {{ $t('assignment.apply_now') }}
            </a>
            <a
              v-if="assignment.sourceUrl"
              :href="assignment.sourceUrl"
              target="_blank"
              class="btn btn-primary focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
              rel="noopener"
              :aria-label="$t('assignment.view_original_aria', { title: assignment.title }) || `${$t('assignment.view_original')} - ${assignment.title}`"
            >
              {{ $t('assignment.view_original') }}
            </a>
          </template>

          <!-- For premium-only assignments show a single upgrade button -->
          <button
            v-if="assignment.premiumOnly && assignment.limitedVersion"
            class="btn btn-primary bg-yellow-500 hover:bg-yellow-600 border-yellow-500 hover:border-yellow-600 focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:ring-offset-2"
          >
            {{ $t('assignment.premium_only') }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {
  BuildingOfficeIcon,
  CalendarIcon,
  ClockIcon,
  CurrencyDollarIcon,
  DocumentIcon,
  GlobeAltIcon,
  MapPinIcon
} from '@heroicons/vue/24/outline'
import {useRoute, useRouter} from "vue-router";
import {useI18n} from 'vue-i18n'
import {useRuntimeConfig} from "nuxt/app";
import AppSeo from '../../components/app/Seo.vue';
import PlainText from "../../components/ui/PlainText.vue";
import Markdown from "../../components/ui/Markdown.vue";

const { locale, t } = useI18n()
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const assignment = ref(null)
const searchQuery = ref('')

const isNew = computed(() => {
  if (!assignment.value?.publishedDate) return false

  const publishedDate = new Date(assignment.value.publishedDate)
  const threeDaysAgo = new Date()
  threeDaysAgo.setDate(threeDaysAgo.getDate() - 3)

  return publishedDate > threeDaysAgo
})

// Generate JSON-LD structured data for this assignment
const assignmentStructuredData = computed(() => {
  if (!assignment.value) return {}

  const baseUrl = process.client ? window.location.origin : 'https://uppdragsradarn.se'
  const currentUrl = `${baseUrl}/assignments/${assignment.value.id}`

  return {
    '@context': 'https://schema.org',
    '@type': 'JobPosting',
    'title': assignment.value.title,
    'description': assignment.value.description || `Job posting for ${assignment.value.title}`,
    'datePosted': assignment.value.publishedDate,
    'validThrough': assignment.value.applicationDeadline,
    'employmentType': 'CONTRACTOR',
    'workHours': assignment.value.hoursPerWeek ? `${assignment.value.hoursPerWeek} hours per week` : 'Full time',
    'hiringOrganization': {
      '@type': 'Organization',
      'name': assignment.value.companyName || 'Unknown company'
    },
    'jobLocation': {
      '@type': 'Place',
      'address': {
        '@type': 'PostalAddress',
        'addressCountry': 'SE',
        'addressLocality': assignment.value.location || 'Sweden'
      }
    },
    'baseSalary': assignment.value.hourlyRateMin ? {
      '@type': 'MonetaryAmount',
      'currency': assignment.value.currency || 'SEK',
      'value': {
        '@type': 'QuantitativeValue',
        'value': assignment.value.hourlyRateMin,
        'unitText': 'HOUR'
      }
    } : undefined,
    'skills': assignment.value.skills?.join(', '),
    'url': currentUrl
  }
})

onMounted(async () => {
  try {
    // First check if query parameter is present
    if (route.query.from) {
      // Allow direct linking with preserved search query via '?from=query'
      searchQuery.value = route.query.from as string
    } else {
      // Try to get the search query from the referring route, if present
      try {
        const referrer = document.referrer
        if (referrer && referrer.trim() !== '') {
          const referrerUrl = new URL(referrer)
          const referringPathname = referrerUrl.pathname
          const referringQueryParams = new URLSearchParams(referrerUrl.search)

          // Extract the search query from referrer
          if ((referringPathname === '/' || referringPathname === '/index.html') && referringQueryParams.has('q')) {
            searchQuery.value = referringQueryParams.get('q') || ''
          }
        }
      } catch (e) {
        console.error('Error parsing referrer:', e)
        // Continue even if referrer parsing fails
      }
    }

    const assignmentId = route.params.id
    const config = useRuntimeConfig()
    const apiBase = config.public.apiBase || 'http://localhost/api'
    const response = await fetch(`${apiBase}/assignments/${assignmentId}`, {
      credentials: 'include' // Include cookies in the request
    })

    if (!response.ok) {
      // Redirect to non-existent route to trigger custom error page for all errors
      if (response.status === 404) {
        // For 404, show the "not found" error
        router.push('/not-found')
      } else {
        // For other error types, still show the error page
        router.push('/error-page')
      }
      return
    }

    assignment.value = await response.json()
  } catch (err) {
    // Log error and redirect to error page
    console.error('Error loading assignment:', err)
    router.push('/error-page')
    return
  } finally {
    loading.value = false
  }
})

function formatRate(min: number | null, max: number | null, currency: string = 'SEK') {
  if (!min && !max) return t('assignment.rate_not_specified')
  if (min && max) return `${min}-${max} ${currency}/hr`
  return `${min || max} ${currency}/hr`
}

function formatDuration(months: number | null) {
  if (!months) return t('assignment.duration_not_specified')
  return months === 1 
    ? t('search.at_least_month', { n: 1 })
    : t('search.at_least_months', { n: months })
}

function formatDate(dateString: string) {
  if (!dateString) return ''
  
  const date = new Date(dateString)
  const locale2 = locale.value === 'sv' ? 'sv-SE' : 'en-GB'
  return new Intl.DateTimeFormat(locale2, {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  }).format(date)
}
</script>