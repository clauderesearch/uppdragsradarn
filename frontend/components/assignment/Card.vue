<template>
  <div
    class="bg-white dark:bg-primary-950 rounded-lg shadow overflow-hidden hover:shadow-md transition-shadow duration-300"
    role="article"
    :class="{ 'border-2 border-yellow-500': assignment.premiumOnly && assignment.limitedVersion }"
  >
    <div class="p-5">
      <div class="flex justify-between items-start">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100 truncate">
          {{ assignment.title }}
        </h3>
        <div class="flex space-x-2">
          <!-- Premium Only Badge -->
          <span
            v-if="assignment.premiumOnly && assignment.limitedVersion"
            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800"
            aria-label="Premium only assignment"
          >
            {{ $t('assignment.premium_only') }}
          </span>

          <!-- New Badge -->
          <span
            v-if="isNew"
            class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"
            aria-label="New assignment"
          >
            {{ $t('common.new') }}
          </span>
        </div>
      </div>

      <div class="mt-2 text-sm text-gray-500 dark:text-gray-400">
        <!-- Company name row with placeholder for premium content -->
        <div class="flex items-center" aria-label="Company">
          <BuildingOfficeIcon class="h-4 w-4 mr-1" aria-hidden="true" />
          <span v-if="!assignment.limitedVersion">
            {{ assignment.companyName || $t('assignment.company_unknown') }}
          </span>
          <span v-else class="italic">
            {{ $t('assignment.company_hidden') }}
          </span>
        </div>

        <!-- Location is shown for all assignments -->
        <div class="flex items-center mt-1" aria-label="Location">
          <MapPinIcon class="h-4 w-4 mr-1" aria-hidden="true" />
          {{ assignment.location || $t('assignment.location_not_specified') }}
          <span v-if="!assignment.limitedVersion && assignment.remotePercentage" class="ml-2">
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
        class="mt-3 flex flex-wrap gap-1"
        role="list"
        aria-label="Required skills"
      >
        <span
          v-for="skill in limitedSkills"
          :key="skill"
          class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-100"
          role="listitem"
        >
          {{ skill }}
        </span>
        <span
          v-if="hasMoreSkills"
          class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-300"
          aria-label="plus more skills"
        >
          {{ $t('assignment.more_skills', { count: assignment.skills.length - maxSkillsToShow }) }}
        </span>
      </div>

      <div class="mt-4 flex items-center justify-between">
        <!-- Rate and duration info (only shown for non-limited or premium-user viewing premium content) -->
        <div v-if="!assignment.limitedVersion">
          <div class="flex items-center">
            <CurrencyDollarIcon class="h-4 w-4 mr-1 text-gray-400" aria-hidden="true" />
            <span class="text-sm text-gray-700 dark:text-gray-300">
              {{ formatRate(assignment.hourlyRateMin, assignment.hourlyRateMax, assignment.currency) }}
            </span>
          </div>
          <div class="flex items-center mt-1">
            <CalendarIcon class="h-4 w-4 mr-1 text-gray-400" aria-hidden="true" />
            <span class="text-sm text-gray-700 dark:text-gray-300">
              {{ formatDuration(assignment.durationMonths) }}
            </span>
          </div>
        </div>

        <!-- Empty div to maintain flex layout when details are hidden -->
        <div v-else></div>

        <NuxtLink
          :to="`/assignments/${assignment.id}`"
          class="btn btn-primary text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
          :aria-label="`View details for ${assignment.title}`"
        >
          {{ $t('assignment.view_details') || 'View Details' }}
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {BuildingOfficeIcon, CalendarIcon, CurrencyDollarIcon, MapPinIcon} from '@heroicons/vue/24/outline'

const props = defineProps({
  assignment: {
    type: Object,
    required: true
  },
  isNew: {
    type: Boolean,
    default: false
  }
})

const maxSkillsToShow = 3

const limitedSkills = computed(() => {
  if (!props.assignment.skills) return []
  return props.assignment.skills.slice(0, maxSkillsToShow)
})

const hasMoreSkills = computed(() => {
  if (!props.assignment.skills) return false
  return props.assignment.skills.length > maxSkillsToShow
})

function formatRate(min: number | null, max: number | null, currency: string = 'SEK') {
  if (!min && !max) return 'Rate not specified'
  if (min && max) return `${min}-${max} ${currency}/hr`
  return `${min || max} ${currency}/hr`
}

function formatDuration(months: number | null) {
  if (!months) return 'Duration not specified'
  return months === 1 ? '1 month' : `${months} months`
}
</script>