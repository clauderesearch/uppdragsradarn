<template>
  <div
    class="bg-white dark:bg-primary-950 rounded-lg shadow overflow-hidden hover:shadow-md transition-shadow"
    role="article"
    :class="{ 'border-2 border-yellow-500': assignment.premiumOnly && assignment.limitedVersion }"
  >
    <div class="p-3">
      <!-- Card Header -->
      <div class="flex justify-between items-start relative">
        <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100 truncate pr-24">
          {{ assignment.title }}
        </h3>
        <div class="flex space-x-1 absolute top-0 right-0">
          <!-- Toggle button to expand/collapse details -->
          <button
            @click="toggleExpanded"
            class="inline-flex items-center rounded-md px-2 py-1 text-xs font-medium bg-primary-50 dark:bg-primary-900 text-primary-700 dark:text-primary-200 hover:bg-primary-100 dark:hover:bg-primary-800 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transition-colors"
            :aria-label="isExpanded ? `Collapse details for ${assignment.title}` : `View details for ${assignment.title}`"
            :aria-expanded="isExpanded"
          >
            <span class="mr-1">{{ isExpanded ? $t('assignment.collapse_details') || 'Collapse' : $t('assignment.view_details') || 'View' }}</span>
            <ChevronDownIcon v-if="!isExpanded" class="h-3 w-3" />
            <ChevronUpIcon v-else class="h-3 w-3" />
          </button>

          <!-- Premium Only Badge -->
          <span
            v-if="assignment.premiumOnly && assignment.limitedVersion"
            class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800"
            aria-label="Premium only assignment"
          >
            {{ $t('assignment.premium_only') }}
          </span>

          <!-- New Badge -->
          <span
            v-if="isNew"
            class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"
            aria-label="New assignment"
          >
            {{ $t('common.new') }}
          </span>
        </div>
      </div>

      <!-- Basic info (always visible) -->
      <div class="mt-1 text-xs text-gray-500 dark:text-gray-400">
        <!-- Company name row with placeholder for premium content -->
        <div class="flex items-center" aria-label="Company">
          <BuildingOfficeIcon class="h-3 w-3 mr-1" aria-hidden="true" />
          <span v-if="!assignment.limitedVersion">
            {{ assignment.companyName || $t('assignment.company_unknown') }}
          </span>
          <span v-else class="italic">
            {{ $t('assignment.company_hidden') }}
          </span>
        </div>

        <!-- Location is shown for all assignments -->
        <div class="flex items-center mt-0.5" aria-label="Location">
          <MapPinIcon class="h-3 w-3 mr-1" aria-hidden="true" />
          {{ assignment.location || $t('assignment.location_not_specified') }}
          <span v-if="!assignment.limitedVersion && assignment.remotePercentage" class="ml-1">
            ({{ $t('assignment.remote_percentage', { percentage: assignment.remotePercentage }) }})
          </span>
        </div>
      </div>

      <!-- Premium Only Message -->
      <div
        v-if="assignment.premiumOnly && assignment.limitedVersion"
        class="mt-3 text-sm text-yellow-600 bg-yellow-50 dark:bg-yellow-900 dark:text-yellow-200 p-2 rounded"
      >
        {{ $t('assignment.upgrade_required') }}
      </div>

      <!-- Skills section (only shown for non-limited or premium-user viewing premium content) -->
      <div
        v-if="!assignment.limitedVersion"
        class="mt-2 flex flex-wrap gap-1"
        role="list"
        aria-label="Required skills"
      >
        <span
          v-for="skill in limitedSkills"
          :key="skill"
          class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-blue-50 dark:bg-blue-900 text-blue-800 dark:text-blue-100"
          role="listitem"
        >
          {{ skill }}
        </span>
        <span
          v-if="hasMoreSkills"
          class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-gray-50 dark:bg-gray-800 text-gray-800 dark:text-gray-300"
          aria-label="plus more skills"
        >
          {{ $t('assignment.more_skills', { count: assignment.skills.length - maxSkillsToShow }) }}
        </span>
      </div>

      <!-- Bottom action row -->
      <div class="mt-2 flex items-center justify-between">
        <!-- Rate and duration info (only shown for non-limited or premium-user viewing premium content) -->
        <div v-if="!assignment.limitedVersion">
          <div class="flex items-center">
            <CurrencyDollarIcon class="h-3 w-3 mr-1 text-gray-400" aria-hidden="true" />
            <span class="text-xs text-gray-700 dark:text-gray-300">
              {{ formatRate(assignment.hourlyRateMin, assignment.hourlyRateMax, assignment.currency) }}
            </span>
          </div>
          <div class="flex items-center mt-0.5">
            <CalendarIcon class="h-3 w-3 mr-1 text-gray-400" aria-hidden="true" />
            <span class="text-xs text-gray-700 dark:text-gray-300">
              {{ formatDuration(assignment.durationMonths) }}
            </span>
          </div>
        </div>

        <!-- Empty div to maintain flex layout when details are hidden -->
        <div v-else></div>
      </div>
    </div>

    <!-- Expandable Detail Section -->
    <div
      v-if="isExpanded"
      class="border-t border-gray-100 dark:border-gray-800 p-5 bg-gray-50 dark:bg-primary-900"
    >
      <!-- Main content sections - Only visible for non-limited or premium users -->
      <section v-if="!assignment.limitedVersion" class="mb-6">
        <h4 class="text-lg font-semibold mb-3 text-gray-800 dark:text-gray-200">{{ $t('assignment.overview') }}</h4>
        <Markdown
          :content="assignment.description"
          :empty-message="$t('assignment.no_description')"
          tabindex="0"
        />
      </section>
      
      <section v-if="!assignment.limitedVersion && (!assignment.skills || assignment.skills.length > maxSkillsToShow)" class="mb-6">
        <h4 class="text-lg font-semibold mb-3 text-gray-800 dark:text-gray-200">{{ $t('assignment.skills') }}</h4>
        <div v-if="assignment.skills && assignment.skills.length" class="flex flex-wrap gap-2" role="list" tabindex="0">
          <span
            v-for="skill in assignment.skills"
            :key="skill"
            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-900 dark:text-blue-100"
            role="listitem"
          >
            {{ skill }}
          </span>
        </div>
      </section>

      <div v-if="!assignment.limitedVersion" class="border-t pt-4 flex justify-end gap-4">
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
        <NuxtLink
          :to="`/assignments/${assignment.id}`"
          class="btn btn-outline focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
          :aria-label="`View full details for ${assignment.title}`"
        >
          {{ $t('assignment.view_full_details') || 'Full Details' }}
        </NuxtLink>
      </div>

      <!-- For premium-only assignments show a single upgrade button -->
      <div v-if="assignment.premiumOnly && assignment.limitedVersion" class="border-t pt-4 flex justify-end">
        <button
          class="btn btn-primary bg-yellow-500 hover:bg-yellow-600 border-yellow-500 hover:border-yellow-600 focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:ring-offset-2"
        >
          {{ $t('assignment.premium_only') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'
import {
  BuildingOfficeIcon,
  CalendarIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  CurrencyDollarIcon,
  MapPinIcon
} from '@heroicons/vue/24/outline'
import PlainText from "../ui/PlainText.vue";
import Markdown from "../ui/Markdown.vue";

const { t: $t } = useI18n()

const props = defineProps({
  assignment: {
    type: Object,
    required: true
  },
  isNew: {
    type: Boolean,
    default: false
  },
  isExpanded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggleExpanded'])

const maxSkillsToShow = 3

const limitedSkills = computed(() => {
  if (!props.assignment.skills) return []
  return props.assignment.skills.slice(0, maxSkillsToShow)
})

const hasMoreSkills = computed(() => {
  if (!props.assignment.skills) return false
  return props.assignment.skills.length > maxSkillsToShow
})

function toggleExpanded() {
  emit('toggleExpanded')
}

function formatRate(min: number | null, max: number | null, currency: string = 'SEK') {
  if (!min && !max) return $t('assignment.rate_not_specified')
  if (min && max) return `${min}-${max} ${currency}/hr`
  return `${min || max} ${currency}/hr`
}

function formatDuration(months: number | null) {
  if (!months) return $t('assignment.duration_not_specified')
  return months === 1
    ? $t('search.at_least_month', { n: 1 })
    : $t('search.at_least_months', { n: months })
}
</script>

<style scoped>
.btn {
  @apply inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2;
}

.btn-primary {
  @apply text-white bg-primary-600 hover:bg-primary-700 focus:ring-primary-500;
}

.btn-secondary {
  @apply text-primary-700 bg-primary-100 hover:bg-primary-200 focus:ring-primary-500;
}

.btn-outline {
  @apply text-primary-700 bg-white border-primary-300 hover:bg-gray-50 focus:ring-primary-500;
}
</style>